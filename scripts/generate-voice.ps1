# Offline station announcements with a narrow-band public-address/echo effect.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$audioRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../src/main/resources/audio'))
$tempRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../target/voice'))
New-Item -ItemType Directory -Force $audioRoot, $tempRoot | Out-Null
$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
$speaker.SelectVoice('Microsoft Zira Desktop')
$speaker.Rate = -1
$lines = @{
    station = 'Attention please. Last metro in forty five seconds. Mind the closing doors.'
    hurry = 'Last departure. Ten seconds. Hurry!'
    arrived = 'You made it. Welcome aboard the last metro.'
    missed = 'Service has ended. The city never waits. Try again.'
}
try {
    foreach ($name in $lines.Keys) {
        $raw = Join-Path $tempRoot "$name.wav"
        $speaker.SetOutputToWaveFile($raw)
        $speaker.Speak($lines[$name])
        $speaker.SetOutputToNull()
        & ffmpeg -hide_banner -loglevel error -y -i $raw -af 'highpass=f=350,lowpass=f=3400,aecho=0.8:0.65:85:0.18,alimiter=limit=0.85' -ar 22050 -ac 1 (Join-Path $audioRoot "$name.wav")
        if ($LASTEXITCODE -ne 0) { throw "Could not filter $name" }
    }
} finally { $speaker.Dispose() }
