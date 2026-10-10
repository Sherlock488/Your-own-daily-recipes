import { Ingredient, Recipe } from "../types";

const normalize = (s: string) => s.trim().toLowerCase();

export function recommendRecipes(
  recipes: Recipe[],
  inventory: Ingredient[],
  options?: { maxMinutes?: number; budget?: number; dislikes?: string; allergies?: string }
): Recipe[] {
  const available = inventory.map((i) => normalize(i.name));
  const dislikes = (options?.dislikes ?? "").split(",").map(normalize).filter(Boolean);
  const allergies = (options?.allergies ?? "").split(",").map(normalize).filter(Boolean);

  return recipes
    .filter((recipe) => {
      const names = recipe.ingredients.map((i) => normalize(i.name));
      if (options?.maxMinutes && recipe.prepMinutes + recipe.cookMinutes > options.maxMinutes) return false;
      if (options?.budget !== undefined && recipe.estimatedCost > options.budget) return false;
      if (dislikes.some((d) => names.some((n) => n.includes(d) || d.includes(n)))) return false;
      if (allergies.some((a) => names.some((n) => n.includes(a) || a.includes(n))) ) return false;
      return true;
    })
    .map((recipe) => {
      const matchCount = recipe.ingredients.filter((ri) =>
        available.some((a) => a.includes(normalize(ri.name)) || normalize(ri.name).includes(a))
      ).length;
      return { recipe, score: matchCount / Math.max(recipe.ingredients.length, 1) };
    })
    .sort((a, b) => b.score - a.score || a.recipe.estimatedCost - b.recipe.estimatedCost)
    .map((item) => item.recipe);
}
