$ErrorActionPreference = 'Stop'

$testPlan = Join-Path $PSScriptRoot 'appointment-race-10-users.jmx'
$jmeterCommand = Get-Command 'jmeterw.cmd' -ErrorAction SilentlyContinue
$candidates = @(
    if ($jmeterCommand) { $jmeterCommand.Source }
    if ($env:JMETER_HOME) { Join-Path $env:JMETER_HOME 'bin\jmeterw.cmd' }
    'C:\Users\ADMIN\AppData\Local\Programs\ApacheJMeter\apache-jmeter-5.6.3\bin\jmeterw.cmd'
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }
$jmeterLauncher = $candidates | Select-Object -First 1

if (-not $jmeterLauncher) {
    throw 'JMeter launcher not found. Add JMeter bin to PATH or set JMETER_HOME.'
}

Start-Process -FilePath $jmeterLauncher -WorkingDirectory $PSScriptRoot -ArgumentList @('-t', $testPlan)
