package com.mealmenu;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

public class LoadingPage extends VBox {

    private static final Color PALE_GREEN = Color.web("#A9CDB0");
    private static final Color LIGHT_GREEN = Color.web("#DCEBDD");
    private final Label statusLabel;

    public LoadingPage() {
        getStyleClass().add("loading-page");
        setAlignment(Pos.CENTER);
        setFillWidth(true);

        StackPane artwork = new StackPane();
        artwork.getStyleClass().add("loading-artwork");
        artwork.setMinHeight(260);
        artwork.setPrefHeight(340);

        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(artwork.widthProperty());
        canvas.heightProperty().bind(artwork.heightProperty());
        artwork.getChildren().add(canvas);

        canvas.widthProperty().addListener(
                (observable, oldValue, newValue) -> drawUtensils(canvas)
        );
        canvas.heightProperty().addListener(
                (observable, oldValue, newValue) -> drawUtensils(canvas)
        );
        drawUtensils(canvas);

        Label title = new Label("dailyrecipes");
        title.getStyleClass().add("loading-title");

        statusLabel = new Label("Preparing your kitchen...");
        statusLabel.getStyleClass().add("loading-status");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(34, 34);
        progress.getStyleClass().add("loading-progress");

        VBox content = new VBox(14, artwork, title, statusLabel, progress);
        content.getStyleClass().add("loading-content");
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(330);
        VBox.setVgrow(artwork, Priority.ALWAYS);
        getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    private void drawUtensils(Canvas canvas) {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);

        double scale = Math.min(width / 320.0, height / 300.0);
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double plateSize = 142 * scale;

        graphics.setFill(Color.web("#F4F9F4"));
        graphics.fillOval(
                centerX - plateSize / 2,
                centerY - plateSize / 2,
                plateSize,
                plateSize
        );

        graphics.setStroke(PALE_GREEN);
        graphics.setLineWidth(8 * scale);
        graphics.strokeOval(
                centerX - plateSize / 2,
                centerY - plateSize / 2,
                plateSize,
                plateSize
        );

        graphics.setStroke(LIGHT_GREEN);
        graphics.setLineWidth(3 * scale);
        graphics.strokeOval(
                centerX - plateSize * 0.39,
                centerY - plateSize * 0.39,
                plateSize * 0.78,
                plateSize * 0.78
        );

        graphics.setStroke(PALE_GREEN);
        graphics.setFill(PALE_GREEN);
        graphics.setLineCap(StrokeLineCap.ROUND);
        graphics.setLineWidth(5 * scale);

        double forkX = centerX - 108 * scale;
        graphics.strokeLine(
                forkX, centerY - 28 * scale,
                forkX, centerY + 59 * scale
        );
        graphics.setLineWidth(3.5 * scale);
        for (int tine = -2; tine <= 2; tine++) {
            double tineX = forkX + tine * 5 * scale;
            graphics.strokeLine(
                    tineX, centerY - 58 * scale,
                    tineX, centerY - 30 * scale
            );
        }
        graphics.setLineWidth(4 * scale);
        graphics.strokeLine(
                forkX - 10 * scale, centerY - 29 * scale,
                forkX + 10 * scale, centerY - 29 * scale
        );

        double knifeX = centerX + 108 * scale;
        graphics.beginPath();
        graphics.moveTo(knifeX, centerY - 59 * scale);
        graphics.bezierCurveTo(
                knifeX + 21 * scale, centerY - 41 * scale,
                knifeX + 19 * scale, centerY - 20 * scale,
                knifeX, centerY - 9 * scale
        );
        graphics.closePath();
        graphics.fill();
        graphics.setLineWidth(5 * scale);
        graphics.strokeLine(
                knifeX, centerY - 10 * scale,
                knifeX, centerY + 59 * scale
        );
    }
}
