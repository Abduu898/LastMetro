"""Mix the desktop smoke capture's real simulation event log into a preview soundtrack."""
from pathlib import Path
from array import array
import sys
import wave

ROOT = Path(__file__).resolve().parents[1]
RATE, SECONDS = 22050, 8
audio = ROOT / 'src/main/resources/audio'
preview = ROOT / 'target/preview'
mix = [0.0] * (RATE * SECONDS)

def read(name):
    path = audio / (name + '.wav')
    if not path.exists(): return array('h')
    with wave.open(str(path), 'rb') as wav:
        assert wav.getframerate() == RATE and wav.getnchannels() == 1
        data = array('h', wav.readframes(wav.getnframes()))
        if sys.byteorder != 'little': data.byteswap()
        return data

def add(samples, start, volume):
    offset = int(start * RATE)
    for i, sample in enumerate(samples[:max(0, len(mix) - offset)]):
        mix[offset + i] += sample * volume

add(read('ambience'), 0, .22)
for line in (preview / 'events.tsv').read_text().splitlines():
    time, name = line.split('\t')
    add(read(name), float(time), .15 if name == 'step' else .8 if name == 'station' else .5)
samples = array('h', (int(max(-31500, min(31500, value))) for value in mix))
if sys.byteorder != 'little': samples.byteswap()
with wave.open(str(preview / 'soundtrack.wav'), 'wb') as wav:
    wav.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
    wav.writeframes(samples.tobytes())
print('Rendered synchronized preview soundtrack')
