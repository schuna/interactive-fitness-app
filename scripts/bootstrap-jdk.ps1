$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $PSScriptRoot
$toolsDir = Join-Path $projectDir ".tools"
$archive = Join-Path $toolsDir "jdk17.zip"
$installDir = Join-Path $toolsDir "jdk17"
$homeFile = Join-Path $toolsDir "jdk17-home.txt"
$downloadUrl =
    "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"

$existingJava = Get-ChildItem $installDir -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match "\\bin\\java\.exe$" } |
    Select-Object -First 1
if ($existingJava) {
    $jdkHome = Split-Path -Parent (Split-Path -Parent $existingJava.FullName)
    Set-Content -LiteralPath $homeFile -Value $jdkHome -NoNewline
    Write-Host "JDK 17 is already ready: $jdkHome"
    exit 0
}

New-Item -ItemType Directory -Path $toolsDir -Force | Out-Null
Write-Host "Downloading Temurin JDK 17..."
curl.exe -L --fail --retry 3 $downloadUrl -o $archive

if (Test-Path $installDir) {
    Remove-Item -LiteralPath $installDir -Recurse -Force
}
Expand-Archive -LiteralPath $archive -DestinationPath $installDir -Force

$installedJava = Get-ChildItem $installDir -Filter java.exe -Recurse |
    Where-Object { $_.FullName -match "\\bin\\java\.exe$" } |
    Select-Object -First 1
if (-not $installedJava) {
    throw "JDK installation did not produce the expected java.exe."
}
$jdkHome = Split-Path -Parent (Split-Path -Parent $installedJava.FullName)
Set-Content -LiteralPath $homeFile -Value $jdkHome -NoNewline

Write-Host "JDK 17 is ready: $jdkHome"
Write-Host "Use gradlew-local.bat for Gradle commands."
