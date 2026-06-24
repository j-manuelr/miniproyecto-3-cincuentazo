package np.cincuentazo.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Provides reusable micro-animation presets for JavaFX nodes in the
 * Cincuentazo UI.
 *
 * <p>All methods are {@code static} and the class is {@code final} with a
 * private constructor — it is a pure utility class with no instance state.
 * Every animation is applied directly to the target {@link Node}'s
 * {@link Node#translateYProperty()}, {@link Node#scaleXProperty()}/
 * {@link Node#scaleYProperty()}, or {@link Node#opacityProperty()}, so
 * callers need no knowledge of JavaFX animation internals.</p>
 *
 * <h2>Active-transition tracking</h2>
 * <p>Each animated node stores its currently running {@link ParallelTransition}
 * in its {@link Node#getProperties() properties map} under the key
 * {@link #ACTIVE_TRANSITION_KEY}. Before starting a new animation, the
 * previous one is stopped so transitions never overlap and the node's
 * transform state stays consistent.</p>
 *
 * <h2>SRP note</h2>
 * <p>Keeping animation logic in this class means controllers and views
 * never contain {@code Timeline}/{@code Transition} setup code — they
 * call a single method and this class handles everything else.</p>
 */
public final class UiAnimations {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * Key used to store the currently running {@link ParallelTransition} in
     * a node's properties map, so it can be stopped before a new animation
     * starts on the same node.
     */
    private static final String ACTIVE_TRANSITION_KEY = "ui-active-transition";

    /** Duration for fast, snappy interactions (press/release feedback). */
    private static final Duration QUICK  = Duration.millis(95);

    /** Duration for smooth hover enter/exit transitions. */
    private static final Duration SMOOTH = Duration.millis(150);

    /** Duration for the card-deal "fly from deck" animation. */
    private static final Duration DEAL   = Duration.millis(420);

    /**
     * Custom cubic-Bezier easing that starts fast and decelerates smoothly —
     * gives animations a physical, springy feel without an explicit spring solver.
     */
    private static final Interpolator EASE = Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0);

    // -------------------------------------------------------------------------
    // Private constructor — utility class, not instantiable
    // -------------------------------------------------------------------------

    private UiAnimations() {
        throw new AssertionError("UiAnimations is a utility class and must not be instantiated.");
    }

    // -------------------------------------------------------------------------
    // Public animation presets
    // -------------------------------------------------------------------------

    /**
     * Applies hover, press, and release micro-animations to a button-like node.
     *
     * <p>Behaviour:
     * <ul>
     *   <li><b>Mouse enter:</b> node lifts slightly (translateY −1.5 px) and
     *       scales up to 102 %.</li>
     *   <li><b>Mouse exit:</b> node returns to its rest position.</li>
     *   <li><b>Mouse pressed:</b> node sinks (translateY +1.5 px) and scales
     *       down to 97 %, providing tactile press feedback.</li>
     *   <li><b>Mouse released:</b> node returns to the hover state if the
     *       cursor is still over it, or to rest otherwise.</li>
     * </ul>
     * </p>
     *
     * @param node the button or button-like node to animate; existing
     *             {@code onMouseEntered/Exited/Pressed/Released} handlers
     *             are replaced
     */
    public static void applyButtonMotion(Node node) {
        node.setOnMouseEntered(event -> animate(node, -1.5, 1.02, SMOOTH));
        node.setOnMouseExited(event -> animate(node, 0, 1.0, SMOOTH));
        node.setOnMousePressed(event -> animate(node, 1.5, 0.97, QUICK));
        node.setOnMouseReleased(event -> {
            boolean hovered = node.isHover();
            animate(node, hovered ? -1.5 : 0, hovered ? 1.02 : 1.0, SMOOTH);
        });
    }

    /**
     * Applies hover and press micro-animations to a playable card view.
     *
     * <p>Identical in structure to {@link #applyButtonMotion(Node)} but with
     * exaggerated vertical lift (−6 px on hover) to suggest that the card is
     * "pickable". Animations are suppressed for cards that carry the CSS class
     * {@code card-disabled} — those cards cannot be played and must not
     * respond visually to hover.</p>
     *
     * @param node the {@link CardView} node to animate
     */
    public static void applyCardMotion(Node node) {
        node.setOnMouseEntered(event -> {
            if (!isDisabledCard(node)) animate(node, -6, 1.03, SMOOTH);
        });
        node.setOnMouseExited(event -> animate(node, 0, 1.0, SMOOTH));
        node.setOnMousePressed(event -> {
            if (!isDisabledCard(node)) animate(node, -3, 0.98, QUICK);
        });
        node.setOnMouseReleased(event -> {
            if (!isDisabledCard(node))
                animate(node, node.isHover() ? -6 : 0, node.isHover() ? 1.03 : 1.0, SMOOTH);
        });
    }

    /**
     * Applies hover and press micro-animations to the deck pile graphic.
     *
     * <p>Similar to {@link #applyButtonMotion(Node)} but with subtler
     * values (−2 px lift, 102 % scale) appropriate for a larger graphic
     * element rather than a text button.</p>
     *
     * @param node the deck pile node to animate (typically a {@link javafx.scene.layout.StackPane})
     */
    public static void applyDeckMotion(Node node) {
        node.setOnMouseEntered(event -> animate(node, -2, 1.02, SMOOTH));
        node.setOnMouseExited(event -> animate(node, 0, 1.0, SMOOTH));
        node.setOnMousePressed(event -> animate(node, 1, 0.98, QUICK));
        node.setOnMouseReleased(event ->
            animate(node, node.isHover() ? -2 : 0, node.isHover() ? 1.02 : 1.0, SMOOTH)
        );
    }

    /**
     * Animates a card flying from the deck pile to its final position in
     * a player's hand, then invokes a completion callback.
     *
     * <p>The card's position is initially overridden so it appears to sit on
     * top of the deck (using scene-coordinate conversion), then a
     * {@link ParallelTransition} moves, scales, and fades it back to its
     * natural layout position over {@link #DEAL} milliseconds.</p>
     *
     * <p>During the animation the card is set
     * {@link Node#setMouseTransparent(boolean) mouse-transparent} to avoid
     * accidental clicks, and the flag is cleared when the animation finishes.</p>
     *
     * @param deck       the deck pile node that acts as the animation's origin;
     *                   its scene bounds are used to compute the start position
     * @param card       the card node that will fly into the hand; must already
     *                   be part of the scene graph (added to {@code humanHand})
     *                   before this method is called
     * @param onFinished callback invoked on the JavaFX Application Thread when
     *                   the animation completes; may be {@code null}
     */
    public static void animateCardFromDeck(Node deck, Node card, Runnable onFinished) {
        Bounds deckBounds = deck.localToScene(deck.getBoundsInLocal());
        Bounds cardBounds = card.localToScene(card.getBoundsInLocal());

        double deckCenterX = deckBounds.getMinX() + deckBounds.getWidth()  / 2;
        double deckCenterY = deckBounds.getMinY() + deckBounds.getHeight() / 2;
        double cardCenterX = cardBounds.getMinX() + cardBounds.getWidth()  / 2;
        double cardCenterY = cardBounds.getMinY() + cardBounds.getHeight() / 2;

        card.setMouseTransparent(true);
        card.setTranslateX(deckCenterX - cardCenterX);
        card.setTranslateY(deckCenterY - cardCenterY);
        card.setScaleX(0.86);
        card.setScaleY(0.86);
        card.setOpacity(0.92);

        TranslateTransition move = new TranslateTransition(DEAL, card);
        move.setToX(0);
        move.setToY(0);
        move.setInterpolator(EASE);

        ScaleTransition resize = new ScaleTransition(DEAL, card);
        resize.setToX(1);
        resize.setToY(1);
        resize.setInterpolator(EASE);

        FadeTransition fade = new FadeTransition(DEAL, card);
        fade.setToValue(1);
        fade.setInterpolator(EASE);

        ParallelTransition transition = new ParallelTransition(move, resize, fade);
        transition.setOnFinished(event -> {
            card.setMouseTransparent(false);
            if (onFinished != null) onFinished.run();
        });
        transition.play();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code node} carries the {@code card-disabled}
     * CSS class, indicating it represents a card that cannot be played.
     * Used to suppress hover/press feedback on unplayable cards.
     *
     * @param node the node to inspect
     * @return {@code true} if the node is a disabled card
     */
    private static boolean isDisabledCard(Node node) {
        return node.getStyleClass().contains("card-disabled");
    }

    /**
     * Builds and plays a combined translate-Y + uniform-scale animation on
     * {@code node}, cancelling any previously running animation first.
     *
     * <p>The running animation is stored in the node's properties map under
     * {@link #ACTIVE_TRANSITION_KEY} so it can be interrupted when a new
     * interaction fires before the current animation finishes.</p>
     *
     * @param node       the node to animate
     * @param translateY the target Y translation in pixels (negative = up)
     * @param scale      the target uniform scale (1.0 = natural size)
     * @param duration   how long the transition should take
     */
    private static void animate(Node node, double translateY, double scale, Duration duration) {
        Object active = node.getProperties().get(ACTIVE_TRANSITION_KEY);
        if (active instanceof ParallelTransition transition) {
            transition.stop();
        }

        TranslateTransition move = new TranslateTransition(duration, node);
        move.setToY(translateY);
        move.setInterpolator(EASE);

        ScaleTransition resize = new ScaleTransition(duration, node);
        resize.setToX(scale);
        resize.setToY(scale);
        resize.setInterpolator(EASE);

        ParallelTransition transition = new ParallelTransition(move, resize);
        node.getProperties().put(ACTIVE_TRANSITION_KEY, transition);
        transition.setOnFinished(event -> node.getProperties().remove(ACTIVE_TRANSITION_KEY));
        transition.play();
    }
}
