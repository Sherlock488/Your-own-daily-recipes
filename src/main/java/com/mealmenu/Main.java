package com.mealmenu;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public class Main extends Application {

    private static final double WINDOW_WIDTH = 350;
    private static final double WINDOW_HEIGHT = 720;
    private static final double MINIMUM_SPLASH_MILLIS = 1500;

    private Stage primaryStage;
    private StackPane screenSurface;
    private StackPane pageHost;
    private KitchenPage kitchenPage;
    private HomePage homePage;
    private ProfilePage profilePage;
    private NavigationBar navigationBar;
    private MealPlanStore mealPlanStore;
    private LocalAccountService accountService;
    private Node currentPage;
    private ParallelTransition pageTransition;
    private double dragOffsetX;
    private double dragOffsetY;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Daily Menu");
        stage.setResizable(false);

        BorderPane deviceFrame = new BorderPane();
        deviceFrame.getStyleClass().add("device-frame");
        deviceFrame.setPadding(new Insets(5));
        deviceFrame.setTop(createDragHandle());

        screenSurface = new StackPane();
        screenSurface.getStyleClass().add("screen-surface");
        Rectangle screenClip = new Rectangle();
        screenClip.setArcWidth(44);
        screenClip.setArcHeight(44);
        screenClip.widthProperty().bind(screenSurface.widthProperty());
        screenClip.heightProperty().bind(screenSurface.heightProperty());
        screenSurface.setClip(screenClip);
        deviceFrame.setCenter(screenSurface);

        Scene scene = new Scene(deviceFrame, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        URL stylesheet = Main.class.getResource("/com/mealmenu/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                Platform.exit();
                event.consume();
            }
        });

        LoadingPage loadingPage = new LoadingPage();
        screenSurface.getChildren().setAll(loadingPage);
        stage.setScene(scene);
        stage.show();
        beginStartup(loadingPage);
    }

    private StackPane createDragHandle() {
        StackPane handle = new StackPane();
        handle.getStyleClass().add("drag-handle");
        handle.setPrefHeight(12);
        handle.setPickOnBounds(true);
        Region speaker = new Region();
        speaker.getStyleClass().add("device-speaker");
        speaker.setPrefSize(38, 4);
        speaker.setMaxSize(38, 4);
        handle.getChildren().add(speaker);
        handle.setOnMousePressed(event -> {
            dragOffsetX = event.getScreenX() - primaryStage.getX();
            dragOffsetY = event.getScreenY() - primaryStage.getY();
        });
        handle.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - dragOffsetX);
            primaryStage.setY(event.getScreenY() - dragOffsetY);
        });
        return handle;
    }

    private void beginStartup(LoadingPage loadingPage) {
        CompletableFuture<Void> minimumDisplayTime = new CompletableFuture<>();
        PauseTransition minimumDelay =
                new PauseTransition(Duration.millis(MINIMUM_SPLASH_MILLIS));
        minimumDelay.setOnFinished(event -> minimumDisplayTime.complete(null));
        minimumDelay.play();

        CompletableFuture<LocalAccountService> initialization =
                CompletableFuture.supplyAsync(LocalAccountService::new);
        CompletableFuture.allOf(minimumDisplayTime, initialization)
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        loadingPage.setStatusText(
                                "Startup failed. Press Escape to close the app."
                        );
                        return;
                    }
                    accountService = initialization.join();
                    if (accountService.isLoggedIn()) {
                        showAuthenticatedApplication();
                    } else {
                        showAuthPage();
                    }
                }));
    }

    private void showAuthPage() {
        pageHost = null;
        currentPage = null;
        screenSurface.getChildren().setAll(new AuthPage(
                primaryStage,
                accountService,
                this::showAuthenticatedApplication
        ));
    }

    private void showAuthenticatedApplication() {
        String accountId = accountService.accountId();
        mealPlanStore = new MealPlanStore(accountId);
        AppUsageTracker usageTracker = new AppUsageTracker(accountId);
        pageHost = new StackPane();
        pageHost.getStyleClass().add("page-host");
        navigationBar = new NavigationBar(this::navigateTo);

        homePage = new HomePage(
                primaryStage,
                mealPlanStore,
                usageTracker,
                accountService,
                date -> {
                    kitchenPage.setTargetDate(date);
                    navigateTo("Kitchen");
                },
                selection -> startQuickStart(selection.tag(), selection.mealPeriod())
        );
        kitchenPage = new KitchenPage(
                primaryStage,
                accountService,
                (date, period, recipe) -> {
                    boolean saved = mealPlanStore.add(date, period, recipe.id());
                    homePage.refresh();
                    return saved;
                }
        );
        profilePage = new ProfilePage(
                primaryStage,
                accountService,
                this::logout
        );

        BorderPane applicationLayout = new BorderPane();
        applicationLayout.getStyleClass().add("application-layout");
        applicationLayout.setCenter(pageHost);
        applicationLayout.setBottom(navigationBar);
        screenSurface.getChildren().setAll(applicationLayout);
        navigateTo("Home");
    }

    private void startQuickStart(String tag, String mealPeriod) {
        kitchenPage.prepareQuickStart(tag, LocalDate.now(), mealPeriod);
        navigateTo("Kitchen");
        PauseTransition showSuggestion = new PauseTransition(Duration.millis(240));
        showSuggestion.setOnFinished(event -> kitchenPage.showRecommendation());
        showSuggestion.play();
    }

    private void logout() {
        accountService.logout();
        if (homePage != null) {
            homePage.stopClock();
        }
        kitchenPage = null;
        homePage = null;
        profilePage = null;
        navigationBar = null;
        mealPlanStore = null;
        showAuthPage();
    }

    private void navigateTo(String pageName) {
        if (pageHost == null || homePage == null) {
            return;
        }
        Node nextPage = switch (pageName) {
            case "Kitchen" -> kitchenPage;
            case "Profile" -> profilePage;
            default -> homePage;
        };
        if (navigationBar != null) {
            navigationBar.setActivePage(pageName);
        }
        if (nextPage == kitchenPage) {
            kitchenPage.refreshPreferences();
        }
        if (nextPage == currentPage) {
            if (nextPage == homePage) {
                homePage.refresh();
            }
            return;
        }
        if (pageTransition != null) {
            pageTransition.stop();
        }
        pageHost.getChildren().setAll(nextPage);
        currentPage = nextPage;
        if (nextPage == homePage) {
            homePage.refresh();
        }
        nextPage.setOpacity(0);
        nextPage.setTranslateY(7);
        FadeTransition fade = new FadeTransition(Duration.millis(180), nextPage);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(
                Duration.millis(180), nextPage
        );
        slide.setFromY(7);
        slide.setToY(0);
        pageTransition = new ParallelTransition(fade, slide);
        pageTransition.setOnFinished(event -> {
            nextPage.setOpacity(1);
            nextPage.setTranslateY(0);
        });
        pageTransition.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
