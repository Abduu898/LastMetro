package com.lastmetro.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimationPlayerTest {
    @Test
    void loopingClipAdvancesAndWrapsFrames() {
        AnimationPlayer player = new AnimationPlayer();
        player.add("run", new AnimationClip(4, 0.1, true));

        player.update(0.25);
        assertEquals(2, player.getCurrentFrame());
        player.update(0.2);
        assertEquals(0, player.getCurrentFrame());
        assertFalse(player.isFinished());
    }

    @Test
    void oneShotClipStopsOnItsLastFrame() {
        AnimationPlayer player = new AnimationPlayer();
        player.add("hit", new AnimationClip(3, 0.1, false));

        player.update(0.5);

        assertEquals(2, player.getCurrentFrame());
        assertTrue(player.isFinished());
    }

    @Test
    void switchingClipsStartsAtTheFirstFrame() {
        AnimationPlayer player = new AnimationPlayer();
        player.add("run", new AnimationClip(4, 0.1, true));
        player.add("jump", new AnimationClip(2, 0.2, false));
        player.update(0.25);

        player.play("jump");

        assertEquals("jump", player.getCurrentName());
        assertEquals(0, player.getCurrentFrame());
    }
}
