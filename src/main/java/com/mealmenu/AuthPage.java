package com.mealmenu;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;

public final class AuthPage extends ScrollPane {

    private final LocalAccountService accountService;
    private final Runnable onAuthenticated;
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox keepSignedIn = new CheckBox("Keep me signed in on this device");
    private final Label status = new Label();
    private final Button loginTab = new Button("Log In");
    private final Button registerTab = new Button("Register");
    private boolean registering;

    public AuthPage(Stage owner, LocalAccountService accountService, Runnable onAuthenticated) {
        this.accountService = accountService;
        this.onAuthenticated = onAuthenticated;
        getStyleClass().add("page-scroll");
        setFitToWidth(true);
        setPannable(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        VBox page = new VBox(16);
        page.getStyleClass().add("auth-page");
        page.setPadding(new Insets(34, 22, 28, 22));
        page.setAlignment(Pos.CENTER);

        StackPane mark = new StackPane(new Label("DM"));
        mark.getStyleClass().add("brand-mark");
        mark.setMinSize(64, 64);
        mark.setPrefSize(64, 64);
        mark.setMaxSize(64, 64);
        Label title = new Label("Daily Menu");
        title.getStyleClass().add("auth-title");
        Label subtitle = new Label("A calmer way to plan what you eat.");
        subtitle.getStyleClass().add("page-subtitle");

        HBox tabs = new HBox(6, loginTab, registerTab);
        tabs.getStyleClass().add("auth-tabs");
        tabs.setAlignment(Pos.CENTER);
        loginTab.setMaxWidth(Double.MAX_VALUE);
        registerTab.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginTab, Priority.ALWAYS);
        HBox.setHgrow(registerTab, Priority.ALWAYS);
        loginTab.getStyleClass().add("auth-mode-button");
        registerTab.getStyleClass().add("auth-mode-button");
        loginTab.setOnAction(event -> setRegistering(false));
        registerTab.setOnAction(event -> setRegistering(true));

        usernameField.setPromptText("Username");
        passwordField.setPromptText("Password (at least 8 characters)");
        passwordField.setOnAction(event -> submit());
        keepSignedIn.getStyleClass().add("check-option");
        status.getStyleClass().add("status-text");
        status.setWrapText(true);
        status.setMinHeight(34);

        Button submitButton = new Button("Continue");
        submitButton.getStyleClass().add("primary-button");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setOnAction(event -> submit());

        Label localNote = new Label(
                "Your account and meal plans are stored locally on this computer. "
                        + "Your password is saved as a secure hash, never as plain text."
        );
        localNote.getStyleClass().add("muted-text");
        localNote.setWrapText(true);

        VBox form = new VBox(12, tabs, usernameField, passwordField,
                keepSignedIn, submitButton, status, localNote);
        form.getStyleClass().add("auth-card");
        page.getChildren().addAll(mark, title, subtitle, form);
        setContent(page);
        setRegistering(false);
    }

    private void setRegistering(boolean value) {
        registering = value;
        loginTab.getStyleClass().remove("auth-mode-active");
        registerTab.getStyleClass().remove("auth-mode-active");
        (registering ? registerTab : loginTab).getStyleClass().add("auth-mode-active");
        passwordField.setPromptText(registering
                ? "Create a password (at least 6 characters)"
                : "Password");
        status.setText(registering
                ? "Create a local account using a username and password."
                : "Welcome back. Log in to open your meal plans.");
    }

    private void submit() {
        char[] password = passwordField.getText().toCharArray();
        String error;
        try {
            error = registering
                    ? accountService.register(usernameField.getText(), password,
                    keepSignedIn.isSelected())
                    : accountService.login(usernameField.getText(), password,
                    keepSignedIn.isSelected());
        } finally {
            Arrays.fill(password, '\0');
        }
        if (error != null) {
            status.setText(error);
            return;
        }
        passwordField.clear();
        onAuthenticated.run();
    }
}
