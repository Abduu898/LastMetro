package com.lastmetro.view;

import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PixelRouteAssetsTest {
    @Test void allCroppedLayersAndTransferDecode() throws Exception {
        for (String name : new String[] {"street-far.png", "street-buildings.png", "street-walkway.png", "transfer.png",
                "underground-far.png", "underground-wall.png", "underground-front.png", "platform-ending.png"}) {
            try (var stream = getClass().getResourceAsStream("/assets/pixel-route/" + name)) {
                assertNotNull(stream, name); var image = ImageIO.read(stream); assertNotNull(image);
                assertTrue(image.getWidth() >= 2500); assertTrue(image.getHeight() >= 300);
                assertTrue((double) image.getWidth() / image.getHeight() > 1.6);
            }
        }
    }
    @Test void adultPixelCommuterHasTransparentPaddingAndEightUsablePoses() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/pixel-route/commuter.png")) {
            assertNotNull(stream); var image = ImageIO.read(stream);
            assertTrue(image.getColorModel().hasAlpha()); assertEquals(0, image.getRGB(0, 0) >>> 24);
            int w = image.getWidth() / 4, h = image.getHeight() / 2;
            for (int i = 0; i < 8; i++) {
                int pixels = 0;
                for (int y = 0; y < h; y += 8) for (int x = 0; x < w; x += 8)
                    if ((image.getRGB(i % 4 * w + x, i / 4 * h + y) >>> 24) > 40) pixels++;
                assertTrue(pixels > 300, "Empty animation cell " + i);
            }
        }
    }
    @Test void checkerboardWasRemovedFromLayerOpenings() throws Exception {
        for(String name:new String[]{"street-buildings.png","street-walkway.png","underground-wall.png","underground-front.png"}) {
            try(var stream=getClass().getResourceAsStream("/assets/pixel-route/"+name)) {
                var image=ImageIO.read(stream);int clear=0,total=0;
                for(int y=0;y<image.getHeight();y+=12)for(int x=0;x<image.getWidth();x+=12) {
                    total++;if((image.getRGB(x,y)>>>24)==0)clear++;
                }
                assertTrue(clear>total*.12,"Opaque checkerboard layer: "+name);
            }
        }
    }
}
