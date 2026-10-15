"""Rebuild original provisional PCM clips and loops without external recordings or packages."""
from pathlib import Path
import math
import random
import struct
import wave

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "core" / "assets" / "audio"
DEST.relative_to(ROOT)
RATE = 22050
TAU = math.tau
DURATIONS = {
    "attack_bow": .22, "attack_sword": .25, "attack_pistol": .14, "attack_fists": .18,
    "attack_wand": .32, "attack_curse": .40, "attack_soul": .35, "attack_shield": .28,
    "hit": .18, "block": .28, "immune": .25, "retaliation": .35, "dash": .25,
    "xp": .22, "player_hurt": .35, "boss_warning": .65, "archer_warning": .30,
    "red_lock": .55, "level_up": .70, "chest": .50, "passage": .80,
    "resurrection": 1.2, "death": 1.4, "victory": 1.8,
}

def tone(freq, t):
    return math.sin(TAU * freq * t)

def note(freq, t, duration):
    if t < 0 or t >= duration:
        return 0.0
    envelope = min(1.0, t / .006) * (1 - t / duration) ** 1.8
    return envelope * (tone(freq, t) + .22 * tone(freq * 2, t) + .08 * tone(freq * 3, t))

def sequence(t, frequencies, total):
    spacing = total / (len(frequencies) + .5)
    return sum(note(freq, t - i * spacing, spacing * 1.45) for i, freq in enumerate(frequencies))

def effect(name, duration):
    rng = random.Random(701 + sum((i + 1) * ord(ch) for i, ch in enumerate(name)))
    data = []
    smooth = 0.0
    for index in range(round(duration * RATE)):
        t = index / RATE
        q = t / duration
        noise = rng.uniform(-1, 1)
        smooth = .8 * smooth + .2 * noise
        hiss = noise - smooth
        env = min(1, t / .005) * (1 - q) ** 2
        if name == "attack_bow":
            value = env * (hiss * .55 + tone(1700 - 900 * q, t) * .3)
        elif name == "attack_sword":
            value = env * (.7 * smooth + .35 * tone(700 - 500 * q, t)) + .18 * note(1600, t - .04, duration - .04)
        elif name == "attack_pistol":
            value = env * (.6 * hiss + .35 * tone(160 - 80 * q, t))
        elif name == "attack_fists":
            value = env * (.3 * smooth + tone(140 - 75 * q, t))
        elif name == "attack_wand":
            value = sequence(t, [587, 880, 1174], duration) * .5 + env * hiss * .12
        elif name == "attack_curse":
            value = env * (.5 * tone(420 - 180 * q, t) + .35 * tone(210 - 90 * q, t) + smooth * .2)
        elif name == "attack_soul":
            value = sequence(t, [294, 440, 659], duration) * .5 + env * smooth * .15
        elif name in ("attack_shield", "block"):
            value = env * (.45 * tone(740, t) + .3 * tone(1187, t) + .3 * hiss)
        elif name == "hit":
            value = env * (.4 * tone(200 - 80 * q, t) + hiss * .45)
        elif name == "dash":
            value = env * (hiss * .55 + .3 * tone(350 + 650 * q, t))
        elif name == "player_hurt":
            value = env * (.55 * tone(160 - 75 * q, t) + .35 * tone(227 - 100 * q, t) + .25 * smooth)
        elif name == "red_lock":
            value = sequence(t, [392, 294, 196], duration) * .5
        elif name == "boss_warning":
            value = sequence(t, [294, 392, 294], duration) * .5
        elif name == "archer_warning":
            value = sequence(t, [659, 440], duration) * .5
        else:
            frequencies = {
                "immune": [988, 1174], "retaliation": [392, 784, 1174], "xp": [1174, 1568],
                "level_up": [587, 740, 880, 1174], "chest": [440, 659, 880],
                "passage": [294, 440, 587], "resurrection": [294, 392, 440, 587, 880],
                "death": [294, 262, 220, 147], "victory": [294, 370, 440, 587, 740],
            }[name]
            value = sequence(t, frequencies, duration) * .55
        data.append(value)
    return data

def loop(combat):
    beat = .5 if combat else .75
    total = 32 * beat
    data = [0.0] * round(total * RATE)
    # D minor -> B-flat -> F -> C; deliberately simple provisional instrumental loop.
    chords = [(146.83, 174.61, 220.0), (116.54, 146.83, 174.61),
              (174.61, 220.0, 261.63), (130.81, 164.81, 196.0)]
    melody = [587.33, 440, 349.23, 440, 466.16, 349.23, 293.66, 349.23,
              523.25, 440, 349.23, 440, 392, 329.63, 261.63, 329.63]
    rng = random.Random(910 if combat else 911)
    for index in range(len(data)):
        t = index / RATE
        step = int(t / beat)
        chord = chords[step // 8]
        local = t - step * beat
        value = .10 * note(chord[step % 3], local, beat * .94)
        value += .08 * note(melody[(step // 2) % len(melody)], t - (step // 2) * beat * 2, beat * 1.9)
        value += .07 * note(chord[0] / 2, t - (step // 4) * beat * 4, beat * 3.8)
        if combat:
            value += .13 * note(65, local, beat * .65) if step % 2 == 0 else 0
            value += rng.uniform(-1, 1) * .05 * math.exp(-local * 65) * min(1, local / .002)
        # Zero at both boundaries, preventing a discontinuity when Music loops.
        value *= min(1, t / .02, (total - t) / .02)
        data[index] = value
    return data

def write(name, data, ceiling):
    peak = max(abs(value) for value in data)
    gain = min(1, ceiling / max(.001, peak))
    # Write PCM16 mono, supported directly by LWJGL/OpenAL.
    payload = bytearray()
    for value in data:
        payload.extend(struct.pack("<h", round(max(-1, min(1, value * gain)) * 32767)))
    with wave.open(str(DEST / (name + ".wav")), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(payload)

def main():
    DEST.mkdir(parents=True, exist_ok=True)
    for name, duration in DURATIONS.items():
        write(name, effect(name, duration), .62)
    write("refuge_loop", loop(False), .35)
    write("combat_loop", loop(True), .40)
    print(f"AUDIO_ASSETS_OK: {len(DURATIONS)} effects, 2 original loops; PCM16 mono {RATE} Hz")

if __name__ == "__main__":
    main()

