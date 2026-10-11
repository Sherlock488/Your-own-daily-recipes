package com.mealmenu;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/** "Eat" tab: today's Rutgers dining hall menus and nearby restaurants. */
public final class EatPage extends BorderPane {

    private static final int MAX_DAYS_AHEAD = 6;
    private static final Map<String, Predicate<DiningService.Item>> DIET_FILTERS = new LinkedHashMap<>();

    static {
        DIET_FILTERS.put("Vegan", item -> item.icons().contains("Vegan"));
        DIET_FILTERS.put("Vegetarian", item -> item.icons().contains("Vegan")
                || item.icons().contains("Vegetarian"));
        DIET_FILTERS.put("No Milk", item -> !item.icons().contains("Milk"));
        DIET_FILTERS.put("No Wheat", item -> !item.icons().contains("Wheat"));
        DIET_FILTERS.put("No Nuts", item -> !item.icons().contains("Peanuts")
                && !item.icons().contains("Tree Nuts"));
        DIET_FILTERS.put("No Shellfish", item -> !item.icons().contains("Shellfish"));
    }

    private final Stage owner;
    private final PlanAdder planAdder;
    private final DiningService dining = DiningService.get();

    private final StackPaneSwitch views = new StackPaneSwitch();
    private final VBox diningContent = new VBox(12);
    private final VBox restaurantContent = new VBox(12);

    // Dining hall state
    private DiningService.Hall hall;
    private String mealSlug;
    private LocalDate date = LocalDate.now();
    private final Map<String, ToggleButton> mealButtons = new LinkedHashMap<>();
    private final Map<String, ToggleButton> dietButtons = new LinkedHashMap<>();
    private final FlowPane mealChoices = new FlowPane(7, 7);
    private final Label hallName = new Label();
    private final Label hallInfo = new Label();
    private final Label dateLabel = new Label();
    private final Button previousDay = new Button("‹");
    private final Button nextDay = new Button("›");
    private final TextField dishSearch = new TextField();
    private final Label diningStatus = new Label();
    private final VBox stationList = new VBox(10);
    private List<DiningService.Station> stations = List.of();
    private int menuRequest;

    // Restaurant state
    private final TextField restaurantSearch = new TextField();
    private final ComboBox<Campus> fromCampus = new ComboBox<>();
    private final ComboBox<String> cuisinePicker = new ComboBox<>();
    private final Map<String, ToggleButton> areaButtons = new LinkedHashMap<>();
    private final ToggleGroup areaGroup = new ToggleGroup();
    private final Label restaurantStatus = new Label();
    private final VBox restaurantList = new VBox(9);

    public EatPage(Stage owner, PlanAdder planAdder) {
        this.owner = owner;
        this.planAdder = planAdder;
        getStyleClass().add("kitchen-page");

        Label title = new Label("Rutgers Eats");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Real dining hall menus and nearby restaurants.");
        subtitle.getStyleClass().add("page-subtitle");
        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton hallsMode = modeButton("Dining Halls", modeGroup);
        ToggleButton restaurantsMode = modeButton("Restaurants", modeGroup);
        HBox modes = new HBox(6, hallsMode, restaurantsMode);
        HBox.setHgrow(hallsMode, Priority.ALWAYS);
        HBox.setHgrow(restaurantsMode, Priority.ALWAYS);
        VBox header = new VBox(6, title, subtitle, modes);
        header.setPadding(new Insets(17, 17, 8, 17));
        setTop(header);

        buildDiningView();
        buildRestaurantView();
        views.add("halls", scroll(diningContent));
        views.add("restaurants", scroll(restaurantContent));
        setCenter(views);

        modeGroup.selectedToggleProperty().addListener((observable, old, selected) -> {
            if (selected == null) {
                modeGroup.selectToggle(old);
                return;
            }
            views.show(selected == hallsMode ? "halls" : "restaurants");
        });
        hallsMode.setSelected(true);
    }

    // ---------------------------------------------------------------- Dining halls

    private void buildDiningView() {
        diningContent.setPadding(new Insets(4, 17, 18, 17));
        FlowPane hallChoices = new FlowPane(7, 7);
        ToggleGroup hallGroup = new ToggleGroup();
        for (DiningService.Hall each : dining.halls()) {
            ToggleButton button = chip(each.shortName());
            button.setToggleGroup(hallGroup);
            button.setOnAction(event -> {
                if (!button.isSelected()) {
                    button.setSelected(true);
                }
                selectHall(each);
            });
            hallChoices.getChildren().add(button);
        }

        hallName.getStyleClass().add("card-heading");
        hallInfo.getStyleClass().add("muted-text");
        hallInfo.setWrapText(true);
        Button navigate = new Button("Navigate");
        navigate.getStyleClass().add("primary-button");
        navigate.setOnAction(event -> {
            if (hall != null) {
                Links.directions(hall.name(), hall.address(),
                        hall.lat() == null ? 0 : hall.lat(), hall.lng() == null ? 0 : hall.lng());
            }
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hallHeader = new HBox(8, hallName, spacer, navigate);
        hallHeader.setAlignment(Pos.CENTER_LEFT);
        VBox hallCard = new VBox(6, hallHeader, hallInfo);
        hallCard.getStyleClass().add("content-card");

        previousDay.getStyleClass().add("secondary-button");
        nextDay.getStyleClass().add("secondary-button");
        previousDay.setOnAction(event -> changeDay(-1));
        nextDay.setOnAction(event -> changeDay(1));
        dateLabel.getStyleClass().add("card-heading");
        dateLabel.setMaxWidth(Double.MAX_VALUE);
        dateLabel.setAlignment(Pos.CENTER);
        HBox.setHgrow(dateLabel, Priority.ALWAYS);
        HBox dateRow = new HBox(8, previousDay, dateLabel, nextDay);
        dateRow.setAlignment(Pos.CENTER);

        FlowPane dietChoices = new FlowPane(6, 6);
        for (String diet : DIET_FILTERS.keySet()) {
            ToggleButton button = chip(diet);
            button.getStyleClass().add("small-chip");
            button.setOnAction(event -> renderStations());
            dietButtons.put(diet, button);
            dietChoices.getChildren().add(button);
        }
        dishSearch.setPromptText("Search dishes, e.g. chicken, tofu, pizza");
        dishSearch.textProperty().addListener((observable, old, text) -> renderStations());

        diningStatus.getStyleClass().add("status-text");
        diningStatus.setWrapText(true);
        diningContent.getChildren().addAll(hallChoices, hallCard, dateRow, mealChoices,
                dishSearch, dietChoices, diningStatus, stationList);

        if (!dining.halls().isEmpty()) {
            ((ToggleButton) hallChoices.getChildren().get(0)).setSelected(true);
            selectHall(dining.halls().get(0));
        } else {
            diningStatus.setText("Dining hall data is unavailable.");
        }
    }

    private void selectHall(DiningService.Hall selected) {
        hall = selected;
        hallName.setText(hall.name());
        mealChoices.getChildren().clear();
        mealButtons.clear();
        ToggleGroup group = new ToggleGroup();
        for (DiningService.Meal meal : hall.meals()) {
            ToggleButton button = chip(meal.name());
            button.setToggleGroup(group);
            button.setOnAction(event -> {
                if (!button.isSelected()) {
                    button.setSelected(true);
                }
                mealSlug = meal.slug();
                loadMenu();
            });
            mealButtons.put(meal.slug(), button);
            mealChoices.getChildren().add(button);
        }
        if (mealSlug == null || !mealButtons.containsKey(mealSlug)) {
            mealSlug = defaultMeal();
        }
        ToggleButton current = mealButtons.get(mealSlug);
        if (current != null) {
            current.setSelected(true);
        }
        loadMenu();
    }

    private String defaultMeal() {
        LocalTime now = LocalTime.now();
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        String preferred;
        if (now.isBefore(LocalTime.of(10, 30)) && !weekend) {
            preferred = "breakfast";
        } else if (now.isBefore(LocalTime.of(16, 0))) {
            preferred = "lunch";
        } else if (now.isBefore(LocalTime.of(21, 0))) {
            preferred = "dinner";
        } else {
            preferred = "knight";
        }
        return hall.meals().stream().map(DiningService.Meal::slug)
                .filter(slug -> slug.contains(preferred)).findFirst()
                .orElse(hall.meals().isEmpty() ? null : hall.meals().get(0).slug());
    }

    private void changeDay(int delta) {
        LocalDate next = date.plusDays(delta);
        if (next.isBefore(LocalDate.now()) || next.isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            return;
        }
        date = next;
        loadMenu();
    }

    private void loadMenu() {
        if (hall == null || mealSlug == null) {
            return;
        }
        previousDay.setDisable(!date.isAfter(LocalDate.now()));
        nextDay.setDisable(!date.isBefore(LocalDate.now().plusDays(MAX_DAYS_AHEAD)));
        dateLabel.setText(dayLabel(date));
        hallInfo.setText(hall.campus() + " · " + hall.hoursOn(date) + "\n" + hall.address());
        diningStatus.setText("Loading menu…");
        stations = List.of();
        stationList.getChildren().clear();
        int request = ++menuRequest;
        dining.menu(hall.slug(), mealSlug, date).thenAccept(result -> Platform.runLater(() -> {
            if (request != menuRequest) {
                return;
            }
            stations = mainStationsFirst(result.stations());
            diningStatus.setText(result.source());
            renderStations();
        }));
    }

    private void renderStations() {
        stationList.getChildren().clear();
        if (stations.isEmpty()) {
            if (!"Loading menu…".equals(diningStatus.getText())) {
                stationList.getChildren().add(muted("No menu posted for this meal. Try another meal or day."));
            }
            return;
        }
        String query = dishSearch.getText() == null ? "" : dishSearch.getText().strip().toLowerCase(Locale.ROOT);
        List<Predicate<DiningService.Item>> active = new ArrayList<>();
        dietButtons.forEach((name, button) -> {
            if (button.isSelected()) {
                active.add(DIET_FILTERS.get(name));
            }
        });
        boolean filtering = !query.isEmpty() || !active.isEmpty();
        int shown = 0;
        for (DiningService.Station station : stations) {
            List<DiningService.Item> items = station.items().stream()
                    .filter(item -> query.isEmpty() || item.name().toLowerCase(Locale.ROOT).contains(query)
                            || station.name().toLowerCase(Locale.ROOT).contains(query))
                    .filter(item -> active.stream().allMatch(filter -> filter.test(item)))
                    .toList();
            if (items.isEmpty()) {
                continue;
            }
            stationList.getChildren().add(stationCard(station.name(), items, filtering || shown < 3));
            shown++;
        }
        if (shown == 0) {
            stationList.getChildren().add(muted("No dishes match these filters."));
        }
    }

    private static final List<String> MAIN_STATIONS = List.of("entree", "grill", "rotisserie", "pub",
            "asian", "wok", "mongolian", "halal", "kosher", "pizza", "pasta", "carving");

    /** Hot entrées first; topping bars (mostly tiny-calorie add-ons) and bagels last. */
    private static List<DiningService.Station> mainStationsFirst(List<DiningService.Station> input) {
        return input.stream().sorted(Comparator.comparingInt(EatPage::stationRank)).toList();
    }

    private static int stationRank(DiningService.Station station) {
        String name = station.name().toLowerCase(Locale.ROOT);
        double[] calories = station.items().stream().filter(item -> item.calories() != null)
                .mapToDouble(DiningService.Item::calories).sorted().toArray();
        double median = calories.length == 0 ? 100 : calories[calories.length / 2];
        if (median < 40) {
            return 3;
        }
        if (name.contains("deli") || name.contains("salad")) {
            return 1;
        }
        for (int i = 0; i < MAIN_STATIONS.size(); i++) {
            if (name.contains(MAIN_STATIONS.get(i))) {
                return 0;
            }
        }
        return name.contains("bagel") || name.contains("bread") || name.contains("fruit") ? 2 : 1;
    }

    private VBox stationCard(String name, List<DiningService.Item> items, boolean expanded) {
        Button header = new Button((expanded ? "▾  " : "▸  ") + name + "  (" + items.size() + ")");
        header.getStyleClass().add("station-header");
        header.setMaxWidth(Double.MAX_VALUE);
        VBox rows = new VBox(2);
        for (DiningService.Item item : items) {
            rows.getChildren().add(dishRow(item, name));
        }
        rows.setVisible(expanded);
        rows.setManaged(expanded);
        header.setOnAction(event -> {
            boolean show = !rows.isVisible();
            rows.setVisible(show);
            rows.setManaged(show);
            header.setText((show ? "▾  " : "▸  ") + name + "  (" + items.size() + ")");
        });
        VBox card = new VBox(4, header, rows);
        card.getStyleClass().add("station-card");
        return card;
    }

    private Node dishRow(DiningService.Item item, String station) {
        Label name = new Label(item.name());
        name.getStyleClass().add("dish-name");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);
        Label calories = new Label(item.calories() == null ? "" : Math.round(item.calories()) + " cal");
        calories.getStyleClass().add("dish-meta");
        calories.setMinWidth(Region.USE_PREF_SIZE);
        HBox top = new HBox(6, name, calories);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox row = new VBox(1, top);
        String diet = dietSummary(item);
        if (!diet.isEmpty()) {
            Label tags = new Label(diet);
            tags.getStyleClass().add("dish-tags");
            tags.setWrapText(true);
            row.getChildren().add(tags);
        }
        row.getStyleClass().add("dish-row");
        row.setOnMouseClicked(event -> EatDialogs.showDiningItem(owner, hall, station, item, date,
                periodFor(mealSlug), planAdder));
        return row;
    }

    private static String dietSummary(DiningService.Item item) {
        List<String> parts = new ArrayList<>();
        if (item.icons().contains("Vegan")) {
            parts.add("Vegan");
        } else if (item.icons().contains("Vegetarian")) {
            parts.add("Vegetarian");
        }
        List<String> allergens = item.icons().stream()
                .filter(icon -> !icon.equals("Vegan") && !icon.equals("Vegetarian")).toList();
        if (!allergens.isEmpty()) {
            parts.add("Contains " + String.join(", ", allergens).toLowerCase(Locale.ROOT));
        }
        return String.join(" · ", parts);
    }

    static String periodFor(String mealSlug) {
        if (mealSlug == null) {
            return AppDialogs.defaultMealPeriod();
        }
        if (mealSlug.contains("breakfast")) {
            return "Breakfast";
        }
        if (mealSlug.contains("lunch")) {
            return "Lunch";
        }
        if (mealSlug.contains("dinner")) {
            return "Dinner";
        }
        return "Late-night snack";
    }

    // ---------------------------------------------------------------- Restaurants

    private void buildRestaurantView() {
        restaurantContent.setPadding(new Insets(4, 17, 18, 17));
        List<RestaurantCatalog.Restaurant> all = RestaurantCatalog.all();

        restaurantSearch.setPromptText("Search restaurants or dishes, e.g. ramen");
        restaurantSearch.textProperty().addListener((observable, old, text) -> renderRestaurants());

        fromCampus.getItems().setAll(Campus.values());
        fromCampus.setValue(Campus.COLLEGE_AVE);
        fromCampus.setOnAction(event -> renderRestaurants());
        Set<String> cuisines = new TreeSet<>();
        all.forEach(r -> cuisines.add(r.cuisineGroup()));
        cuisinePicker.getItems().add("All cuisines");
        cuisinePicker.getItems().addAll(cuisines);
        cuisinePicker.setValue("All cuisines");
        cuisinePicker.setOnAction(event -> renderRestaurants());
        cuisinePicker.setMaxWidth(Double.MAX_VALUE);
        fromCampus.setMaxWidth(Double.MAX_VALUE);
        Label fromLabel = muted("From");
        fromLabel.setMinWidth(Region.USE_PREF_SIZE);
        HBox pickers = new HBox(6, fromLabel, fromCampus, cuisinePicker);
        pickers.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fromCampus, Priority.ALWAYS);
        HBox.setHgrow(cuisinePicker, Priority.ALWAYS);

        FlowPane areas = new FlowPane(6, 6);
        Set<String> areaNames = new TreeSet<>();
        all.forEach(r -> areaNames.add(r.areaGroup()));
        ToggleButton allAreas = chip("All areas");
        allAreas.getStyleClass().add("small-chip");
        allAreas.setToggleGroup(areaGroup);
        areaButtons.put("All areas", allAreas);
        areas.getChildren().add(allAreas);
        for (String area : areaNames) {
            ToggleButton button = chip(area);
            button.getStyleClass().add("small-chip");
            button.setToggleGroup(areaGroup);
            areaButtons.put(area, button);
            areas.getChildren().add(button);
        }
        allAreas.setSelected(true);
        areaGroup.selectedToggleProperty().addListener((observable, old, selected) -> {
            if (selected == null) {
                areaGroup.selectToggle(old);
                return;
            }
            renderRestaurants();
        });

        restaurantStatus.getStyleClass().add("status-text");
        restaurantStatus.setWrapText(true);
        restaurantContent.getChildren().addAll(restaurantSearch, pickers, areas,
                restaurantStatus, restaurantList);
        renderRestaurants();
    }

    private void renderRestaurants() {
        restaurantList.getChildren().clear();
        Campus campus = fromCampus.getValue() == null ? Campus.COLLEGE_AVE : fromCampus.getValue();
        String query = restaurantSearch.getText();
        String cuisine = cuisinePicker.getValue();
        String area = areaButtons.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey).findFirst().orElse("All areas");
        List<RestaurantCatalog.Restaurant> matches = RestaurantCatalog.all().stream()
                .filter(r -> "All areas".equals(area) || area.equals(r.areaGroup()))
                .filter(r -> cuisine == null || "All cuisines".equals(cuisine) || cuisine.equals(r.cuisineGroup()))
                .filter(r -> r.matches(query))
                .sorted(Comparator.comparingDouble(r -> r.hasLocation()
                        ? campus.milesTo(r.lat(), r.lng()) : Double.MAX_VALUE))
                .toList();
        if (RestaurantCatalog.all().isEmpty()) {
            restaurantStatus.setText("Restaurant data is unavailable.");
            return;
        }
        restaurantStatus.setText(matches.size() + " places · sorted by distance from " + campus.label());
        for (RestaurantCatalog.Restaurant restaurant : matches) {
            restaurantList.getChildren().add(restaurantCard(restaurant, campus, query));
        }
        if (matches.isEmpty()) {
            restaurantList.getChildren().add(muted("No restaurants match. Try another dish or area."));
        }
    }

    private Node restaurantCard(RestaurantCatalog.Restaurant restaurant, Campus campus, String query) {
        Label name = EatDialogs.cjk(new Label(restaurant.name()));
        name.getStyleClass().add("recipe-title");
        name.setWrapText(true);
        List<String> meta = new ArrayList<>();
        meta.add(restaurant.cuisine());
        if (restaurant.priceLevel() != null) {
            meta.add(restaurant.priceLevel());
        }
        if (restaurant.hasLocation()) {
            meta.add(String.format(Locale.US, "%.1f mi", campus.milesTo(restaurant.lat(), restaurant.lng())));
        }
        meta.add(restaurant.area());
        Label details = new Label(String.join(" · ", meta));
        details.getStyleClass().add("muted-text");
        details.setWrapText(true);
        VBox card = new VBox(3, name, details);
        List<String> dishes = restaurant.matchingDishes(query, 3);
        if (!dishes.isEmpty()) {
            Label dishLabel = EatDialogs.cjk(new Label("Has: " + String.join(", ", dishes)));
            dishLabel.getStyleClass().add("dish-tags");
            dishLabel.setWrapText(true);
            card.getChildren().add(dishLabel);
        } else if (restaurant.hours() != null) {
            Label hours = new Label(restaurant.hours());
            hours.getStyleClass().add("dish-tags");
            hours.setWrapText(true);
            card.getChildren().add(hours);
        }
        card.getStyleClass().addAll("content-card", "restaurant-card");
        card.setOnMouseClicked(event -> EatDialogs.showRestaurant(owner, restaurant, campus, planAdder));
        return card;
    }

    // ---------------------------------------------------------------- Helpers

    private static String dayLabel(LocalDate day) {
        String text = day.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH));
        if (day.equals(LocalDate.now())) {
            return "Today · " + text;
        }
        if (day.equals(LocalDate.now().plusDays(1))) {
            return "Tomorrow · " + text;
        }
        return text;
    }

    private static ToggleButton modeButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.getStyleClass().add("mode-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private static ToggleButton chip(String text) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("tag-choice");
        return button;
    }

    private static Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted-text");
        label.setWrapText(true);
        return label;
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    /** Saves an eat-out dish to the user's meal plan. */
    @FunctionalInterface
    public interface PlanAdder {
        boolean add(LocalDate date, String mealPeriod, String planId);
    }

    /** Minimal card switcher so each view keeps its scroll position. */
    private static final class StackPaneSwitch extends javafx.scene.layout.StackPane {
        private final Map<String, Node> pages = new LinkedHashMap<>();

        void add(String key, Node page) {
            pages.put(key, page);
            getChildren().add(page);
            page.setVisible(false);
        }

        void show(String key) {
            pages.forEach((name, page) -> page.setVisible(name.equals(key)));
        }
    }
}
