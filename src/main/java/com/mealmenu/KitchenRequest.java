package com.mealmenu;

import java.util.List;

public record KitchenRequest(
        List<String> ingredients,
        int availableMinutes,
        String cookingStyle,
        String nutritionStyle
) {
    public KitchenRequest {
        ingredients = List.copyOf(ingredients);
    }
}
