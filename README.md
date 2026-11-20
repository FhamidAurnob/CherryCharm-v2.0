<div align="center">

# 🍒 CherryCharm

**A modern e-commerce app for handbags, built for shoppers in Bangladesh.**

Native Android app in Kotlin & Jetpack Compose · React admin dashboard · Firebase backend

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20·%20Firestore%20·%20FCM-FFCA28?logo=firebase&logoColor=black)
![Hilt](https://img.shields.io/badge/DI-Hilt-34A853)
![React](https://img.shields.io/badge/Admin-React%20%2B%20TypeScript-61DAFB?logo=react&logoColor=black)
![Min SDK](https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white)

</div>

---

## About

CherryCharm is a full-stack shopping app for a handbag boutique: totes, clutches, crossbody bags, backpacks and wallets. Customers browse, search, save favourites, check out with **Cash on Delivery, bKash, Nagad or card (SSLCommerz)** and track every order to their door. Store staff manage products, orders, customers, promo codes and push notifications from a web dashboard.

All prices are in Bangladeshi Taka (৳), delivery fees reflect inside/outside-Dhaka zones, and phone and address validation follows local formats.

## Features

### 🛍️ Shopping
- **Home:** personalised greeting, auto-scrolling banners, category row, **flash sale with a live countdown**, new arrivals, best sellers, recently viewed and a "Just for you" feed ranked by browsing history
- **Explore:** curated collections (Sale, New in, Best sellers, Featured) and category tiles with item counts
- **Search:** results as you type, suggestions, recent and trending searches
- **Sort and filter:** 7 sort options; filter by category, price range, rating, colour, in-stock and on-sale, with a live result count
- **Product page:** swipeable gallery with pinch-to-zoom, colour swatches, stock badges, "You save" amount, delivery estimate, sharing and related products
- **Reviews:** rating histogram, star filter, one editable review per customer, **Verified purchase** badge

### 🛒 Bag and checkout
- Live prices and stock in the bag, with warnings for price changes and sold-out items
- Free-delivery progress bar, undo on remove, "Save for later"
- Address picker; **৳60 inside Dhaka / ৳120 outside, free over ৳2,000**
- Promo codes (percentage or flat, minimum order, usage limits, expiry)
- Secure in-app payment page; if payment fails, retry or switch to cash on delivery. The bag is only cleared after payment succeeds.

### 📦 After purchase
- Order history with status filters and a **tracking timeline** with timestamps
- Cancel (while pending or confirmed), **Buy again**, share receipt, "Pay now" for unpaid orders
- Push and in-app notifications that deep-link to the right screen (`bagsapp://…`)

### 👤 Account
- Profile photo, edit profile, address book (Home / Work / Other, default address)
- Light / Dark / System theme, change password, delete account
- Help & FAQ with one-tap call, WhatsApp and email

### 🧑‍💼 Admin dashboard (`bags-admin/`)
- Sales dashboard with revenue and order charts
- Product management with image upload
- Order status updates, customer list, promo codes
- Push notifications to all users, a topic, or one user

## Tech stack

| Layer | Technology |
|---|---|
| Android UI | Kotlin, Jetpack Compose, Material 3, Navigation Compose, Coil |
| Architecture | MVVM, repositories, Kotlin Coroutines and Flow, Hilt dependency injection |
| Local storage | DataStore (theme, onboarding, recent searches, recently viewed) |
| Backend | Firebase Auth, Cloud Firestore (offline cache), Storage, Cloud Messaging |
| Payments | Cloud Functions → bKash, Nagad, SSLCommerz |
| Admin | React, TypeScript, Tailwind CSS, MUI icons, Recharts |

## Project structure

```
CherryCharm/
├── app/                              # Android app
│   └── src/main/java/com/example/cherrycharm/
│       ├── core/                     # Resource wrapper, formatting (৳), constants
│       ├── data/
│       │   ├── model/                # Product, Order, CartItem, Address… + Firestore mapping
│       │   ├── repository/           # Auth, Product, Cart, Order, Payment, User, Address, Notification
│       │   └── local/                # DataStore preferences
│       ├── di/                       # Hilt module
│       ├── notifications/            # FCM service + notification channels
│       └── ui/
│           ├── theme/                # Cherry colour scheme (light and dark), typography
│           ├── components/           # Reusable UI: product card, price, rating, stepper…
│           ├── navigation/           # Routes, nav host, bottom bar, deep links
│           └── screens/              # home, catalog, search, product, cart, checkout,
│                                     # orders, wishlist, profile, notifications, auth, onboarding
├── bags-admin/                       # React admin dashboard
├── functions/                        # Firebase Cloud Functions (payment gateways)
├── firestore.rules                   # Firestore security rules
└── storage.rules                     # Storage security rules
```

## Getting started

### Prerequisites
- Android Studio (Ladybug or newer) with **JDK 17**
- A Firebase project
- Node.js 18+ (for the admin dashboard and Cloud Functions)

### 1. Firebase setup
1. Create a Firebase project and add an Android app with the package name `com.example.cherrycharm`.
2. Download **`google-services.json`** and put it in `app/`. It isn't committed to this repo.
3. Turn on **Authentication → Email/Password**.
4. Create a **Firestore** database and **Storage** bucket.
5. Deploy the security rules:
   ```bash
   firebase deploy --only firestore:rules,storage
   ```

### 2. Run the Android app
Open the project in Android Studio, let Gradle sync, then click **Run ▶**.

> Debug builds pre-fill a test login (`test@cherrycharm.com` / `Test@123`). Create that user in Firebase → Authentication → Users first.

### 3. Run the admin dashboard
```bash
cd bags-admin
npm install
# create .env with your REACT_APP_FIREBASE_* web config
npm start
```
To make yourself an admin, create a document in `admins/{your-uid}` in Firestore.

### 4. Payment gateways (optional)
```bash
cd functions
npm install
firebase deploy --only functions
```
Set your bKash, Nagad and SSLCommerz credentials in the functions config. Sandbox URLs are used by default.

## Firestore data model

| Collection | Purpose |
|---|---|
| `products/{id}` | name, price, salePrice, category, images, colors, stock, rating, isActive, isFeatured… |
| `products/{id}/reviews/{uid}` | one review per customer |
| `users/{uid}` | profile, wishlist, FCM token |
| `users/{uid}/addresses`, `/notifications` | address book and inbox |
| `carts/{uid}/items` | shopping bag |
| `orders/{id}` | items, totals, address, payment, status history |
| `promoCodes/{CODE}` | discount rules |
| `categories`, `banners` | optional storefront content (the app falls back to built-in content) |
| `admins/{uid}` | dashboard access |

## Roadmap
- [ ] Confirm card and Nagad payments on the server (gateway IPN) instead of in the app
- [ ] Google sign-in with Credential Manager
- [ ] Price-drop alerts for wishlisted items
- [ ] Bangla language support

## Author

**Fhamid Mottaki Aurnob**
[LinkedIn](https://linkedin.com/in/fhamid-aurnob-115a8915a) · [GitHub](https://github.com/FhamidAurnob)

---

<div align="center">Made with 🍒 in Dhaka</div>
