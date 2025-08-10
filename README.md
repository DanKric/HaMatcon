
# HaMatcon – Android Recipe App (Kotlin + Firebase)

Browse, favorite, rate, and manage recipes. Search by ingredients with chips + autocomplete, filter by cuisine, view details, and (if you’re the owner) create/edit recipes with an image uploaded to Firebase Storage.

* **Stack:** Kotlin, AndroidX, Material Components, Navigation Component, Firebase **Auth / Firestore / Storage**, Coil
* **Package:** `com.example.hamatcon`
* **Navigation:** `FragmentContainerView` (`@id/nav_host`) + BottomNavigationView with destinations `nav_home`, `nav_my_recipes`, `nav_favorites`, `nav_profile`

---

## ✨ Features

* **Auth:** Email/password sign‑in, logout (Profile).
* **Home:** Live recipe list from Firestore; ingredient **chips** + free‑text search; cuisine filter; favorite toggle (transaction).
* **Favorites:** Shows only recipes the user favorited; live sync; unfavorite works and decrements aggregate safely via transaction.
* **My Recipes:** Owner list (with overflow menu support in adapter); **FAB** to add.
* **Add/Edit Recipe:** Image picker (system OpenDocument), Coil preview, upload to Storage, write `imageUrl`.
* **Details + Ratings:** Half‑star rating (0.5 steps). Stores per‑user rating (`value2` in half‑star units). Aggregates: `ratingSum` (sum of half‑stars), `ratingCount`; average shown consistently.
* **Profile:** Email + live counts for **Total Recipes** (owned) and **Favorite Recipes** (ignores stale favorites).

---

## 📁 App Structure (high‑level)

* `MainActivity` – sets up NavHost + BottomNav, auth‑gate to Login.
* `HomeFragment`, `FavoritesFragment`, `MyRecipesFragment`, `ProfileFragment` – four tabs (bottom nav).
* `AddRecipeActivity`, `RecipeDetailActivity` – creation/editing & details/rating screens.
* `RecipeAdapter` – binds cards; handles favorites heart, “N ratings” label, overflow menu for owner.
* Layouts: `fragment_home.xml`, `fragment_my_recipes.xml`, `fragment_profile.xml`, `activity_add_recipe.xml`, `activity_recipe_detail.xml`, `item_recipe.xml`, `dialog_rate_recipe.xml`.

---

## 🧱 Data Model (Firestore)

**Collection: `Recipes`**

```json
{
  "id": "<doc id>",
  "name": "String",
  "cuisine": "String",
  "cookTime": "String",     // minutes; UI formats to "X hr Y min"
  "difficulty": "String",
  "ingredients": ["String"],
  "instructions": "String",
  "ownerUid": "String",
  "favoritesCount": 0,
  "imageUrl": "https://…",
  "ratingSum": 0,           // sum of user half-stars (1..10)
  "ratingCount": 0,         // unique raters
  "createdAt": <Timestamp>
}
```

**Subcollection: `Recipes/{recipeId}/ratings/{uid}`**

```json
{ "value2": 1..10, "ts": <Timestamp> }  // half-star units (3.5★ => 7)
```

**Collection: `users/{uid}/favorites/{recipeId}`**

```json
{ "saved": true, "ts": <Timestamp> }
```

Average rating formula used in code: `(ratingSum / 2f) / ratingCount` (safe‑guarded when `ratingCount == 0`) .

---

## 🔧 Local Setup

1. **Clone & open in Android Studio** (Giraffe+ recommended).

2. **Java/Kotlin/AGP** – Use the versions declared in `app/build.gradle.kts`.

3. **Firebase project**

    * Create a project in Firebase Console.
    * Enable **Authentication** (Email/Password).
    * Create **Cloud Firestore** (Native mode).
    * Create **Storage** (pick a region; remember it).
    * Download `google-services.json` and place it under:

      ```
      app/google-services.json
      ```
    * Make sure the package name matches `com.example.hamatcon`.

4. **Sync Gradle** and run on an emulator or device.

---

## 🎨 UI & Navigation

* **Nav Host:** `@id/nav_host` in `activity_main.xml`; start destination `@id/nav_home`.
* **Bottom Nav Menu IDs:** `nav_home`, `nav_my_recipes`, `nav_favorites`, `nav_profile` (must match graph destination IDs).
* **Colors:** see `res/values/colors.xml` (has **bottom\_nav\_active**, **bottom\_nav\_inactive**, etc.). If you use `@color/nav_item_selector`, include a selector file:

  ```xml
  <!-- res/color/nav_item_selector.xml -->
  <selector xmlns:android="http://schemas.android.com/apk/res/android">
      <item android:color="@color/bottom_nav_active" android:state_checked="true"/>
      <item android:color="@color/bottom_nav_inactive"/>
  </selector>
  ```

  Palette examples live in `colors.xml`.

---

## 🖼️ Images (Coil)

* All recipe images load via **Coil** with a placeholder and no post‑overwrite.
* Add/Edit screen uses **OpenDocument** to pick an image, previews with Coil, uploads to `recipes/{uid}/{recipeId}.jpg`, then writes `imageUrl`.

---

## 🔐 Firebase Security Rules

### Firestore (example)

> Adjust to your exact needs; these match the app’s behavior (owner can edit doc, non‑owner can only change aggregates via transaction).

```rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() { return request.auth != null; }
    function isOwner() { return signedIn() && resource.data.ownerUid == request.auth.uid; }

    match /Recipes/{recipeId} {
      allow read: if true;
      allow create: if signedIn() && request.resource.data.ownerUid == request.auth.uid;

      // Owner full control
      allow update, delete: if isOwner();

      // Non-owner can only update aggregates (favoritesCount, ratingSum, ratingCount)
      allow update: if signedIn()
        && request.resource.data.name == resource.data.name
        && request.resource.data.cuisine == resource.data.cuisine
        && request.resource.data.cookTime == resource.data.cookTime
        && request.resource.data.difficulty == resource.data.difficulty
        && request.resource.data.ingredients == resource.data.ingredients
        && request.resource.data.instructions == resource.data.instructions
        && request.resource.data.ownerUid == resource.data.ownerUid
        && request.resource.data.imageUrl == resource.data.imageUrl
        && request.resource.data.favoritesCount is int
        && request.resource.data.ratingSum is int
        && request.resource.data.ratingCount is int;
    }

    match /Recipes/{recipeId}/ratings/{uid} {
      allow read: if true;
      allow write: if signedIn() && request.auth.uid == uid;
    }

    match /users/{uid}/favorites/{recipeId} {
      allow read, write: if signedIn() && request.auth.uid == uid;
    }
  }
}
```

### Storage (example)

```rules
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /recipes/{uid}/{file} {
      allow read: if true; // public read
      allow write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

---

## ▶️ Build & Run

1. Connect a device or start an emulator.
2. **Run ‘app’** from Android Studio.
3. On first launch, register a user (email/password).
4. Add a recipe with an image, then open it and **Rate** (0.5‑star steps).
5. Favorite/unfavorite across **Home** and **Favorites** tabs; counts should update live.

---

## 🧪 Quick Test Plan

* **Auth:** Register → Login → Profile shows email → Logout.
* **Home:** See recipes; add a few ingredient chips and free‑text tokens; filter by cuisine; toggle favorite.
* **Favorites:** Confirm list matches your favorites and unfavoriting removes the item & decrements `favoritesCount`.
* **My Recipes:** Add → Edit (replace image) → Delete; ensure favorites of deleted recipes are ignored in Profile count.
* **Details:** Rate 3.5★ → re-open → average consistent; change to 5★ → average updates; `ratingCount` stable.
* **Profile:** **Total Recipes** equals owned docs; **Favorite Recipes** equals **Favorites** screen.

---

## 🛠️ Troubleshooting

* **Bottom nav doesn’t switch tabs:** Menu item IDs must equal nav destination IDs (`nav_home`, etc.). Check `nav_graph.xml` + `bottom_nav_menu.xml`.
* **Storage upload 404 / URL write fails:** Confirm Storage bucket exists & region matches project; verify `google-services.json` and that Storage rules allow the user to write their path.
* **Images load then disappear:** Ensure nothing calls `setImageResource` after Coil `.load(...)` (adapter already avoids this).
* **Favorites count mismatch:** We update via **transaction** and Profile ignores stale favorites (recipes deleted). If still off, check rules and client code paths.
* **Ratings off by ×2:** Remember `ratingSum` is in **half‑star units**; average is `(sum/2)/count` (the app already uses this).

---

## 📦 Dependencies (core)

* AndroidX, Material Components
* Navigation Component
* **Firebase BoM** (Auth, Firestore, Storage)
* **Coil** for image loading

> Check `app/build.gradle.kts` for exact versions (keep Firebase libs managed via BoM).

---

## 🔑 Notes

* The app currently uses **public read** on Storage example rules for simplicity; consider locking this down (e.g., signed read or time‑limited download URLs).
* If you need deep links to details: a deep link pattern is scaffolded in the graph and `RecipeDetailActivity.start(...)` is already set up.

---

## 🖍️ License

Add your preferred license (MIT/Apache‑2.0/etc.).

---

If you want, I can also generate a tiny **CONTRIBUTING.md** and a **.github/ISSUE\_TEMPLATE** to streamline bug reports (with fields for device, steps to reproduce, and Logcat). Next up: I’ll start the **inline review comments** file‑by‑file.
