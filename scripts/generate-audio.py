"""Original synthesized effects; deterministic mono PCM, no external libraries or samples."""
from pathlib import Path
import math
import random
import struct
import wave

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/audio'
ROOT.mkdir(parents=True, exist_ok=True)
RATE = 22050
rng = random.Random(12)

def write(name, duration, sample):
    data = bytearray()
    for i in range(int(duration * RATE)):
        t = i / RATE
        fade = min(1, t * 100, (duration - t) * 60)
        value = max(-.96, min(.96, sample(t) * fade))
        data.extend(struct.pack('<h', int(value * 32767)))
    with wave.open(str(ROOT / (name + '.wav')), 'wb') as wav:
        wav.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        wav.writeframes(data)

def sine(hz, t): return math.sin(2 * math.pi * hz * t)
def noise(): return rng.uniform(-1, 1)

write('step', .10, lambda t: (noise() * .32 + sine(140, t) * .3) * math.exp(-t * 45))
write('land', .22, lambda t: (noise() * .35 + sine(75, t) * .5) * math.exp(-t * 22))
write('jump', .23, lambda t: (sine(220 + 1300 * t, t) * .23 + noise() * .05) * (1 - t / .23))
write('slide', .40, lambda t: noise() * .23 * math.sin(math.pi * t / .4))
write('hit', .28, lambda t: (noise() * .5 + sine(95 - t * 90, t) * .35) * math.exp(-t * 12))
write('coin', .32, lambda t: (sine(1046 if t < .08 else 1568, t) * .32 + sine(2093, t) * .10) * math.exp(-t * 9))
write('warning', .17, lambda t: sine(880, t) * .17)
write('doors', .7, lambda t: (sine(740, t) * .17 if t < .45 else noise() * .45 * math.exp(-(t - .45) * 25)))
write('train', 2.4, lambda t: (noise() * .22 + sine(52 + t * 9, t) * .23 + sine(180 + t * 130, t) * .06) * math.sin(math.pi * t / 2.4))
notes = [523.25, 659.25, 783.99, 1046.5]
write('win', 1.6, lambda t: sine(notes[min(3, int(t / .18))], t) * .26 * math.exp(-(t % .18) * 4) * min(1, (1.6 - t) * 2))
write('lose', .8, lambda t: sine(330 - t * 180, t) * .23 * math.exp(-t * 3))
write('heal', .55, lambda t: (sine(659.25 if t < .18 else 987.77, t) * .23 + sine(1318.5, t) * .08) * math.exp(-t * 4))
# Generate a seamless, periodic bed of city air and a distant train motor.
cycle = [noise() for _ in range(RATE * 8)]
write('ambience', 8, lambda t: sine(55, t) * .13 + sine(82.5, t) * .07 + sine(110, t) * .035
      + cycle[min(len(cycle) - 1, int(t * RATE))] * .045 + sine(220, t) * .018 * (1 + sine(.125, t)))
print(f'Generated effects in {ROOT}')
