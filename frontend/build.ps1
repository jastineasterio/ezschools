<#
.SYNOPSIS
Build frontend for production.
#>
$ErrorActionPreference = 'Stop'
Push-Location "$PSScriptRoot/.."
& npm run build
Pop-Location
