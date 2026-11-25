package com.lastmetro.audio;

import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioAssetsTest {
    @Test void allBundledSoundsDecodeWithoutAnAudioDevice() throws Exception {
        for (String name : new String[] {"ambience", "step", "jump", "land", "slide", "coin", "hit", "warning",
                "doors", "train", "win", "lose", "heal", "station", "hurry", "arrived", "missed"}) {
            var resource = GameAudio.class.getResource("/audio/" + name + ".wav");
            assertNotNull(resource, name);
            try (var stream = AudioSystem.getAudioInputStream(resource)) {
                assertEquals(22050, stream.getFormat().getSampleRate());
                assertEquals(16, stream.getFormat().getSampleSizeInBits());
                assertTrue(stream.getFrameLength() > 0, name);
            }
        }
    }
}
