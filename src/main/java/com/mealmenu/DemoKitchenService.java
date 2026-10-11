package com.mealmenu;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DemoKitchenService implements KitchenService {

    @Override
    public CompletableFuture<List<Recipe>> generateSuggestions(KitchenRequest request) {
        String ingredientSummary = String.join(", ", request.ingredients());
        String styleSummary = request.cookingStyle().toLowerCase()
                + " and "
                + request.nutritionStyle().toLowerCase();

        Recipe skillet = new Recipe(
                "garden-skillet",
                "Garden Skillet",
                Math.min(20, request.availableMinutes()),
                "A " + styleSummary + " idea using " + ingredientSummary + ".",
                java.util.Set.of("Dinner", "Hot", "Light", "Quick"),
                request.ingredients(),
                List.of(
                        "Prepare the ingredients.",
                        "Warm a pan over medium heat.",
                        "Cook until tender and serve warm."
                )
        );

        Recipe bowl = new Recipe(
                "simple-pantry-bowl",
                "Simple Pantry Bowl",
                Math.min(15, request.availableMinutes()),
                "A flexible bowl built around the ingredients you listed.",
                java.util.Set.of("Lunch", "Dinner", "Light", "Quick"),
                request.ingredients(),
                List.of(
                        "Prepare a base such as rice, noodles, or greens.",
                        "Cook the ingredients until ready.",
                        "Arrange everything in a bowl and season to taste."
                )
        );

        return CompletableFuture.completedFuture(List.of(skillet, bowl));
    }
}
