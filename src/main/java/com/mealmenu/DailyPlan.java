package com.mealmenu;

import java.time.LocalDate;

public record DailyPlan(
        String id,
        LocalDate date,
        String mealPeriod,
        String recipeId
) {
}
