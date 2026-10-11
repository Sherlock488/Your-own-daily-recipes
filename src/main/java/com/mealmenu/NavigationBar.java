package com.mealmenu;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NavigationBar extends HBox {

    private final Consumer<String> pageHandler;
    private final Map<String, Button> buttons = new LinkedHashMap<>();

    public NavigationBar(Consumer<String> pageHandler) {
        this.pageHandler = pageHandler;
        getStyleClass().add("navigation-bar");
        setSpacing(6);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10, 12, 12, 12));

        addButton("Home");
        addButton("Eat");
        addButton("Kitchen");
        addButton("Profile");
        setActivePage("Home");
    }

    public void setActivePage(String pageName) {
        buttons.forEach((name, button) -> {
            button.getStyleClass().remove("nav-button-active");
            if (name.equals(pageName)) {
                button.getStyleClass().add("nav-button-active");
            }
        });
    }

    private void addButton(String pageName) {
        Button button = new Button(pageName);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setOnAction(event -> pageHandler.accept(pageName));
        buttons.put(pageName, button);
        getChildren().add(button);
    }
}
