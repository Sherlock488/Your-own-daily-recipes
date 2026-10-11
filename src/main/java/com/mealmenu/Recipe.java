package com.mealmenu;

import java.util.List;
import java.util.Set;

public record Recipe(
        String id,
        String name,
        int minutes,
        String description,
        Set<String> tags,
        List<String> ingredients,
        List<String> steps
) {
    public Recipe {
        tags = Set.copyOf(tags);
        ingredients = List.copyOf(ingredients);
        steps = List.copyOf(steps);
    }

    @Override
    public String toString() {
        return name;
    }
}
