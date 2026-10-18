<#
.SYNOPSIS
Start EduManage backend.
#>

$ErrorActionPreference = 'Stop'

$env:DATABASE_URL = $env:DATABASE_URL -or 'jdbc:postgresql://localhost:5432/edumanage'
$env:DATABASE_USERNAME = $env:DATABASE_USERNAME -or 'edumanage'
$env:DATABASE_PASSWORD = $env:DATABASE_PASSWORD -or 'edumanage'
$env:JWT_SECRET = $env:JWT_SECRET -or 'change-me-in-production-use-a-long-random-string'
$env:CORS_ALLOWED_ORIGINS = $env:CORS_ALLOWED_ORIGINS -or 'http://localhost:5173,http://localhost:3000'

Push-Location "$PSScriptRoot/.."

Write-Host "Running Maven Spring Boot..."
& mvn spring-boot:run

Pop-Location
