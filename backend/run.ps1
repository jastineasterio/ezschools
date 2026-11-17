$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:DATABASE_URL)) {
    $env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/edumanage'
}

if ([string]::IsNullOrWhiteSpace($env:DATABASE_USERNAME)) {
    $env:DATABASE_USERNAME = 'edumanage'
}

if ([string]::IsNullOrWhiteSpace($env:DATABASE_PASSWORD)) {
    $env:DATABASE_PASSWORD = 'edumanage'
}

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $env:JWT_SECRET = 'change-me-in-production-use-a-long-random-string'
}

if ([string]::IsNullOrWhiteSpace($env:CORS_ALLOWED_ORIGINS)) {
    $env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173,http://localhost:3000'
}

Write-Host "Starting EduManage Backend..." -ForegroundColor Cyan
Write-Host "Database URL: $env:DATABASE_URL" -ForegroundColor Green
Write-Host "Database User: $env:DATABASE_USERNAME" -ForegroundColor Green

Push-Location $PSScriptRoot

try {
    & mvn -f "$PSScriptRoot\pom.xml" spring-boot:run
}
finally {
    Pop-Location
}
