export type StorageLocation = "Pantry" | "Fridge" | "Freezer";

export type Ingredient = {
  id: string;
  name: string;
  quantity: number | null;
  unit: string;
  category: string;
  location: StorageLocation;
  addedAt: string;
};

export type RecipeIngredient = {
  name: string;
  quantity: number;
  unit: string;
};

export type Recipe = {
  id: string;
  name: string;
  description: string;
  mealType: "Breakfast" | "Lunch" | "Dinner";
  prepMinutes: number;
  cookMinutes: number;
  estimatedCost: number;
  difficulty: "Easy" | "Medium";
  ingredients: RecipeIngredient[];
  instructions: string[];
  tags: string[];
};

export type PlannedMeal = {
  id: string;
  day: string;
  mealType: "Lunch" | "Dinner";
  recipeId: string;
};

export type Preferences = {
  budget: string;
  cookingTime: string;
  goal: string;
  allergies: string;
  dislikes: string;
};
