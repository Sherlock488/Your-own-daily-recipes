package com.mealmenu;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Main extends Application {

    private static final double WINDOW_WIDTH = 350;
    private static final double WINDOW_HEIGHT = 720;
    private static final double MINIMUM_SPLASH_MILLIS = 1500;

    private Stage primaryStage;
    private StackPane screenSurface;
    private StackPane pageHost;
    private KitchenPage kitchenPage;
    private EatPage eatPage;
    private Node framedHome;
    private Node framedKitchen;
    private Node framedProfile;
    private HomePage homePage;
    private ProfilePage profilePage;
    private NavigationBar navigationBar;
    private MealPlanStore mealPlanStore;
    private LocalAccountService accountService;
    private SwipeNavigator swipeNavigator;
    private double dragOffsetX;
    private double dragOffsetY;

    private static final double DESKTOP_WIDTH = 1240;
    private static final double DESKTOP_HEIGHT = 820;
    private static final double PAGE_MAX_WIDTH = 760;

    private WindowMode windowMode;

    @Override
    public void start(Stage stage) {
        Links.init(getHostServices());
        windowMode = WindowMode.resolve(getParameters().getRaw());
        openWindow(stage, false);
    }

    /** Builds the window for the current mode; {@code keepSession} skips the splash after a mode switch. */
    private void openWindow(Stage stage, boolean keepSession) {
        primaryStage = stage;
        stage.setTitle("Daily Menu");
        screenSurface = new StackPane();
        screenSurface.getStyleClass().add("screen-surface");
        Scene scene;
        if (windowMode == WindowMode.PHONE) {
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);
            BorderPane deviceFrame = new BorderPane();
            deviceFrame.getStyleClass().add("device-frame");
            deviceFrame.setPadding(new Insets(5));
            deviceFrame.setTop(createDragHandle());
            Rectangle screenClip = new Rectangle();
            screenClip.setArcWidth(44);
            screenClip.setArcHeight(44);
            screenClip.widthProperty().bind(screenSurface.widthProperty());
            screenClip.heightProperty().bind(screenSurface.heightProperty());
            screenSurface.setClip(screenClip);
            deviceFrame.setCenter(screenSurface);
            scene = new Scene(deviceFrame, WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.setFill(Color.TRANSPARENT);
            // The frameless phone window has no close button, so Escape quits.
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE
                        && !(scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl)) {
                    Platform.exit();
                    event.consume();
                }
            });
        } else {
            stage.setResizable(true);
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            scene = new Scene(screenSurface, DESKTOP_WIDTH, DESKTOP_HEIGHT);
            scene.getRoot().getStyleClass().add("desktop");
        }
        URL stylesheet = Main.class.getResource("/com/mealmenu/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        stage.setScene(scene);
        stage.show();
        if (keepSession && accountService != null && accountService.accountId() != null) {
            showAuthenticatedApplication();
            navigateTo("Profile");
            return;
        }
        LoadingPage loadingPage = new LoadingPage();
        screenSurface.getChildren().setAll(loadingPage);
        beginStartup(loadingPage);
    }

    /** Re-opens the app in the other window style without logging out. */
    private void switchWindowMode(WindowMode mode) {
        if (mode == windowMode) {
            return;
        }
        windowMode = mode;
        mode.save();
        if (homePage != null) {
            homePage.stopClock();
        }
        Stage old = primaryStage;
        currentPage = null;
        openWindow(new Stage(), true);
        old.close();
    }

    private boolean desktop() {
        return windowMode == WindowMode.DESKTOP;
    }

    /** In desktop mode, single-column pages are centered with a readable width. */
    private Node framed(Node page) {
        if (!desktop()) {
            return page;
        }
        StackPane holder = new StackPane(page);
        holder.getStyleClass().add("page-host");
        if (page instanceof Region region) {
            region.setMaxWidth(PAGE_MAX_WIDTH);
        }
        return holder;
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
                CompletableFuture.supplyAsync(() -> {
                    DiningService.get();
                    RestaurantCatalog.all();
                    return new LocalAccountService();
                });
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
        if (swipeNavigator != null) {
            swipeNavigator.dispose();
            swipeNavigator = null;
        }
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
        navigationBar = new NavigationBar(page -> {
            // A date picked from Schedule shouldn't stick when Kitchen is opened from the nav bar.
            if ("Kitchen".equals(page) && kitchenPage != null) {
                kitchenPage.setTargetDate(LocalDate.now());
            }
            navigateTo(page);
        }, desktop());

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
        eatPage = new EatPage(
                primaryStage,
                desktop(),
                accountId,
                (date, period, planId) -> {
                    boolean saved = mealPlanStore.add(date, period, planId);
                    homePage.refresh();
                    return saved;
                }
        );
        profilePage = new ProfilePage(
                primaryStage,
                accountService,
                this::logout,
                windowMode,
                this::switchWindowMode
        );
        homePage.setOnOpenHall(slug -> {
            navigateTo("Eat");
            eatPage.showHall(slug);
        });
        framedHome = framed(homePage);
        framedKitchen = framed(kitchenPage);
        framedProfile = framed(profilePage);

        BorderPane applicationLayout = new BorderPane();
        applicationLayout.getStyleClass().add("application-layout");
        applicationLayout.setCenter(pageHost);
        if (desktop()) {
            applicationLayout.setLeft(navigationBar);
        } else {
            applicationLayout.setBottom(navigationBar);
        }
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
        eatPage = null;
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
            case "Eat" -> eatPage;
            case "Kitchen" -> framedKitchen;
            case "Profile" -> framedProfile;
            default -> framedHome;
        };
        if (navigationBar != null) {
            navigationBar.setActivePage(pageName);
        }
        if (nextPage == eatPage) {
            eatPage.onShow();
        }
        if (nextPage == framedKitchen) {
            kitchenPage.refreshPreferences();
        }
        if (nextPage == currentPage) {
            if (nextPage == framedHome) {
                homePage.refresh();
            }
            return;
        }
        if (pageTransition != null) {
            pageTransition.stop();
        }
        pageHost.getChildren().setAll(nextPage);
        currentPage = nextPage;
        if (nextPage == framedHome) {
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
