package np.cincuentazo.view;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public final class UiAnimations {

    private static final String ACTIVE_TRANSITION_KEY = "ui-active-transition";
    private static final Duration QUICK = Duration.millis(95);
    private static final Duration SMOOTH = Duration.millis(150);
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
