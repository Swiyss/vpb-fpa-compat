param(
    [switch]$Build,
    [string]$Profile = "C:\Users\joao2\AppData\Roaming\ModrinthApp\profiles\Good_Craft test version"
)

$ErrorActionPreference = "Stop"

$Repo = Resolve-Path (Join-Path $PSScriptRoot "..")
$Mods = Join-Path $Profile "mods"
$Backups = Join-Path $Profile "vpb-fpa-compat-backups"
$Libs = Join-Path $Repo "build\libs"
$Wrapper = Join-Path $Repo "gradlew.bat"

if (-not (Test-Path -LiteralPath $Mods -PathType Container)) {
    throw "GoodCraft mods folder was not found: $Mods"
}

if ($Build) {
    Push-Location $Repo
    try {
        & $Wrapper build
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

$Jar = Get-ChildItem -LiteralPath $Libs -Filter "vpb-fpa-compat-*.jar" |
    Where-Object { $_.Name -notlike "*-sources.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $Jar) {
    throw "No built vpb-fpa-compat jar found in $Libs. Run .\gradlew build first, or rerun this script with -Build."
}

$Existing = Get-ChildItem -LiteralPath $Mods -Filter "vpb-fpa-compat-*.jar"
$Moved = @()

if ($Existing.Count -gt 0) {
    $BackupDir = Join-Path $Backups (Get-Date -Format "yyyyMMdd-HHmmss")
    New-Item -ItemType Directory -Path $BackupDir | Out-Null

    foreach ($File in $Existing) {
        $Destination = Join-Path $BackupDir $File.Name
        Move-Item -LiteralPath $File.FullName -Destination $Destination
        $Moved += [pscustomobject]@{
            From = $File.FullName
            To = $Destination
        }
    }
}

$Target = Join-Path $Mods $Jar.Name
Copy-Item -LiteralPath $Jar.FullName -Destination $Target

Write-Host "VPB-FPA GoodCraft test install complete."
Write-Host "Copied:"
Write-Host "  $($Jar.FullName)"
Write-Host "  -> $Target"

if ($Moved.Count -gt 0) {
    Write-Host "Moved previous test jars:"
    foreach ($Item in $Moved) {
        Write-Host "  $($Item.From)"
        Write-Host "  -> $($Item.To)"
    }
}
else {
    Write-Host "Moved previous test jars: none"
}

Write-Host "No other GoodCraft mods, configs, resource packs, jars, zips, or content packs were changed by this script."
