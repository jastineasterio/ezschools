<#
.SYNOPSIS
Start EduManage frontend dev server.
#>

$ErrorActionPreference = 'Stop'

Push-Location "$PSScriptRoot/.."

if (-not (Test-Path node_modules)) {
    Write-Host "Installing dependencies..."
    & npm install
}

Write-Host "Starting Vite dev server..."
& npm run dev

Pop-Location
