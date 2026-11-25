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
    private void key(KeyCode code) {
        stage.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
        stage.getScene().getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
    }
    private void settle() throws Exception { ((ScreenTransition) field(app, "screenTransition")).update(.5); }
    private void capture(String name) throws Exception {
        var m = MainApp.class.getDeclaredMethod("drawFrame", Canvas.class, GraphicsContext.class); m.setAccessible(true);
        m.invoke(app, canvas, canvas.getGraphicsContext2D());
        var image = canvas.snapshot(null, null);
        var output = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[1280 * 720];
        image.getPixelReader().getPixels(0, 0, 1280, 720, javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, 1280);
        output.setRGB(0, 0, 1280, 720, pixels, 0, 1280);
        ImageIO.write(output, "png", Path.of("target/preview", name + ".png").toFile());
    }
    private GameWorld quietWorld(GameWorld.Mode mode) throws Exception {
        var world = new GameWorld(1280, MainApp.GROUND_Y, new HazardFactory() {
            @Override public Hazard createRandomHazard(double x, double y) { return new EScooter(1e9, y); }
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
        capture("menu"); key(KeyCode.H); settle(); capture("help"); key(KeyCode.ENTER); settle(); key(KeyCode.O); settle(); capture("settings"); key(KeyCode.ENTER); settle();
        key(KeyCode.DIGIT1); settle();
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
            if (world.isFinaleActive() && world.getFinaleTime() > .8 && world.getFinaleTime() < .82) capture("train");
            if (world.isGameOver()) break;
        }
        settle(); renderer.updateUi(.1, true); capture("victory");
        if (!world.isPlayerWon()) throw new AssertionError("Catch mode did not win");
        key(KeyCode.R); settle();
        if (((GameWorld) field(app, "world")).isGameOver()) throw new AssertionError("Retry failed");
        world = quietWorld(GameWorld.Mode.ENDLESS);
        for (int hit = 0; hit < 3; hit++) {
            world.getHazards().clear(); world.getHazards().add(new EScooter(154, MainApp.GROUND_Y));
            invoke("updateSimulation", double.class, 1.0 / 60);
            if (hit < 2) for (int i = 0; i < 70; i++) invoke("updateSimulation", double.class, 1.0 / 60);
        }
        settle(); capture("defeat");
        if (!world.isGameOver()) throw new AssertionError("Defeat failed");
        System.out.printf("SMOKE PASS: menu, settings, jump, pickup, impact, heart healing, mouse pause/resume, motion toggle, train, victory, retry, endless defeat. Capture average %.1f ms (includes PNG encoding).%n", rendered == 0 ? 0 : renderNanos / 1e6 / rendered);
        app.stop(); stage.close(); javafx.application.Platform.exit();
    }
    public static void main(String[] args) { launch(args); }
}
