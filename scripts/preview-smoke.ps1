param([switch]$NoFrames)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $projectRoot
try {
    & mvn -q test-compile dependency:build-classpath '-Dmdep.outputFile=target/classpath.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    $fxModules = ((Get-Content target/classpath.txt) -split ';' | Where-Object { $_ -match 'javafx-.*-win.jar$' }) -join ';'
    New-Item -ItemType Directory -Force target/preview-classes | Out-Null
    & javac --module-path $fxModules --add-modules javafx.controls -cp target/classes -d target/preview-classes scripts/PreviewSmoke.java
    if ($LASTEXITCODE -ne 0) { throw 'Preview compilation failed' }
    $captureFrames = if ($NoFrames) { 'false' } else { 'true' }
    & java '-Duser.home=target/smoke-home' "-Dlastmetro.captureFrames=$captureFrames" --enable-native-access=javafx.graphics --module-path $fxModules --add-modules javafx.controls -cp 'target/classes;target/preview-classes' PreviewSmoke
    if ($LASTEXITCODE -ne 0) { throw 'Desktop smoke check failed' }
} finally { Pop-Location }
