package np.cincuentazo.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Centralises all scene-navigation logic for the Cincuentazo application.
 *
 * <p>Before this class existed, every controller loaded FXML, created a
 * {@link Scene}, obtained the {@link Stage} from a node, called
 * {@link Stage#setScene(Scene)} and {@link Stage#setTitle(String)} inline
 * — the same five-line boilerplate repeated in four different places.
 * {@code SceneNavigator} extracts that boilerplate into one place and
 * exposes three semantic, destination-specific methods so callers read as
 * plain intent.</p>
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li><b>Final / utility class:</b> all methods are {@code static} and the
 *       constructor is private. There is nothing to instantiate — the class
 *       is a collection of pure functions that transform a JavaFX scene
 *       graph.</li>
 *   <li><b>Stage obtained from a {@link Node}:</b> controllers do not hold a
 *       direct reference to the stage. Instead, any node currently attached
 *       to the scene graph can reach the stage via
 *       {@code node.getScene().getWindow()}. This keeps controllers
 *       stage-agnostic and easy to test.</li>
 *   <li><b>Unchecked wrapping:</b> {@link IOException} from {@link FXMLLoader}
 *       is wrapped in {@link RuntimeException}. Navigation failures are
 *       programming errors (wrong FXML path), not recoverable user-facing
 *       conditions, so forcing every call site to handle a checked exception
 *       would be noise.</li>
 *   <li><b>Single package:</b> placed alongside the controllers that use it
 *       ({@code np.cincuentazo.controller}) so no new package declaration is
 *       needed in {@code module-info.java}.</li>
 * </ul>
 *
 * <h2>SRP</h2>
 * <p>This class has exactly one reason to change: the mechanics of how the
 * application switches between its three screens. If a fourth screen is ever
 * added, only this class and its callers change — not the navigation
 * logic scattered across every controller.</p>
 */
public final class SceneNavigator {

    // -------------------------------------------------------------------------
    // FXML resource paths
    // -------------------------------------------------------------------------

    /** Resource path for the start / configuration screen (HU-1). */
    private static final String FXML_START   = "/np/cincuentazo/start-view.fxml";

    /** Resource path for the main game screen. */
    private static final String FXML_GAME    = "/np/cincuentazo/game-view.fxml";

    /** Resource path for the victory / results screen (HU-6). */
    private static final String FXML_VICTORY = "/np/cincuentazo/victory-view.fxml";

    // -------------------------------------------------------------------------
    // Private constructor — utility class, not instantiable
    // -------------------------------------------------------------------------

    private SceneNavigator() {
        throw new AssertionError("SceneNavigator is a utility class and must not be instantiated.");
    }

    // -------------------------------------------------------------------------
    // Public navigation API
    // -------------------------------------------------------------------------

    /**
     * Navigates to the start / configuration screen ({@code start-view.fxml}).
     *
     * <p>Used by:
     * <ul>
     *   <li>{@link GameController#onNewGame()} — player wants a fresh game.</li>
     *   <li>{@link VictoryController#onStart()} — player wants to change the
     *       number of machine opponents.</li>
     * </ul>
     * </p>
     *
     * @param anyNode any node currently attached to the scene whose
     *                {@link Stage} will host the new scene; must not be
     *                {@code null} and must already be part of an active scene
     * @throws RuntimeException if {@code start-view.fxml} cannot be loaded
     */
    public static void toStartScreen(Node anyNode) {
        loadAndApply(anyNode, FXML_START, "Cincuentazo \u2013 Inicio", null);
    }

    /**
     * Navigates to the main game screen ({@code game-view.fxml}) and
     * immediately starts a game with the given number of machine players.
     *
     * <p>Calls {@link GameController#startGame(int)} on the newly created
     * controller before making the scene visible, so the game is fully
     * initialised the moment the player sees the board.</p>
     *
     * <p>Used by:
     * <ul>
     *   <li>{@link StartController#onStart()} — first game of a session.</li>
     *   <li>{@link VictoryController#onPlayAgain()} — rematch with same config.</li>
     * </ul>
     * </p>
     *
     * @param anyNode     any node currently attached to the scene whose
     *                    {@link Stage} will host the new scene
     * @param numMachines number of machine opponents to include (1–3)
     * @throws RuntimeException if {@code game-view.fxml} cannot be loaded
     */
    public static void toGameScreen(Node anyNode, int numMachines) {
        loadAndApply(anyNode, FXML_GAME, "Cincuentazo", loader -> {
            GameController gc = loader.getController();
            gc.startGame(numMachines);
        });
    }

    /**
     * Navigates to the victory / results screen ({@code victory-view.fxml})
     * and injects the game result into the controller.
     *
     * <p>Calls {@link VictoryController#setResult(boolean, String, int)} on
     * the newly created controller before the scene becomes visible.</p>
     *
     * <p>Used by {@link GameController#navigateToVictoryScreen()} when the
     * game detects that only one player remains (HU-6).</p>
     *
     * @param anyNode        any node currently attached to the scene whose
     *                       {@link Stage} will host the new scene
     * @param humanWon       {@code true} if the human player is the sole survivor
     * @param winnerName     display name of the winning player
     * @param numberOfMachines number of machine opponents in the finished game
     * @throws RuntimeException if {@code victory-view.fxml} cannot be loaded
     */
    public static void toVictoryScreen(Node anyNode,
                                        boolean humanWon,
                                        String winnerName,
                                        int numberOfMachines) {
        loadAndApply(anyNode, FXML_VICTORY, "Cincuentazo \u2013 Resultado", loader -> {
            VictoryController vc = loader.getController();
            vc.setResult(humanWon, winnerName, numberOfMachines);
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Functional interface for the optional post-load configuration step
     * (e.g., passing data into the freshly created controller).
     */
    @FunctionalInterface
    private interface LoaderConsumer {
        /**
         * Configures the controller obtained from {@code loader}.
         *
         * @param loader the {@link FXMLLoader} after {@code load()} has been called
         */
        void configure(FXMLLoader loader);
    }

    /**
     * Core helper: loads an FXML file, optionally configures its controller,
     * wraps the root in a {@link Scene}, and applies it to the {@link Stage}
     * obtained from {@code anyNode}.
     *
     * @param anyNode   the source node whose stage will show the new scene
     * @param fxmlPath  classpath-absolute path to the FXML file
     * @param title     window title to set on the stage
     * @param configure optional post-load step; {@code null} if not needed
     * @throws RuntimeException if the FXML cannot be loaded
     */
    private static void loadAndApply(Node anyNode,
                                      String fxmlPath,
                                      String title,
                                      LoaderConsumer configure) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneNavigator.class.getResource(fxmlPath)
            );
            Scene scene = new Scene(loader.load());

            if (configure != null) {
                configure.configure(loader);
            }

            Stage stage = (Stage) anyNode.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);

        } catch (IOException e) {
            throw new RuntimeException(
                    "SceneNavigator: no se pudo cargar " + fxmlPath, e
            );
        }
    }
}
