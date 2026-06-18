package np.cincuentazo.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.util.Duration;

public final class UiAnimations {

    private static final String ACTIVE_TRANSITION_KEY = "ui-active-transition";
    private static final Duration QUICK = Duration.millis(95);
    private static final Duration SMOOTH = Duration.millis(150);
    private static final Duration DEAL = Duration.millis(420);
    private static final Interpolator EASE = Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0);

    private UiAnimations() {
    }

    public static void applyButtonMotion(Node node) {
        node.setOnMouseEntered(event -> animate(node, -1.5, 1.02, SMOOTH));
        node.setOnMouseExited(event -> animate(node, 0, 1.0, SMOOTH));
        node.setOnMousePressed(event -> animate(node, 1.5, 0.97, QUICK));
        node.setOnMouseReleased(event -> {
            boolean hovered = node.isHover();
            animate(node, hovered ? -1.5 : 0, hovered ? 1.02 : 1.0, SMOOTH);
        });
    }

    public static void applyCardMotion(Node node) {
        node.setOnMouseEntered(event -> {
            if (!isDisabledCard(node)) animate(node, -6, 1.03, SMOOTH);
        });
        node.setOnMouseExited(event -> animate(node, 0, 1.0, SMOOTH));
        node.setOnMousePressed(event -> {
            if (!isDisabledCard(node)) animate(node, -3, 0.98, QUICK);
        });
        node.setOnMouseReleased(event -> {
            if (!isDisabledCard(node)) animate(node, node.isHover() ? -6 : 0, node.isHover() ? 1.03 : 1.0, SMOOTH);
        });
    }

    public static void applyDeckMotion(Node node) {
        node.setOnMouseEntered(event -> animate(node, -2, 1.02, SMOOTH));
        node.setOnMouseExited(event -> animate(node, 0, 1.0, SMOOTH));
        node.setOnMousePressed(event -> animate(node, 1, 0.98, QUICK));
        node.setOnMouseReleased(event -> animate(node, node.isHover() ? -2 : 0, node.isHover() ? 1.02 : 1.0, SMOOTH));
    }

    public static void animateCardFromDeck(Node deck, Node card, Runnable onFinished) {
        Bounds deckBounds = deck.localToScene(deck.getBoundsInLocal());
        Bounds cardBounds = card.localToScene(card.getBoundsInLocal());

        double deckCenterX = deckBounds.getMinX() + deckBounds.getWidth() / 2;
        double deckCenterY = deckBounds.getMinY() + deckBounds.getHeight() / 2;
        double cardCenterX = cardBounds.getMinX() + cardBounds.getWidth() / 2;
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

    private static boolean isDisabledCard(Node node) {
        return node.getStyleClass().contains("card-disabled");
    }

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
