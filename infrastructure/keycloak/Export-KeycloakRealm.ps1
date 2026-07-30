param(
    [string]$ContainerName = "keycloak",
    [string]$Realm = "healthcare"
)

$ErrorActionPreference = "Stop"
$scriptRoot = $PSScriptRoot
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $scriptRoot "backups\$timestamp"
$dataSnapshot = Join-Path $backupRoot "data"
$realmExport = Join-Path $backupRoot "export"

New-Item -ItemType Directory -Force -Path $dataSnapshot, $realmExport | Out-Null

$image = docker inspect $ContainerName --format "{{.Config.Image}}"
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($image)) {
    throw "Cannot inspect Keycloak container '$ContainerName'."
}

Write-Host "Stopping '$ContainerName' briefly to take a consistent H2 snapshot..."
docker stop $ContainerName | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Cannot stop Keycloak container '$ContainerName'."
}

try {
    docker cp "${ContainerName}:/opt/keycloak/data/." $dataSnapshot
    if ($LASTEXITCODE -ne 0) {
        throw "Cannot copy Keycloak data from '$ContainerName'."
    }
}
finally {
    Write-Host "Starting the original Keycloak container again..."
    docker start $ContainerName | Out-Null
}

$dataMount = "${dataSnapshot}:/opt/keycloak/data"
$exportMount = "${realmExport}:/opt/keycloak/data/export"

Write-Host "Exporting realm '$Realm' from the copied H2 database..."
docker run --rm --user 0 `
    --volume $dataMount `
    --volume $exportMount `
    $image `
    export --realm $Realm --dir /opt/keycloak/data/export --users realm_file

if ($LASTEXITCODE -ne 0) {
    throw "Realm export failed. The original container has already been restarted."
}

Write-Host "Export completed: $realmExport"
Write-Host "Review the export, then copy its JSON file(s) to '$scriptRoot\import' before starting the production stack."
