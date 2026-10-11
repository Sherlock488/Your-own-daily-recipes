package com.mealmenu;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface KitchenService {
    CompletableFuture<List<Recipe>> generateSuggestions(KitchenRequest request);
}
