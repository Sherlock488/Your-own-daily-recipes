package com.mealmenu;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

final class AppDialogs {

    private static final List<String> MEAL_PERIODS = List.of(
            "Breakfast", "Lunch", "Dinner", "Late-night snack"
    );

    private AppDialogs() {
    }

    static void showRecipeDetails(Stage owner, Recipe recipe) {
        VBox details = new VBox(12);
        details.setPadding(new Insets(14));
        details.getStyleClass().add("dialog-content");

        HBox meta = new HBox(8,
                badge(recipe.minutes() + " min"),
                badge(recipe.tags().stream().limit(3).reduce((a, b) -> a + " · " + b)
                        .orElse("American classic")));
        meta.setAlignment(Pos.CENTER_LEFT);
        Label description = new Label(recipe.description());
        description.getStyleClass().add("recipe-description");
        description.setWrapText(true);
        Label ingredientsTitle = sectionTitle("Ingredients");
        Label ingredients = new Label(String.join("  •  ", recipe.ingredients()));
        ingredients.getStyleClass().add("recipe-detail");
        ingredients.setWrapText(true);
        VBox steps = new VBox(8);
        for (int index = 0; index < recipe.steps().size(); index++) {
            Label step = new Label((index + 1) + ".  " + recipe.steps().get(index));
            step.getStyleClass().add("recipe-detail");
            step.setWrapText(true);
            steps.getChildren().add(step);
        }
        details.getChildren().addAll(meta, description, ingredientsTitle,
                ingredients, sectionTitle("How to make it"), steps);
        showScrollModal(owner, recipe.name(), 322, 520, details);
    }

    static Optional<QuickStartSelection> showQuickStart(Stage owner) {
        Stage stage = createModal(owner, "Quick Start", 318, 380);
        VBox body = new VBox(14);
        body.getStyleClass().add("dialog-content");
        body.setPadding(new Insets(14));
        Label intro = new Label("What sounds right for you today?");
        intro.getStyleClass().add("muted-text");
        GridPane grid = new GridPane();
        grid.setHgap(9);
        grid.setVgap(9);
        String[][] options = {
                {"Breakfast", "Breakfast"}, {"Lunch", "Lunch"},
                {"Dinner", "Dinner"}, {"Late-night snack", "Late-night snack"},
                {"Hearty", "Hearty"}, {"Light", "Light"}
        };
        QuickStartSelection[] selected = new QuickStartSelection[1];
        for (int i = 0; i < options.length; i++) {
            String tag = options[i][0];
            String period = options[i][1];
            Button choice = new Button(tag);
            choice.getStyleClass().add("quick-choice-button");
            choice.setMaxWidth(Double.MAX_VALUE);
            choice.setMinHeight(48);
            GridPane.setHgrow(choice, Priority.ALWAYS);
            choice.setOnAction(event -> {
                selected[0] = new QuickStartSelection(tag,
                        MEAL_PERIODS.contains(period) ? period : defaultMealPeriod());
                stage.close();
            });
            grid.add(choice, i % 2, i / 2);
        }
        body.getChildren().addAll(intro, grid);
        setCenter(stage, body);
        stage.showAndWait();
        return Optional.ofNullable(selected[0]);
    }

    static Optional<LocalDate> showSchedule(Stage owner, MealPlanStore store) {
        Stage stage = createModal(owner, "Meal Schedule", 322, 500);
        VBox body = new VBox(12);
        body.getStyleClass().add("dialog-content");
        body.setPadding(new Insets(14));
        Label hint = new Label("Choose a day to review or plan meals.");
        hint.getStyleClass().add("muted-text");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        VBox plans = new VBox(8);
        ScrollPane planScroll = new ScrollPane(plans);
        planScroll.setFitToWidth(true);
        planScroll.setPrefHeight(235);
        planScroll.getStyleClass().add("dialog-scroll");
        Consumer<LocalDate> refresh = date -> {
            plans.getChildren().clear();
            List<DailyPlan> dailyPlans = store.getPlans(date);
            if (dailyPlans.isEmpty()) {
                Label empty = new Label("No meals planned for this day.");
                empty.getStyleClass().add("muted-text");
                plans.getChildren().add(empty);
                return;
            }
            for (DailyPlan plan : dailyPlans) {
                Recipe recipe = RecipeCatalog.byId(plan.recipeId()).orElse(null);
                if (recipe == null) {
                    continue;
                }
                Button row = new Button(plan.mealPeriod() + "  ·  " + recipe.name());
                row.getStyleClass().add("schedule-row");
                row.setMaxWidth(Double.MAX_VALUE);
                row.setOnAction(event -> showRecipeDetails(stage, recipe));
                plans.getChildren().add(row);
            }
        };
        datePicker.valueProperty().addListener((observable, oldDate, newDate) -> {
            if (newDate != null) {
                refresh.accept(newDate);
            }
        });
        refresh.accept(LocalDate.now());

        LocalDate[] result = new LocalDate[1];
        Button planButton = new Button("Plan a Meal for This Day");
        planButton.getStyleClass().add("primary-button");
        planButton.setMaxWidth(Double.MAX_VALUE);
        planButton.setOnAction(event -> {
            result[0] = datePicker.getValue();
            stage.close();
        });
        body.getChildren().addAll(hint, datePicker, sectionTitle("Planned Meals"),
                planScroll, planButton);
        setCenter(stage, body);
        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    static Optional<PlanSelection> showRecommendation(
            Stage owner,
            Set<String> selectedTags,
            LocalDate targetDate,
            String initialMealPeriod
    ) {
        Stage stage = createModal(owner, "Your Meal Idea", 326, 530);
        VBox body = new VBox(11);
        body.getStyleClass().add("dialog-content");
        body.setPadding(new Insets(14));

        DatePicker datePicker = new DatePicker(targetDate == null ? LocalDate.now() : targetDate);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> periodPicker = new ComboBox<>();
        periodPicker.getItems().setAll(MEAL_PERIODS);
        periodPicker.setValue(MEAL_PERIODS.contains(initialMealPeriod)
                ? initialMealPeriod : defaultMealPeriod());
        periodPicker.setMaxWidth(Double.MAX_VALUE);

        Label recipeName = new Label();
        recipeName.getStyleClass().add("recipe-title");
        recipeName.setWrapText(true);
        Label recipeMeta = new Label();
        recipeMeta.getStyleClass().add("muted-text");
        Label description = new Label();
        description.getStyleClass().add("recipe-description");
        description.setWrapText(true);
        Label ingredients = new Label();
        ingredients.getStyleClass().add("recipe-detail");
        ingredients.setWrapText(true);

        Recipe[] current = {RecipeCatalog.recommend(selectedTags, null)};
        Consumer<Recipe> display = recipe -> {
            current[0] = recipe;
            recipeName.setText(recipe.name());
            recipeMeta.setText(recipe.minutes() + " min  ·  "
                    + recipe.tags().stream().limit(4).reduce((a, b) -> a + "  ·  " + b)
                    .orElse("American classic"));
            description.setText(recipe.description());
            ingredients.setText("Ingredients: " + String.join(", ", recipe.ingredients()));
        };
        display.accept(current[0]);
        Button viewRecipe = new Button("View Full Recipe");
        viewRecipe.getStyleClass().add("link-button");
        viewRecipe.setOnAction(event -> showRecipeDetails(stage, current[0]));

        Label addStatus = new Label();
        addStatus.getStyleClass().add("status-text");
        addStatus.setWrapText(true);
        Button another = new Button("Try Another");
        another.getStyleClass().add("secondary-button");
        another.setMaxWidth(Double.MAX_VALUE);
        another.setOnAction(event -> {
            Recipe next = RecipeCatalog.recommend(selectedTags, current[0].id());
            display.accept(next);
            addStatus.setText("");
        });

        PlanSelection[] result = new PlanSelection[1];
        Button add = new Button("Add to Menu");
        add.getStyleClass().add("primary-button");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(event -> {
            if (datePicker.getValue() == null || periodPicker.getValue() == null) {
                addStatus.setText("Choose a date and meal time first.");
                return;
            }
            result[0] = new PlanSelection(datePicker.getValue(),
                    periodPicker.getValue(), current[0]);
            stage.close();
        });

        HBox actions = new HBox(8, another, add);
        HBox.setHgrow(another, Priority.ALWAYS);
        HBox.setHgrow(add, Priority.ALWAYS);
        another.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().addAll(
                new Label("Plan this suggestion for:"), datePicker, periodPicker,
                recipeName, recipeMeta, description, ingredients, viewRecipe,
                addStatus, actions
        );
        setCenter(stage, body);
        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    static Optional<PlanEdit> showEditPlan(Stage owner, DailyPlan plan) {
        Stage stage = createModal(owner, "Edit Planned Meal", 318, 300);
        VBox body = new VBox(12);
        body.getStyleClass().add("dialog-content");
        body.setPadding(new Insets(14));
        ComboBox<String> period = new ComboBox<>();
        period.getItems().setAll(MEAL_PERIODS);
        period.setValue(plan.mealPeriod());
        period.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Recipe> recipe = new ComboBox<>();
        recipe.getItems().setAll(RecipeCatalog.all());
        recipe.setValue(RecipeCatalog.byId(plan.recipeId()).orElse(RecipeCatalog.all().get(0)));
        recipe.setMaxWidth(Double.MAX_VALUE);
        PlanEdit[] result = new PlanEdit[1];
        Button save = new Button("Save Changes");
        save.getStyleClass().add("primary-button");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(event -> {
            result[0] = new PlanEdit(period.getValue(), recipe.getValue());
            stage.close();
        });
        body.getChildren().addAll(sectionTitle("Meal Time"), period,
                sectionTitle("Recipe"), recipe, save);
        setCenter(stage, body);
        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    static Optional<LocalDate> chooseFutureDate(Stage owner) {
        Stage stage = createModal(owner, "Copy to a Future Date", 318, 230);
        VBox body = new VBox(14);
        body.getStyleClass().add("dialog-content");
        body.setPadding(new Insets(14));
        Label hint = new Label("Choose a date after today for this meal.");
        hint.getStyleClass().add("muted-text");
        DatePicker picker = new DatePicker(LocalDate.now().plusDays(1));
        picker.setMaxWidth(Double.MAX_VALUE);
        Callback<javafx.scene.control.DatePicker, DateCell> dayCellFactory = datePicker ->
                new DateCell() {
                    @Override
                    public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        setDisable(empty || date == null || !date.isAfter(LocalDate.now()));
                    }
                };
        picker.setDayCellFactory(dayCellFactory);
        LocalDate[] result = new LocalDate[1];
        Button copy = new Button("Choose Date");
        copy.getStyleClass().add("primary-button");
        copy.setMaxWidth(Double.MAX_VALUE);
        copy.setOnAction(event -> {
            if (picker.getValue() != null && picker.getValue().isAfter(LocalDate.now())) {
                result[0] = picker.getValue();
                stage.close();
            }
        });
        body.getChildren().addAll(hint, picker, copy);
        setCenter(stage, body);
        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static void showScrollModal(Stage owner, String title, double width,
                                       double height, Node content) {
        Stage stage = createModal(owner, title, width, height);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("dialog-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(stage, scroll);
        stage.showAndWait();
    }

    private static Stage createModal(Stage owner, String title,
                                     double width, double height) {
        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        }
        stage.setTitle(title);
        stage.setResizable(false);
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-root");
        HBox header = new HBox(8);
        header.getStyleClass().add("dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 14, 10, 16));
        Label heading = new Label(title);
        heading.getStyleClass().add("dialog-title");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("×");
        close.getStyleClass().add("dialog-close-button");
        close.setAccessibleText("Close dialog");
        close.setOnAction(event -> stage.close());
        header.getChildren().addAll(heading, spacer, close);
        root.setTop(header);
        Scene scene = new Scene(root, width, height);
        URL stylesheet = AppDialogs.class.getResource("/com/mealmenu/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });
        stage.setScene(scene);
        return stage;
    }

    private static void setCenter(Stage stage, Node center) {
        ((BorderPane) stage.getScene().getRoot()).setCenter(center);
    }

    private static Label badge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("time-badge");
        badge.setWrapText(true);
        return badge;
    }

    private static Label sectionTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("card-heading");
        return title;
    }

    static String defaultMealPeriod() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 10) {
            return "Breakfast";
        }
        if (hour < 15) {
            return "Lunch";
        }
        if (hour < 22) {
            return "Dinner";
        }
        return "Late-night snack";
    }

    record QuickStartSelection(String tag, String mealPeriod) {
    }

    record PlanSelection(LocalDate date, String mealPeriod, Recipe recipe) {
    }

    record PlanEdit(String mealPeriod, Recipe recipe) {
    }
}
