package com.lastmetro.view;

import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpriteStripTest {
    @BeforeAll
    static void startJavaFx() {
        Platform.startup(() -> { });
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void loadsExplicitFramesFromTheSuppliedCharacterAtlas() {
        var strip = SpriteStrip.loadFrames(SpriteStrip.class, "/assets/sprites/commuter-atlas.png",
                new Rectangle2D(65, 34, 181, 208),
                new Rectangle2D(334, 34, 186, 208),
                new Rectangle2D(631, 34, 177, 208),
                new Rectangle2D(887, 34, 182, 208));

        assertTrue(strip.isPresent());
        assertEquals(4, strip.orElseThrow().getFrameCount());
    }

    @Test
    void rejectsFramesThatExtendBeyondTheAtlas() {
        assertTrue(SpriteStrip.loadFrames(SpriteStrip.class, "/assets/sprites/commuter-atlas.png",
                new Rectangle2D(1670, 920, 20, 30)).isEmpty());
    }

    @Test
    void loadsHazardsCollectiblesAndHeartFramesFromTheObjectAtlas() {
        var strip = SpriteStrip.loadFrames(SpriteStrip.class, "/assets/sprites/objects-atlas.png",
                new Rectangle2D(90, 50, 61, 109),
                new Rectangle2D(65, 225, 214, 133),
                new Rectangle2D(91, 423, 134, 119),
                new Rectangle2D(63, 614, 187, 115),
                new Rectangle2D(355, 842, 51, 47));

        assertTrue(strip.isPresent());
        assertEquals(5, strip.orElseThrow().getFrameCount());
    }

    @Test
    void loadsTheSuppliedParisSceneComposite() {
        var resource = SpriteStrip.class.getResource("/assets/scenes/paris-composite.jpg");
        assertNotNull(resource);
        var image = new javafx.scene.image.Image(resource.toExternalForm(), false);

        assertFalse(image.isError());
        assertEquals(2752.0, image.getWidth());
        assertEquals(1536.0, image.getHeight());
    }
}
