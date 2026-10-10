# Nourish

**Your personal food companion.** A GitHub-ready Expo / React Native starter app for ingredient inventory, meal ideas, weekly planning, and photo-based food workflows.

## Current MVP features

- Native iOS/Android project using Expo and TypeScript.
- Home screen with recipe suggestions and kitchen summary.
- Kitchen inventory: add, search, and remove ingredients.
- Recipe recommendations from a bundled local recipe set.
- Weekly meal plan screen with recipe assignment.
- Ingredient/receipt photo selection using the device photo library or camera.
- Review-first scanning flow: scanned photos are previewed, and inventory is not changed automatically.
- Local persistence with AsyncStorage.
- Profile preferences for dietary goals, disliked foods, allergies, budget, and cooking time.
- Demo mode that is honest about AI not being configured.

## Important: photo recognition

This repository includes photo selection and a review workflow, but **real image recognition is not enabled by default**. To enable it, create a secure backend endpoint that accepts images, calls a vision-capable AI provider, validates the response, and returns structured candidates. The mobile app should call that backend through `API_BASE_URL`.

Do not embed private AI keys in the mobile application. Mobile app bundles can be inspected, so secrets must stay on a server. The `.env.example` file is documentation only; Expo public environment variables are not suitable for private credentials.

Receipt totals, quantities, ingredient amounts, expiration dates, and allergy safety must be confirmed by users when uncertain. This app does not provide medical advice.

## Requirements

- Node.js LTS
- npm
- Expo Go on your phone, or an iOS simulator / Android emulator

## Run locally

```bash
npm install
npx expo start
```

Scan the QR code with Expo Go, or press `i` for iOS Simulator (macOS with Xcode) or `a` for Android.

Type-check:

```bash
npm run typecheck
```

## Upload to GitHub

1. Create an empty repository on GitHub, for example `nourish-app`.
2. Unzip this project.
3. Open a terminal in the `nourish-app` folder.
4. Run:

```bash
git init
git add .
git commit -m "Initial Nourish app MVP"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/nourish-app.git
git push -u origin main
```

Replace `YOUR-USERNAME` with your GitHub username. If you already have a repository, do not overwrite its existing files without checking them first.

## Suggested competition demo

1. Open Home and show the product purpose.
2. Add a few ingredients in Kitchen.
3. Open Scan and choose an ingredient photo or receipt photo.
4. Explain that scan results must be reviewed before inventory is changed.
5. Select a recipe and add it to the plan.
6. Show Profile preferences and the grocery-planning concept.

## Next steps before production

- Implement and deploy a secure backend for real vision recognition and LLM recipe generation.
- Add tests and CI.
- Replace local-only storage with authenticated cloud storage if cross-device sync is required.
- Add explicit privacy disclosures and a delete-data workflow.
- Review dietary/allergy handling with suitable domain experts before making safety claims.
- Set real iOS bundle identifiers and app-store assets before release.
