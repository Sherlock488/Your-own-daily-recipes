import { Recipe } from "../types";

export const RECIPES: Recipe[] = [
  {
    id: "tomato-eggs",
    name: "Tomato & Egg Rice Bowl",
    description: "A comforting, quick meal with pantry-friendly ingredients.",
    mealType: "Lunch",
    prepMinutes: 5,
    cookMinutes: 10,
    estimatedCost: 2.5,
    difficulty: "Easy",
    ingredients: [
      { name: "Eggs", quantity: 2, unit: "pcs" },
      { name: "Tomatoes", quantity: 2, unit: "pcs" },
      { name: "Rice", quantity: 1, unit: "cup cooked" }
    ],
    instructions: [
      "Wash and cut the tomatoes into wedges.",
      "Beat the eggs in a bowl.",
      "Scramble the eggs in a lightly oiled pan and set aside.",
      "Cook the tomatoes until softened, then return the eggs to the pan.",
      "Serve over cooked rice and season to taste."
    ],
    tags: ["Quick", "Budget", "Beginner"]
  },
  {
    id: "spinach-egg-noodles",
    name: "Spinach Egg Noodles",
    description: "A simple noodle bowl with greens and protein.",
    mealType: "Dinner",
    prepMinutes: 5,
    cookMinutes: 12,
    estimatedCost: 3.25,
    difficulty: "Easy",
    ingredients: [
      { name: "Eggs", quantity: 1, unit: "pc" },
      { name: "Spinach", quantity: 1, unit: "cup" },
      { name: "Noodles", quantity: 1, unit: "serving" }
    ],
    instructions: [
      "Cook the noodles according to the package directions.",
      "Wilt spinach in a pan with a little oil.",
      "Add a beaten egg and cook thoroughly.",
      "Toss with the noodles and your preferred seasoning."
    ],
    tags: ["Quick", "Vegetarian", "Beginner"]
  },
  {
    id: "chicken-veggie-rice",
    name: "Chicken & Vegetable Rice",
    description: "A flexible one-pan meal for using up vegetables.",
    mealType: "Dinner",
    prepMinutes: 8,
    cookMinutes: 18,
    estimatedCost: 4.75,
    difficulty: "Easy",
    ingredients: [
      { name: "Chicken breast", quantity: 150, unit: "g" },
      { name: "Rice", quantity: 1, unit: "cup cooked" },
      { name: "Carrots", quantity: 1, unit: "pc" },
      { name: "Spinach", quantity: 1, unit: "cup" }
    ],
    instructions: [
      "Cut chicken and vegetables into bite-size pieces.",
      "Cook chicken in a pan until fully cooked through.",
      "Add carrots and a splash of water; cook until tender.",
      "Stir in spinach until wilted.",
      "Serve with cooked rice."
    ],
    tags: ["Protein", "Meal prep"]
  },
  {
    id: "potato-egg-hash",
    name: "Potato & Egg Hash",
    description: "Crispy potatoes with eggs for a satisfying meal.",
    mealType: "Breakfast",
    prepMinutes: 5,
    cookMinutes: 18,
    estimatedCost: 2.2,
    difficulty: "Easy",
    ingredients: [
      { name: "Potatoes", quantity: 2, unit: "pcs" },
      { name: "Eggs", quantity: 2, unit: "pcs" },
      { name: "Onions", quantity: 0.5, unit: "pc" }
    ],
    instructions: [
      "Dice potatoes and onion into small pieces.",
      "Pan-fry potatoes until tender and golden, adding a splash of water if needed.",
      "Add onion and cook until softened.",
      "Add eggs and cook thoroughly before serving."
    ],
    tags: ["Budget", "Vegetarian"]
  },
  {
    id: "chickpea-salad",
    name: "Chickpea Tomato Salad",
    description: "A no-fuss salad with pantry staples.",
    mealType: "Lunch",
    prepMinutes: 10,
    cookMinutes: 0,
    estimatedCost: 3,
    difficulty: "Easy",
    ingredients: [
      { name: "Chickpeas", quantity: 1, unit: "can" },
      { name: "Tomatoes", quantity: 2, unit: "pcs" },
      { name: "Spinach", quantity: 1, unit: "cup" }
    ],
    instructions: [
      "Drain and rinse chickpeas.",
      "Chop tomatoes and wash spinach.",
      "Combine in a bowl with olive oil, lemon, and seasoning if available."
    ],
    tags: ["No-cook", "Vegetarian", "Budget"]
  },
  {
    id: "beef-cabbage-stirfry",
    name: "Beef & Cabbage Stir-fry",
    description: "A fast savory stir-fry served with rice or noodles.",
    mealType: "Dinner",
    prepMinutes: 8,
    cookMinutes: 12,
    estimatedCost: 5.5,
    difficulty: "Medium",
    ingredients: [
      { name: "Beef", quantity: 150, unit: "g" },
      { name: "Cabbage", quantity: 2, unit: "cups" },
      { name: "Rice", quantity: 1, unit: "cup cooked" }
    ],
    instructions: [
      "Slice beef thinly and shred cabbage.",
      "Cook beef in a hot pan until safely cooked.",
      "Add cabbage and stir-fry until tender-crisp.",
      "Season to taste and serve with rice."
    ],
    tags: ["Protein", "Quick"]
  },
  {
    id: "banana-oats",
    name: "Banana Oatmeal",
    description: "A warm, flexible breakfast with fruit and oats.",
    mealType: "Breakfast",
    prepMinutes: 2,
    cookMinutes: 6,
    estimatedCost: 1.5,
    difficulty: "Easy",
    ingredients: [
      { name: "Oats", quantity: 0.5, unit: "cup" },
      { name: "Milk", quantity: 1, unit: "cup" },
      { name: "Banana", quantity: 1, unit: "pc" }
    ],
    instructions: [
      "Combine oats and milk in a saucepan.",
      "Cook gently, stirring, until creamy.",
      "Slice banana on top and add cinnamon if desired."
    ],
    tags: ["Breakfast", "Budget", "Vegetarian"]
  },
  {
    id: "garlic-broccoli-pasta",
    name: "Garlic Broccoli Pasta",
    description: "A simple pasta with broccoli and garlic.",
    mealType: "Dinner",
    prepMinutes: 5,
    cookMinutes: 15,
    estimatedCost: 3.5,
    difficulty: "Easy",
    ingredients: [
      { name: "Pasta", quantity: 1, unit: "serving" },
      { name: "Broccoli", quantity: 1, unit: "cup" },
      { name: "Garlic", quantity: 2, unit: "cloves" }
    ],
    instructions: [
      "Cook pasta according to package directions.",
      "Steam or boil broccoli until tender.",
      "Gently cook chopped garlic in oil.",
      "Toss pasta and broccoli with garlic oil and a little pasta water."
    ],
    tags: ["Vegetarian", "Quick"]
  },
  {
    id: "tuna-rice-bowl",
    name: "Tuna Rice Bowl",
    description: "A quick pantry meal with simple toppings.",
    mealType: "Lunch",
    prepMinutes: 5,
    cookMinutes: 0,
    estimatedCost: 3.75,
    difficulty: "Easy",
    ingredients: [
      { name: "Tuna", quantity: 1, unit: "can" },
      { name: "Rice", quantity: 1, unit: "cup cooked" },
      { name: "Cucumber", quantity: 0.5, unit: "pc" }
    ],
    instructions: [
      "Drain tuna.",
      "Slice cucumber.",
      "Serve tuna and cucumber over cooked rice with your preferred dressing."
    ],
    tags: ["Quick", "No-cook"]
  },
  {
    id: "black-bean-quesadilla",
    name: "Black Bean Quesadilla",
    description: "Crisp tortillas filled with beans and cheese.",
    mealType: "Lunch",
    prepMinutes: 5,
    cookMinutes: 8,
    estimatedCost: 2.8,
    difficulty: "Easy",
    ingredients: [
      { name: "Tortillas", quantity: 2, unit: "pcs" },
      { name: "Black beans", quantity: 0.5, unit: "can" },
      { name: "Cheese", quantity: 0.5, unit: "cup" }
    ],
    instructions: [
      "Drain and lightly mash the beans.",
      "Add beans and cheese to a tortilla and fold.",
      "Toast in a dry pan until crisp and the cheese melts."
    ],
    tags: ["Vegetarian", "Budget"]
  }
];
