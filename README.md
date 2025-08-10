# HaMatcon 🍳

A modern Android recipe app built with Kotlin and Firebase that lets you browse, create, favorite, and rate recipes with a beautiful Material Design interface.

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)

## ✨ Features

### 🔍 **Smart Recipe Discovery**
- Ingredient-based search with interactive chips and autocomplete
- Cuisine filtering (Italian, Mexican, Asian, etc.)
- Free-text search that matches ingredient names intelligently
- Real-time recipe browsing with live sync

### ⭐ **Advanced Rating System**
- Half-star precision ratings (0.5, 1.0, 1.5... up to 5.0 stars)
- Aggregated ratings with proper transaction-based counting
- Per-user rating persistence with timestamp tracking
- Consistent rating display across all screens

### ❤️ **Favorites Management**
- One-tap favorite/unfavorite with heart animation
- Transactional favorite count updates for data consistency
- Live sync between Home and Favorites screens

### 📱 **Recipe Management**
- Create & edit recipes with rich content
- Image upload to Firebase Storage with preview
- My Recipes section for managing your creations
- Owner-only edit/delete permissions with overflow menu

### 👤 **User Profile**
- Firebase Authentication (email/password)
- Live recipe statistics (Total Recipes, Favorite Recipes)
- Clean logout functionality

## 🏗️ Architecture

### **Navigation Structure**
- Single Activity architecture with Navigation Component
- Bottom navigation with 4 main destinations:
    - 🏠 **Home** - Browse and search recipes
    - 📚 **My Recipes** - Manage your created recipes
    - ❤️ **Favorites** - View favorited recipes
    - 👤 **Profile** - User info and settings

### **Tech Stack**
- **Language:** Kotlin
- **UI:** AndroidX + Material Components
- **Navigation:** Navigation Component with Fragment destinations
- **Backend:** Firebase (Auth + Firestore + Storage)
- **Image Loading:** Coil with placeholders
- **Architecture:** MVVM-like pattern with Firebase listeners

## 🚀 Setup & Installation

### Prerequisites
- **Android Studio** Giraffe (2022.3.1) or newer
- **Android SDK** 24+ (target SDK 34)
- **Firebase project** with enabled services
- **Java 8+** for build tools

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/hamatcon.git
cd hamatcon
```

### 2. Firebase Configuration

#### Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing
3. Add Android app with package name: `com.example.hamatcon`

#### Enable Firebase Services
Enable these services in Firebase Console:
- Authentication (Email/Password provider)
- Cloud Firestore (Native mode)
- Storage (choose your preferred region)

#### Download Configuration
1. Download `google-services.json` from Firebase Console
2. Place it in: `app/google-services.json`
3. **Important:** Ensure package name matches exactly

### 3. Configure Security Rules
You'll need to set up appropriate Firestore and Storage security rules in the Firebase Console. Contact the project maintainer for the specific rule configurations needed for this app.

### 4. Build & Run
```bash
# In Android Studio:
1. Sync Project with Gradle Files
2. Connect device or start emulator
3. Run 'app' configuration
```

## 🧪 Testing Guide

### Quick Verification Checklist
- [ ] **Auth Flow:** Register → Login → Profile shows email → Logout
- [ ] **Home Search:** Add ingredient chips, filter by cuisine, toggle favorites
- [ ] **Favorites Sync:** Favorite on Home → appears in Favorites → unfavorite → disappears
- [ ] **Recipe Creation:** Add recipe with image → preview works → save → appears in My Recipes
- [ ] **Rating System:** Rate 3.5★ → reopen → average displays correctly → change to 5★ → average updates
- [ ] **Profile Counts:** Total Recipes matches My Recipes count, Favorite Recipes matches Favorites list

### Key Test Scenarios

#### Favorites Testing
```
1. Open Home, favorite a recipe (heart turns red, count +1)
2. Switch to Favorites tab → recipe appears
3. In Favorites, unfavorite same recipe → disappears from list
4. Return to Home → heart is empty, count decreased by 1
5. Check Profile → Favorite Recipes count matches Favorites tab
```

#### Rating System Testing
```
1. Open recipe details, click "Rate this recipe"
2. Set rating to 3.5 stars
3. Save and reopen details → average shows correctly
4. Change rating to 4.5 stars → average updates
5. Verify ratingCount stays same (user updated, didn't add new rating)
```

#### Image Upload Testing
```
1. Add new recipe → click "Add image" → select from gallery
2. Verify preview loads immediately
3. Save recipe → confirm image uploads successfully
4. Open recipe details → image loads properly
```

## 🛠️ Troubleshooting

### Common Issues & Solutions

#### **Image Upload Issues**
**Problem:** Image upload fails during recipe creation

**Solutions:**
- Verify Firebase Storage is enabled in console
- Check that Storage bucket exists and matches your project region
- Ensure `google-services.json` is current and matches your project
- Confirm you have proper Storage security rules configured

#### **Favorites Not Syncing**
**Problem:** Favoriting on Home doesn't appear in Favorites tab

**Solutions:**
- Check Firestore security rules allow proper read/write access
- Verify Firebase connection is stable
- Ensure user is properly authenticated

#### **Rating Display Issues**
**Problem:** Ratings show differently on different screens

**Solutions:**
- Check that all rating calculations use the same formula
- Verify rating data is being stored consistently
- Ensure proper transaction handling for rating updates

#### **Images Not Loading**
**Problem:** Recipe images don't display properly

**Solutions:**
- Verify image URLs are accessible
- Check internet connectivity
- Ensure Coil image loading is configured correctly
- Confirm images were uploaded successfully to Storage

#### **Navigation Issues**
**Problem:** Bottom navigation doesn't switch between screens

**Solutions:**
- Check that menu item IDs match navigation destination IDs
- Verify Navigation Component setup is correct
- Ensure fragments are properly configured in navigation graph

## 📦 Dependencies

### Core Libraries
```kotlin
// Firebase (managed by BOM)
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore")
implementation("com.google.firebase:firebase-storage")

// UI & Navigation
implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
implementation("com.google.android.material:material:1.11.0")

// Image Loading
implementation("io.coil-kt:coil:2.5.0")
```

### Version Compatibility
- **Kotlin:** 1.9.0+
- **Android Gradle Plugin:** 8.1.0+
- **Compile SDK:** 34
- **Min SDK:** 24
- **Target SDK:** 34

## 🔒 Security

This app implements proper security measures including:
- Firebase Authentication for user management
- Secure Firestore rules for data access control
- Proper image storage permissions
- Transaction-based updates for data consistency

For production deployment, additional security considerations should be reviewed.

## 🚀 Future Enhancements

- Recipe categories and tags
- Cooking timer integration
- Shopping list generation
- Offline recipe access
- Nutritional information
- Recipe sharing features

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

**Built with ❤️ using Kotlin and Firebase**