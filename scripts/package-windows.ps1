$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$targetRoot = Join-Path $projectRoot 'target'
$inputDirectory = Join-Path $targetRoot 'package-input'
$distributionDirectory = Join-Path $targetRoot 'dist'
$appImageDirectory = Join-Path $distributionDirectory 'LastMetro'
$appJarName = 'LastMetro-1.0-SNAPSHOT.jar'
$packageId = [guid]::NewGuid().ToString('N')
$backupDirectory = Join-Path $projectRoot ('.lastmetro-package-backup-' + $packageId)
$tempRoot = [System.IO.Path]::GetFullPath($env:TEMP)
$stagingRoot = Join-Path $tempRoot ('LastMetro-package-' + $packageId)
$stagedAppImage = Join-Path $stagingRoot 'LastMetro'
$previousBuildSaved = $false

Push-Location $projectRoot
try {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw 'Apache Maven was not found. Install Maven and make sure mvn is available on PATH.'
    }
    if (-not (Get-Command jar -ErrorAction SilentlyContinue)) {
        throw 'jar was not found. Install a full JDK 21 and make sure its bin folder is available on PATH.'
    }
    if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
        throw 'jpackage was not found. Install a full JDK 21 and make sure its bin folder is available on PATH.'
    }

    $resolvedTarget = [System.IO.Path]::GetFullPath($targetRoot).TrimEnd('\') + '\'
    $resolvedProject = [System.IO.Path]::GetFullPath($projectRoot).TrimEnd('\') + '\'
    if (-not $backupDirectory.StartsWith($resolvedProject, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Unsafe package-backup path: $backupDirectory"
    }
    if (-not $stagingRoot.StartsWith($tempRoot.TrimEnd('\') + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Unsafe package-staging path: $stagingRoot"
    }

    if (Test-Path $appImageDirectory) {
        $oldImage = Get-Item -LiteralPath $appImageDirectory -Force
        $resolvedOldImage = (Resolve-Path -LiteralPath $appImageDirectory).Path
        $isUnexpectedPath = -not $resolvedOldImage.StartsWith($resolvedTarget, [System.StringComparison]::OrdinalIgnoreCase)
        $isReparsePoint = [bool]($oldImage.Attributes -band [System.IO.FileAttributes]::ReparsePoint)
        if ($isUnexpectedPath -or $isReparsePoint) {
            throw "Refusing to move an unexpected app-image path: $resolvedOldImage"
        }
        Move-Item -LiteralPath $appImageDirectory -Destination $backupDirectory
        $previousBuildSaved = $true
    }

    & mvn -B clean test
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build or test failed with exit code $LASTEXITCODE."
    }

    $mainClass = Join-Path $targetRoot 'classes\com\lastmetro\MainApp.class'
    if (-not (Test-Path $mainClass)) {
        throw "The compiled game entry point was not created: $mainClass"
    }

    New-Item -ItemType Directory -Path $inputDirectory,$distributionDirectory,$stagingRoot -Force | Out-Null
    $appJar = Join-Path $inputDirectory $appJarName
    & jar --create --file $appJar --main-class com.lastmetro.MainApp -C (Join-Path $targetRoot 'classes') .
    if ($LASTEXITCODE -ne 0) {
        throw "Could not create the game jar (exit code $LASTEXITCODE)."
    }

    $versionMatch = [regex]::Match((Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw), '<javafx\.version>(?<version>[^<]+)</javafx\.version>')
    if (-not $versionMatch.Success) {
        throw 'Could not read javafx.version from pom.xml.'
    }
    $javafxVersion = $versionMatch.Groups['version'].Value
    $mavenRepository = Join-Path $env:USERPROFILE '.m2\repository\org\openjfx'
    foreach ($module in @('javafx-base','javafx-graphics','javafx-controls')) {
        $moduleJar = Join-Path (Join-Path (Join-Path $mavenRepository $module) $javafxVersion) ("$module-$javafxVersion-win.jar")
        if (-not (Test-Path $moduleJar)) {
            throw "The cached Windows JavaFX module is missing: $moduleJar"
        }
        Copy-Item -LiteralPath $moduleJar -Destination $inputDirectory
    }

    & jpackage `
        --type app-image `
        --input $inputDirectory `
        --dest $stagingRoot `
        --name LastMetro `
        --app-version 0.1.0 `
        --vendor 'Last Metro' `
        --main-jar $appJarName `
        --main-class com.lastmetro.MainApp `
        --java-options '--enable-native-access=javafx.graphics' `
        --java-options '--module-path $APPDIR --add-modules javafx.controls,javafx.graphics'
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE."
    }
    if (-not (Test-Path (Join-Path $stagedAppImage 'LastMetro.exe'))) {
        throw "The staged app image is missing its launcher: $stagedAppImage"
    }

    Copy-Item -LiteralPath $stagedAppImage -Destination $appImageDirectory -Recurse
    if (-not (Test-Path (Join-Path $appImageDirectory 'LastMetro.exe'))) {
        throw "The app image is missing its launcher: $appImageDirectory"
    }

    if ($previousBuildSaved) {
        $resolvedBackup = (Resolve-Path -LiteralPath $backupDirectory).Path
        if (-not $resolvedBackup.StartsWith($resolvedProject, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove an unexpected backup path: $resolvedBackup"
        }
        Remove-Item -LiteralPath $resolvedBackup -Recurse -Force
        $previousBuildSaved = $false
    }

    Write-Host "Windows app image created at $appImageDirectory"
    Write-Host 'Run LastMetro.exe inside that folder to start the game.'
}
catch {
    if ($previousBuildSaved -and (Test-Path $backupDirectory)) {
        if (Test-Path $appImageDirectory) {
            $partialImage = Get-Item -LiteralPath $appImageDirectory -Force
            if ($partialImage.Attributes -band [System.IO.FileAttributes]::ReparsePoint) {
                throw "Refusing to remove an unexpected partial app-image path: $appImageDirectory"
            }
            Remove-Item -LiteralPath $appImageDirectory -Recurse -Force
        }
        New-Item -ItemType Directory -Path $distributionDirectory -Force | Out-Null
        Move-Item -LiteralPath $backupDirectory -Destination $appImageDirectory
    }
    throw
}
finally {
    if (Test-Path $stagingRoot) {
        $resolvedStaging = (Resolve-Path -LiteralPath $stagingRoot).Path
        $resolvedTemp = [System.IO.Path]::GetFullPath($env:TEMP).TrimEnd('\') + '\'
        $stagingInfo = Get-Item -LiteralPath $resolvedStaging -Force
        $isUnexpectedStage = -not $resolvedStaging.StartsWith($resolvedTemp, [System.StringComparison]::OrdinalIgnoreCase)
        $isStageReparsePoint = [bool]($stagingInfo.Attributes -band [System.IO.FileAttributes]::ReparsePoint)
        if ($isUnexpectedStage -or $isStageReparsePoint) {
            throw "Refusing to remove an unexpected package-staging path: $resolvedStaging"
        }
        Remove-Item -LiteralPath $resolvedStaging -Recurse -Force
    }
    Pop-Location
}
