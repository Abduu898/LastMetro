import com.lastmetro.MainApp;
import com.lastmetro.animation.ScreenTransition;
import com.lastmetro.model.*;
import com.lastmetro.patterns.factory.HazardFactory;
import com.lastmetro.view.*;
import javafx.application.Application;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.*;
import javafx.stage.Stage;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Desktop integration check and a short reproducible gameplay capture. Uses an isolated user.home. */
public class PreviewSmoke extends Application {
    private MainApp app;
    private Stage stage;
    private GameRenderer renderer;
    private final Canvas canvas = new Canvas(1280, 720);
    private static Object field(Object owner, String name) throws Exception {
        var f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    private void invoke(String name, Class<?> type, Object value) throws Exception {
        var m = MainApp.class.getDeclaredMethod(name, type); m.setAccessible(true); m.invoke(app, value);
    }
    private void invokeMouseClick(double x, double y) throws Exception {
        var click = MainApp.class.getDeclaredMethod("handleMouseClick", double.class, double.class);
        click.setAccessible(true);
        click.invoke(app, x, y);
    }

    private void key(KeyCode code) {
        stage.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
        stage.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }
    private void settle() throws Exception { ((ScreenTransition) field(app, "screenTransition")).update(.5); }
    private void capture(String name) throws Exception { capture(name, 1280, 720); }

    private void capture(String name, int width, int height) throws Exception {
        canvas.setWidth(width);
        canvas.setHeight(height);
        var m = MainApp.class.getDeclaredMethod("drawFrame", Canvas.class, GraphicsContext.class); m.setAccessible(true);
        m.invoke(app, canvas, canvas.getGraphicsContext2D());
        var image = canvas.snapshot(null, null);
        var output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, width);
        output.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(output, "png", Path.of("target/preview", name + ".png").toFile());
    }
    private GameWorld quietWorld(GameWorld.Mode mode) throws Exception {
        var world = new GameWorld(1280, MainApp.GROUND_Y, new HazardFactory() {
            @Override public Pattern choosePattern(int stage, boolean opening, Pattern previous) { return Pattern.EMPTY; }
        }, mode);
        world.addObserver(app);
        var f = MainApp.class.getDeclaredField("world"); f.setAccessible(true); f.set(app, world);
        renderer.resetAnimations(); return world;
    }
    @Override public void start(Stage stage) throws Exception {
        this.stage = stage;
        Files.createDirectories(Path.of("target/preview/frames"));
        Files.createDirectories(Path.of(System.getProperty("user.home"), ".lastmetro"));
        Files.writeString(Path.of(System.getProperty("user.home"), ".lastmetro/settings.properties"), "sound=false\n");
        app = new MainApp(); app.start(stage); renderer = (GameRenderer) field(app, "renderer");
        renderer.updateUi(.1); renderer.updateUi(.1);
        capture("menu");
        capture("menu-1080p", 1920, 1080);
        key(KeyCode.UP);
        if (!field(app, "menuSelection").equals(3)) throw new AssertionError("Up did not wrap");
        key(KeyCode.S); key(KeyCode.DOWN);
        if (!field(app, "menuSelection").equals(1)) throw new AssertionError("Down and S did not advance");
        capture("menu-endless");
        key(KeyCode.SPACE); settle();
        if (((GameWorld) field(app, "world")).getMode() != GameWorld.Mode.ENDLESS)
            throw new AssertionError("Space did not start the selected Endless mode");
        var initialScreen = MainApp.class.getDeclaredField("screen");
        initialScreen.setAccessible(true);
        initialScreen.set(app, GameScreen.MENU);
        key(KeyCode.H); settle();
        if (field(app, "screen") != GameScreen.HOW_TO_PLAY) throw new AssertionError("H did not open help");
        capture("help");
        key(KeyCode.ESCAPE); settle();
        if (field(app, "screen") != GameScreen.MENU) throw new AssertionError("Escape did not close help");
        key(KeyCode.O); settle();
        if (field(app, "screen") != GameScreen.SETTINGS) throw new AssertionError("O did not open settings");
        capture("settings");
        var back = TicketKiosk.BACK;
        invokeMouseClick(back.getMinX() + 10, back.getMinY() + 10);
        settle();
        if (field(app, "screen") != GameScreen.MENU) throw new AssertionError("Back did not close settings");
        key(KeyCode.W); key(KeyCode.ENTER); settle();
        if (field(app, "screen") != GameScreen.HOW_TO_PLAY) throw new AssertionError("Enter did not open selected help");
        key(KeyCode.ESCAPE); settle();
        capture("menu-pointer-1080p", 1920, 1080);
        stage.getScene().getRoot().fireEvent(new MouseEvent(MouseEvent.MOUSE_MOVED, 1050, 620, 1050, 620,
                MouseButton.NONE, 0, false, false, false, false, false, false, false, false, false, false, null));
        if (!field(app, "menuSelection").equals(1)) throw new AssertionError("1080p hover did not select Endless");
        invokeMouseClick(1050, 620);
        settle();
        if (((GameWorld) field(app, "world")).getMode() != GameWorld.Mode.ENDLESS)
            throw new AssertionError("1080p click did not start Endless");
        initialScreen.set(app, GameScreen.MENU);
        key(KeyCode.DIGIT1); settle();
        if (((GameWorld) field(app, "world")).getMode() != GameWorld.Mode.CATCH_THE_METRO)
            throw new AssertionError("1 did not start the timed mode");
        var world = quietWorld(GameWorld.Mode.CATCH_THE_METRO);
        var soundtrack = new com.lastmetro.feedback.RunFeedback();
        var eventLog = new StringBuilder("0.000\tstation\n");
        long renderNanos = 0; int rendered = 0;
        for (int i = 0; i < 480; i++) {
            if (i == 15) world.getHazards().add(new EScooter(900, MainApp.GROUND_Y));
            if (i == 90) world.getPlayer().onJumpPressed();
            if (i == 125) world.getPlayer().onJumpReleased();
            if (i == 180) world.getHazards().add(new Pigeon(850, MainApp.GROUND_Y));
            if (i == 240) world.getPlayer().onSlideChanged(true);
            if (i == 285) world.getPlayer().onSlideChanged(false);
            if (i == 330) world.getTickets().add(new Ticket(210, 480));
            if (i == 410) world.getHazards().add(new EScooter(195, MainApp.GROUND_Y));
            invoke("updateSimulation", double.class, 1.0 / 60);
            for (var event : soundtrack.update(world, 1.0 / 60)) {
                String sound = switch (event) {
                    case COIN, TICKET -> "coin";
                    case TRAIN_ARRIVE, TRAIN_DEPART -> "train";
                    default -> event.name().toLowerCase(java.util.Locale.ROOT);
                };
                eventLog.append(String.format(java.util.Locale.ROOT, "%.3f\t%s%n", i / 60.0, sound));
            }
            renderer.updateUi(1.0 / 60);
            if (i == 100) capture("jump"); if (i == 350) capture("pickup"); if (i == 412) capture("impact");
            if (i % 3 == 0 && Boolean.parseBoolean(System.getProperty("lastmetro.captureFrames", "true"))) {
                long started = System.nanoTime();
                capture(String.format("frames/%04d", i / 3));
                renderNanos += System.nanoTime() - started; rendered++;
            }
        }
        Files.writeString(Path.of("target/preview/events.tsv"), eventLog);
        int beforeHealth=world.getHearts();
        int beforeHeartPickups=world.getHeartsCollected();
        world.getHeartPickups().add(new HeartPickup(154,MainApp.GROUND_Y-54));
        invoke("updateSimulation",double.class,1.0/60);
        if(world.getHearts()!=Math.min(GameWorld.MAX_HEARTS,beforeHealth+1)
                || world.getHeartsCollected()!=beforeHeartPickups+1)throw new AssertionError("Heart pickup failed");
        capture("heart-restored");
        world.getHeartPickups().add(new HeartPickup(700,MainApp.GROUND_Y-54));
        world.getHazards().clear();
        world.getHazards().add(new StreetObstacle(480, MainApp.GROUND_Y, false));
        world.getHazards().add(new StreetObstacle(850, MainApp.GROUND_Y, true));
        for (int i = 0; i < 6; i++) world.getCoins().add(new Coin(340 + i * 45, MainApp.GROUND_Y - 80 - Math.sin(i * Math.PI / 5) * 75));
        capture("street");
        stage.getScene().getRoot().fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 1232, 40, 1232, 40,
                MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false, false, false, null));
        settle();
        if (field(app, "screen") != GameScreen.PAUSED) throw new AssertionError("Pause failed");
        capture("pause"); key(KeyCode.P); settle();
        key(KeyCode.F9);
        if (!((com.lastmetro.persistence.GameSettings) field(app, "settings")).reducedMotion()) throw new AssertionError("Motion toggle failed");
        capture("reduced-motion"); key(KeyCode.F9);
        // Clean route with real countdown, train, feedback and result transition.
        world = quietWorld(GameWorld.Mode.CATCH_THE_METRO);
        for (int i = 0; i < 3400; i++) {
            invoke("updateSimulation", double.class, 1.0 / 60);
            if (world.isTransferring() && world.getTransferProgress() > .45 && world.getTransferProgress() < .46) capture("transfer-" + world.getRouteStage());
            if (world.getRouteStage() == 1 && world.getStageProgress() > .48 && world.getStageProgress() < .482) capture("continuation");
            if (world.getRouteStage() == 2 && world.getStageProgress() > .45 && world.getStageProgress() < .452) capture("underground");
            if (!world.isTransferring() && world.getRouteStage() < 2
                    && world.getStageProgress() > .966 && world.getStageProgress() < .969)
                capture("entrance-" + world.getRouteStage());
            if (world.isFinaleActive() && world.getFinaleTime() > 2.40 && world.getFinaleTime() < 2.42) capture("train-doors");
            if (world.isFinaleActive() && world.getFinaleTime() > 3.69 && world.getFinaleTime() < 3.71) capture("departure");
            if (world.isFinaleActive() && world.getFinaleTime() > .8 && world.getFinaleTime() < .82) capture("train");
            if (world.isGameOver()) break;
        }
        settle();
        if (!world.isPlayerWon() || field(app, "screen") != GameScreen.RESULTS)
            throw new AssertionError("Catch mode did not open victory ticket");
        key(KeyCode.F9);
        capture("victory-reduced-motion");
        if (renderer.updateVictory(.2) != null) throw new AssertionError("Reduced motion played a ticket cue");
        key(KeyCode.F9);
        renderer.resetAnimations();
        int ticks = 0, stamps = 0;
        for (int i = 0; i < 60; i++) {
            String cue = renderer.updateVictory(1.0 / 60);
            if ("tick".equals(cue)) ticks++;
            if ("stamp".equals(cue)) stamps++;
        }
        if (ticks != 6 || stamps != 1) throw new AssertionError("Wrong number of ticket audio cues");
        capture("victory");
        capture("victory-1080p", 1920, 1080);
        key(KeyCode.SPACE); settle();
        if (((GameWorld) field(app, "world")).isGameOver() || field(app, "screen") != GameScreen.PLAYING)
            throw new AssertionError("Space did not retry the run");
        var screenField = MainApp.class.getDeclaredField("screen");
        screenField.setAccessible(true);
        var worldField = MainApp.class.getDeclaredField("world");
        worldField.setAccessible(true);
        for (KeyCode shortcut : new KeyCode[] {KeyCode.ENTER, KeyCode.R, KeyCode.ESCAPE}) {
            worldField.set(app, world);
            screenField.set(app, GameScreen.RESULTS);
            key(shortcut); settle();
            GameScreen expected = shortcut == KeyCode.ESCAPE ? GameScreen.MENU : GameScreen.PLAYING;
            if (field(app, "screen") != expected) throw new AssertionError("Victory shortcut failed: " + shortcut);
        }
        worldField.set(app, world);
        screenField.set(app, GameScreen.RESULTS);
        var menu = VictoryTicket.MENU;
        invokeMouseClick((menu.getMinX() + menu.getWidth() / 2) * 1.5,
                (menu.getMinY() + menu.getHeight() / 2) * 1.5);
        settle();
        if (field(app, "screen") != GameScreen.MENU) throw new AssertionError("1080p ticket button missed");
        key(KeyCode.DIGIT2); settle();
        if (((GameWorld) field(app, "world")).getMode() != GameWorld.Mode.ENDLESS)
            throw new AssertionError("2 did not start Endless");
        world = quietWorld(GameWorld.Mode.ENDLESS);
        for (int hit = 0; hit < 3; hit++) {
            world.getHazards().clear(); world.getHazards().add(new EScooter(154, MainApp.GROUND_Y));
            invoke("updateSimulation", double.class, 1.0 / 60);
            if (hit < 2) for (int i = 0; i < 70; i++) invoke("updateSimulation", double.class, 1.0 / 60);
        }
        settle(); capture("defeat");
        if (!world.isGameOver()) throw new AssertionError("Defeat failed");
        System.out.printf("SMOKE PASS: 720p/1080p kiosk, four menu actions, keyboard navigation, scaled pointer, settings, jump, pickup, impact, heart healing, mouse pause/resume, motion toggle, train, 720p/1080p victory ticket, reduced-motion stamp, retry/menu inputs, endless defeat. Capture average %.1f ms (includes PNG encoding).%n", rendered == 0 ? 0 : renderNanos / 1e6 / rendered);
        app.stop(); stage.close(); javafx.application.Platform.exit();
    }
    public static void main(String[] args) { launch(args); }
}
