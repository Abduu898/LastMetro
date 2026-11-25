package com.lastmetro.model;

import com.lastmetro.feedback.RunFeedback;
import com.lastmetro.patterns.factory.HazardFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.lastmetro.feedback.RunFeedback.Event.*;

class HeartPickupTest {
    private GameWorld quiet(GameWorld.Mode mode) {
        return new GameWorld(1280,576,new HazardFactory() {
            @Override public Pattern choosePattern(int stage, boolean opening, Pattern previous) { return Pattern.EMPTY; }
        },mode);
    }
    private void tick(GameWorld w,double seconds){for(int i=0;i<(int)Math.round(seconds*120);i++)w.update(1.0/120);}
    private void hurt(GameWorld w){w.getHazards().clear();w.getHazards().add(new StreetObstacle(154,576,true));w.update(.001);}
    private void collect(GameWorld w){w.getHeartPickups().add(new HeartPickup(154,522));w.update(.001);}

    @Test void pickupRestoresExactlyOneLifeAndCanOnlyBeCollectedOnce() {
        var w=quiet(GameWorld.Mode.ENDLESS);hurt(w);assertEquals(2,w.getHearts());
        collect(w);assertEquals(3,w.getHearts());assertEquals(1,w.getHeartsCollected());
        assertTrue(w.wasLastHeartRestored());assertTrue(w.getHeartPickups().isEmpty());
        w.update(.001);assertEquals(1,w.getHeartsCollected());
    }
    @Test void fullHealthIsCappedAndPickupGivesBonusPoints() {
        var w=quiet(GameWorld.Mode.ENDLESS);int score=w.getScore();collect(w);
        assertEquals(GameWorld.MAX_HEARTS,w.getHearts());assertFalse(w.wasLastHeartRestored());
        assertEquals(score+HeartPickup.FULL_HEALTH_BONUS,w.getScore());
    }
    @Test void simultaneousDamageAndHealingBothProduceFeedback() {
        var w=quiet(GameWorld.Mode.ENDLESS);var feedback=new RunFeedback();feedback.update(w,.001);
        w.getHeartPickups().add(new HeartPickup(154,522));hurt(w);
        var events=feedback.update(w,.001);assertTrue(events.contains(HIT));assertTrue(events.contains(HEART));
        assertEquals(3,w.getHearts());assertFalse(feedback.update(w,.001).contains(HEART));
    }
    @Test void fatalCollisionCannotBeUndoneByAHeartInTheSameFrame() {
        var w=quiet(GameWorld.Mode.ENDLESS);hurt(w);tick(w,1.1);hurt(w);tick(w,1.1);
        w.getHeartPickups().add(new HeartPickup(154,522));hurt(w);
        assertTrue(w.isGameOver());assertEquals(0,w.getHearts());assertEquals(0,w.getHeartsCollected());
        w.update(1);assertEquals(0,w.getHearts());
    }
    @Test void heartsSpawnWithinReachRemainBoundedAndResetWithRoute() {
        var w = new GameWorld(1280, 576, new HazardFactory() {
            @Override public Pattern choosePattern(int stage, boolean opening, Pattern previous) { return Pattern.RECOVERY; }
        }, GameWorld.Mode.ENDLESS);
        tick(w, 9.1);
        assertFalse(w.getHeartPickups().isEmpty());assertEquals(428,w.getHeartPickups().getFirst().getY());
        w.reset();assertTrue(w.getHeartPickups().isEmpty());assertEquals(0,w.getHeartsCollected());
        int max=0;for(int i=0;i<8000;i++){w.update(.01);max=Math.max(max,w.getHeartPickups().size());}
        assertTrue(max>0 && max<=2);assertFalse(w.isGameOver());
        w.reset();assertEquals(0,w.getScore());assertEquals(3,w.getHearts());
    }
    @Test void transferClearsHeartsAndPreservesTheirTimersWhilePaused() {
        var w=quiet(GameWorld.Mode.CATCH_THE_METRO);tick(w,19);
        w.getHeartPickups().add(new HeartPickup(900,522));while (!w.isTransferring()) w.update(1.0 / 120);
        assertTrue(w.isTransferring());assertTrue(w.getHeartPickups().isEmpty());
        double time=w.getTimeRemaining();tick(w,1);assertEquals(time,w.getTimeRemaining());
    }
}
