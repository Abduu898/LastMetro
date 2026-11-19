$ErrorActionPreference = 'Stop'

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$targetRoot = Join-Path $projectRoot 'target'
$inputDirectory = Join-Path $targetRoot 'package-input'
$distributionDirectory = Join-Path $targetRoot 'dist'
$appImageDirectory = Join-Path $distributionDirectory 'LastMetro'
$appJarName = 'LastMetro-1.0-SNAPSHOT.jar'

Push-Location $projectRoot
try {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw 'Apache Maven was not found. Install Maven and make sure mvn is available on PATH.'
    }
    if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
        throw 'jpackage was not found. Install a full JDK 21 and make sure its bin folder is available on PATH.'
    }

    if (Test-Path $inputDirectory) {
        Remove-Item -LiteralPath $inputDirectory -Recurse -Force
    }
    if (Test-Path $appImageDirectory) {
        Remove-Item -LiteralPath $appImageDirectory -Recurse -Force
    }
    New-Item -ItemType Directory -Path $inputDirectory -Force | Out-Null
    New-Item -ItemType Directory -Path $distributionDirectory -Force | Out-Null

    & mvn -B clean package org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies `
        '-DincludeScope=runtime' `
        "-DoutputDirectory=$inputDirectory"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE."
    }

    $builtJar = Join-Path $targetRoot $appJarName
    if (-not (Test-Path $builtJar)) {
        throw "Expected application jar was not created: $builtJar"
    }
    Copy-Item -LiteralPath $builtJar -Destination (Join-Path $inputDirectory $appJarName)

    & jpackage `
        --type app-image `
        --input $inputDirectory `
        --dest $distributionDirectory `
        --name LastMetro `
        --app-version 0.1.0 `
        --vendor 'Last Metro' `
        --main-jar $appJarName `
        --main-class com.lastmetro.MainApp `
        --java-options '--module-path $APPDIR --add-modules javafx.controls,javafx.graphics'
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE."
    }

    Write-Host "Windows app image created at $appImageDirectory"
    Write-Host 'Run LastMetro.exe inside that folder to start the game.'
}
finally {
    Pop-Location
}
