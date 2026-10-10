import AsyncStorage from "@react-native-async-storage/async-storage";
import { Ingredient, PlannedMeal, Preferences } from "../types";

const KEYS = {
  inventory: "@nourish/inventory/v1",
  plan: "@nourish/plan/v1",
  preferences: "@nourish/preferences/v1"
};

const DEFAULT_PREFERENCES: Preferences = {
  budget: "10",
  cookingTime: "30",
  goal: "Balanced meals",
  allergies: "",
  dislikes: ""
};

async function readJson<T>(key: string, fallback: T): Promise<T> {
  try {
    const raw = await AsyncStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export const storage = {
  getInventory: () => readJson<Ingredient[]>(KEYS.inventory, []),
  setInventory: (value: Ingredient[]) => AsyncStorage.setItem(KEYS.inventory, JSON.stringify(value)),
  getPlan: () => readJson<PlannedMeal[]>(KEYS.plan, []),
  setPlan: (value: PlannedMeal[]) => AsyncStorage.setItem(KEYS.plan, JSON.stringify(value)),
  getPreferences: () => readJson<Preferences>(KEYS.preferences, DEFAULT_PREFERENCES),
  setPreferences: (value: Preferences) => AsyncStorage.setItem(KEYS.preferences, JSON.stringify(value)),
  clear: () => AsyncStorage.multiRemove(Object.values(KEYS))
};
