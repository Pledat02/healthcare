[CmdletBinding()]
param(
    [string]$Realm = 'healthcare',
    [string]$ClientId = 'jmeter-test',
    [string]$KeycloakBaseUrl = 'http://localhost:8080',
    [string]$GatewayBaseUrl = 'http://localhost:8090'
)

$ErrorActionPreference = 'Stop'

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$credentialPath = Join-Path $scriptDirectory 'users.csv'
$kcAdm = '/opt/keycloak/bin/kcadm.sh'
$kcConfig = '/tmp/kcadm-jmeter.config'

function Invoke-KcAdm {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $previousErrorPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $output = & docker exec keycloak $kcAdm @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorPreference
    if ($exitCode -ne 0) {
        throw "Keycloak command failed: $($output -join [Environment]::NewLine)"
    }
    return @($output | ForEach-Object {
        if ($_ -is [System.Management.Automation.ErrorRecord]) {
            $_.Exception.Message
        }
        else {
            [string]$_
        }
    })
}

function New-TestPassword {
    return 'Jm!Aa1' + [Guid]::NewGuid().ToString('N')
}

function Test-PatientProfile {
    param([string]$Token)

    try {
        Invoke-RestMethod `
            -Method Get `
            -Uri "$GatewayBaseUrl/api/patients/me" `
            -Headers @{ Authorization = "Bearer $Token" } | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker was not found in PATH.'
}

$runningContainer = docker ps --filter 'name=^/keycloak$' --format '{{.Names}}'
if ($runningContainer -ne 'keycloak') {
    throw 'The Docker container named keycloak is not running.'
}

Write-Host 'Authenticating with the local Keycloak container...'
$previousErrorPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$authOutput = & docker exec keycloak sh -c `
    '/opt/keycloak/bin/kcadm.sh config credentials --config /tmp/kcadm-jmeter.config --server http://localhost:8080 --realm master --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD"' 2>&1
$authExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorPreference
if ($authExitCode -ne 0) {
    throw "Keycloak authentication failed: $($authOutput -join [Environment]::NewLine)"
}

$clientUuid = Invoke-KcAdm get clients --config $kcConfig -r $Realm -q "clientId=$ClientId" --fields id --format csv --noquotes | Select-Object -First 1
if ($clientUuid) { $clientUuid = $clientUuid.Trim() }
if (-not $clientUuid) {
    Write-Host "Creating Keycloak client $ClientId..."
    Invoke-KcAdm create clients --config $kcConfig -r $Realm `
        -s "clientId=$ClientId" `
        -s 'enabled=true' `
        -s 'publicClient=true' `
        -s 'directAccessGrantsEnabled=true' `
        -s 'standardFlowEnabled=false' `
        -s 'implicitFlowEnabled=false' `
        -s 'serviceAccountsEnabled=false' `
        -s 'protocol=openid-connect' | Out-Null
}
else {
    Write-Host "Keycloak client $ClientId already exists."
}

$roleJson = Invoke-KcAdm get roles/PATIENT --config $kcConfig -r $Realm
if (-not $roleJson) {
    throw "Realm role PATIENT does not exist in $Realm."
}

$accounts = for ($index = 1; $index -le 10; $index++) {
    $number = $index.ToString('00')
    [pscustomobject]@{
        Number   = $number
        Username = "jmeter.patient$number@medibook.local"
        Password = New-TestPassword
    }
}

Write-Host 'Creating or refreshing 10 PATIENT accounts...'
foreach ($account in $accounts) {
    Write-Host "  Configuring $($account.Username)..."
    $userUuid = Invoke-KcAdm get users --config $kcConfig -r $Realm -q "username=$($account.Username)" --fields id --format csv --noquotes | Select-Object -First 1
    if ($userUuid) { $userUuid = $userUuid.Trim() }

    if (-not $userUuid) {
        Invoke-KcAdm create users --config $kcConfig -r $Realm `
            -s "username=$($account.Username)" `
            -s "email=$($account.Username)" `
            -s 'firstName=JMeter' `
            -s "lastName=Patient $($account.Number)" `
            -s 'enabled=true' `
            -s 'emailVerified=true' | Out-Null
    }
    else {
        Write-Host "    Updating Keycloak user ID $userUuid..."
        Invoke-KcAdm update "users/$userUuid" --config $kcConfig -r $Realm `
            -s 'firstName=JMeter' `
            -s "lastName=Patient $($account.Number)" `
            -s 'enabled=true' `
            -s 'emailVerified=true' | Out-Null
    }

    Write-Host '    Setting password...'
    Invoke-KcAdm set-password --config $kcConfig -r $Realm `
        --username $account.Username `
        --new-password $account.Password | Out-Null
    Write-Host '    Assigning PATIENT role...'
    Invoke-KcAdm add-roles --config $kcConfig -r $Realm `
        --uusername $account.Username `
        --rolename PATIENT | Out-Null
}

$csvLines = @('username,password')
$csvLines += $accounts | ForEach-Object { "$($_.Username),$($_.Password)" }
[System.IO.File]::WriteAllLines(
    $credentialPath,
    $csvLines,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host 'Logging in and creating patient profiles...'
$verified = 0
foreach ($account in $accounts) {
    $tokenResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$KeycloakBaseUrl/realms/$Realm/protocol/openid-connect/token" `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{
            grant_type = 'password'
            client_id  = $ClientId
            username   = $account.Username
            password   = $account.Password
        }

    $token = $tokenResponse.access_token
    if (-not $token) {
        throw "No access token was returned for $($account.Username)."
    }

    if (-not (Test-PatientProfile -Token $token)) {
        $profileBody = @{
            fullName    = "JMeter Patient $($account.Number)"
            phone       = "09000000$($account.Number)"
            dateOfBirth = '1990-01-01'
            gender      = if ([int]$account.Number % 2 -eq 0) { 'FEMALE' } else { 'MALE' }
            email       = $account.Username
            address     = 'JMeter race-condition test account'
        } | ConvertTo-Json

        Invoke-RestMethod `
            -Method Post `
            -Uri "$GatewayBaseUrl/api/patients/" `
            -Headers @{ Authorization = "Bearer $token" } `
            -ContentType 'application/json' `
            -Body $profileBody | Out-Null
    }

    if (-not (Test-PatientProfile -Token $token)) {
        throw "Patient profile verification failed for $($account.Username)."
    }
    $verified++
}

Write-Host "Done: $verified/10 accounts can log in and have PATIENT profiles."
Write-Host "Credentials saved to $credentialPath (ignored by Git)."
