<#
.SYNOPSIS
Run backend tests.
#>
$ErrorActionPreference = 'Stop'
Push-Location "$PSScriptRoot/.."
& mvn test
Pop-Location
