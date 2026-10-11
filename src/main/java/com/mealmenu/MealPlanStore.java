package com.mealmenu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class MealPlanStore {

    private final Path storageFile;
    private final Properties plans = new Properties();

    public MealPlanStore(String accountId) {
        storageFile = UserDataPaths.accountDirectory(accountId)
                .resolve("meal-plans.properties");
        if (!Files.exists(storageFile)) {
            return;
        }
        try (InputStream input = Files.newInputStream(storageFile)) {
            plans.load(input);
        } catch (IOException exception) {
            System.err.println("Could not load meal plans: " + exception.getMessage());
        }
    }

    public List<DailyPlan> getPlans(LocalDate date) {
        return getPlansForPrefix(date.toString() + ".", date);
    }

    public List<DailyPlan> getUpcomingPlans(LocalDate fromDate) {
        List<DailyPlan> result = new ArrayList<>();
        plans.stringPropertyNames().stream()
                .sorted()
                .forEach(key -> {
                    int separator = key.indexOf('.');
                    if (separator < 0) {
                        return;
                    }
                    try {
                        LocalDate date = LocalDate.parse(key.substring(0, separator));
                        if (!date.isBefore(fromDate)) {
                            DailyPlan plan = decodePlan(
                                    key.substring(separator + 1), date, plans.getProperty(key)
                            );
                            if (plan != null) {
                                result.add(plan);
                            }
                        }
                    } catch (java.time.format.DateTimeParseException ignored) {
                        // Ignore malformed local records.
                    }
                });
        result.sort(Comparator
                .comparing(DailyPlan::date)
                .thenComparingInt(plan -> mealOrder(plan.mealPeriod()))
                .thenComparing(plan -> recipeName(plan.recipeId()), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private List<DailyPlan> getPlansForPrefix(String prefix, LocalDate date) {
        List<DailyPlan> result = new ArrayList<>();
        plans.stringPropertyNames().stream()
                .filter(key -> key.startsWith(prefix))
                .sorted()
                .forEach(key -> {
                    DailyPlan plan = decodePlan(
                            key.substring(prefix.length()), date, plans.getProperty(key)
                    );
                    if (plan != null) {
                        result.add(plan);
                    }
                });
        result.sort(Comparator
                .comparingInt((DailyPlan plan) -> mealOrder(plan.mealPeriod()))
                .thenComparing(plan -> recipeName(plan.recipeId()), String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    public boolean add(LocalDate date, String mealPeriod, String recipeId) {
        String id = UUID.randomUUID().toString();
        String key = date + "." + id;
        plans.setProperty(key, encode(mealPeriod) + "." + encode(recipeId));
        if (save()) {
            return true;
        }
        plans.remove(key);
        return false;
    }

    public boolean update(DailyPlan plan, String mealPeriod, String recipeId) {
        String key = plan.date() + "." + plan.id();
        if (!plans.containsKey(key)) {
            return false;
        }
        String previous = plans.getProperty(key);
        plans.setProperty(key, encode(mealPeriod) + "." + encode(recipeId));
        if (save()) {
            return true;
        }
        plans.setProperty(key, previous);
        return false;
    }

    public boolean remove(DailyPlan plan) {
        String key = plan.date() + "." + plan.id();
        String previous = (String) plans.remove(key);
        if (previous == null) {
            return true;
        }
        if (save()) {
            return true;
        }
        plans.setProperty(key, previous);
        return false;
    }

    public boolean copyTo(DailyPlan plan, LocalDate targetDate) {
        return add(targetDate, plan.mealPeriod(), plan.recipeId());
    }

    private DailyPlan decodePlan(String id, LocalDate date, String value) {
        try {
            int separator = value.indexOf('.');
            if (separator < 0) {
                return null;
            }
            return new DailyPlan(
                    id,
                    date,
                    decode(value.substring(0, separator)),
                    decode(value.substring(separator + 1))
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean save() {
        try {
            Files.createDirectories(storageFile.getParent());
            try (OutputStream output = Files.newOutputStream(storageFile)) {
                plans.store(output, "Daily Menu meal plans");
            }
            return true;
        } catch (IOException exception) {
            System.err.println("Could not save meal plans: " + exception.getMessage());
            return false;
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private int mealOrder(String mealPeriod) {
        return switch (mealPeriod) {
            case "Breakfast" -> 0;
            case "Lunch" -> 1;
            case "Dinner" -> 2;
            case "Late-night snack" -> 3;
            default -> 4;
        };
    }

    private String recipeName(String recipeId) {
        return RecipeCatalog.byId(recipeId).map(Recipe::name).orElse(recipeId);
    }
}
