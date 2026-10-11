package com.mealmenu;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Keeps the existing pages and bottom navigation while adding horizontal dragging. */
public final class SwipeNavigator {

    private final StackPane host;
    private final List<String> order;
    private final Map<String, Node> pages;
    private final Consumer<String> preparePage;
    private final Consumer<String> selectPage;
    private int currentIndex = -1;
    private int previewIndex = -1;
    private int animationDestination = -1;
    private Timeline animation;
    private boolean tracking;
    private boolean horizontal;
    private boolean vertical;
    private boolean suppressClick;
    private double pressedX;
    private double pressedY;
    private ButtonBase pressedButton;

    public SwipeNavigator(StackPane host, List<String> order, Map<String, Node> pages,
                          Consumer<String> preparePage, Consumer<String> selectPage) {
        this.host = host;
        this.order = List.copyOf(order);
        this.pages = Map.copyOf(pages);
        this.preparePage = preparePage;
        this.selectPage = selectPage;
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(host.widthProperty());
        clip.heightProperty().bind(host.heightProperty());
        host.setClip(clip);
        host.addEventFilter(MouseEvent.MOUSE_PRESSED, this::pressed);
        host.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::dragged);
        host.addEventFilter(MouseEvent.MOUSE_RELEASED, this::released);
        host.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (suppressClick || animation != null) {
                event.consume();
                suppressClick = false;
            }
        });
    }

    public void show(String pageName) {
        int next = order.indexOf(pageName);
        if (next < 0) {
            return;
        }
        if (animation != null) {
            animation.stop();
            settle(animationDestination);
        }
        cancelDrag();
        preparePage.accept(pageName);
        if (currentIndex < 0 || next == currentIndex) {
            settle(next);
            return;
        }
        int direction = next > currentIndex ? 1 : -1;
        previewIndex = next;
        Node incoming = page(next);
        incoming.setTranslateX(direction * width());
        host.getChildren().setAll(page(currentIndex), incoming);
        animate(next, -direction * width(), 0);
    }

    public void cancelDrag() {
        if (!tracking) {
            return;
        }
        if (pressedButton != null) {
            pressedButton.disarm();
        }
        tracking = false;
        horizontal = false;
        vertical = false;
        if (currentIndex >= 0) {
            settle(currentIndex);
        }
    }

    public void dispose() {
        if (animation != null) {
            animation.stop();
            animation = null;
        }
        tracking = false;
        pages.values().forEach(node -> node.setTranslateX(0));
    }

    private void pressed(MouseEvent event) {
        suppressClick = false;
        if (animation != null) {
            event.consume();
            return;
        }
        if (event.getButton() != MouseButton.PRIMARY || currentIndex < 0
                || reservesHorizontalDrag(event.getTarget())) {
            return;
        }
        tracking = true;
        horizontal = false;
        vertical = false;
        pressedX = event.getSceneX();
        pressedY = event.getSceneY();
        pressedButton = findButton(event.getTarget());
    }

    private void dragged(MouseEvent event) {
        if (!tracking || vertical || !event.isPrimaryButtonDown()) {
            return;
        }
        double dx = event.getSceneX() - pressedX;
        double dy = event.getSceneY() - pressedY;
        if (!horizontal) {
            if (Math.abs(dy) >= 10 && Math.abs(dy) > Math.abs(dx)) {
                vertical = true;
                return;
            }
            if (Math.abs(dx) < 12 || Math.abs(dx) < Math.abs(dy) * 1.35) {
                return;
            }
            horizontal = true;
            suppressClick = true;
            if (pressedButton != null) {
                pressedButton.disarm();
            }
        }

        int next = currentIndex + (dx < 0 ? 1 : -1);
        if (next < 0 || next >= order.size()) {
            clearPreview();
            page(currentIndex).setTranslateX(Math.max(-28, Math.min(28, dx * 0.18)));
        } else {
            if (previewIndex != next) {
                clearPreview();
                previewIndex = next;
                preparePage.accept(order.get(next));
                host.getChildren().setAll(page(currentIndex), page(next));
            }
            double offset = Math.max(-width(), Math.min(width(), dx));
            page(currentIndex).setTranslateX(offset);
            page(next).setTranslateX(offset + (dx < 0 ? width() : -width()));
        }
        event.consume();
    }

    private void released(MouseEvent event) {
        if (!tracking) {
            return;
        }
        tracking = false;
        if (!horizontal) {
            return;
        }
        horizontal = false;
        if (pressedButton != null) {
            pressedButton.disarm();
        }
        double offset = page(currentIndex).getTranslateX();
        boolean commit = previewIndex >= 0 && Math.abs(offset) >= Math.max(55, width() * 0.22);
        if (commit) {
            animate(previewIndex, offset < 0 ? -width() : width(), 0);
        } else {
            double previewEnd = previewIndex >= 0 && previewIndex > currentIndex ? width() : -width();
            animate(currentIndex, 0, previewEnd);
        }
        event.consume();
    }

    private void animate(int destination, double outgoingEnd, double incomingEnd) {
        animationDestination = destination;
        Node outgoing = page(currentIndex);
        KeyValue out = new KeyValue(outgoing.translateXProperty(), outgoingEnd,
                Interpolator.EASE_BOTH);
        if (previewIndex >= 0) {
            KeyValue in = new KeyValue(page(previewIndex).translateXProperty(), incomingEnd,
                    Interpolator.EASE_BOTH);
            animation = new Timeline(new KeyFrame(Duration.millis(220), out, in));
        } else {
            animation = new Timeline(new KeyFrame(Duration.millis(170), out));
        }
        animation.setOnFinished(event -> settle(destination));
        animation.play();
    }

    private void settle(int index) {
        animation = null;
        currentIndex = index;
        pages.values().forEach(node -> {
            node.setTranslateX(0);
            node.setTranslateY(0);
            node.setOpacity(1);
        });
        previewIndex = -1;
        host.getChildren().setAll(page(index));
        selectPage.accept(order.get(index));
    }

    private void clearPreview() {
        if (previewIndex >= 0) {
            page(previewIndex).setTranslateX(0);
            host.getChildren().setAll(page(currentIndex));
            previewIndex = -1;
        }
    }

    private Node page(int index) {
        return pages.get(order.get(index));
    }

    private double width() {
        return Math.max(1, host.getWidth());
    }

    private boolean reservesHorizontalDrag(Object target) {
        for (Node node = target instanceof Node ? (Node) target : null;
             node != null && node != host; node = node.getParent()) {
            if (node instanceof TextInputControl || node instanceof Slider
                    || node instanceof ScrollBar || node instanceof ComboBoxBase<?>) {
                return true;
            }
        }
        return false;
    }

    private ButtonBase findButton(Object target) {
        for (Node node = target instanceof Node ? (Node) target : null;
             node != null && node != host; node = node.getParent()) {
            if (node instanceof ButtonBase button) {
                return button;
            }
        }
        return null;
    }
}
