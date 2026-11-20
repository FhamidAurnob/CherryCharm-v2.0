# CherryCharm v2 — upgrade guide

## Install (2 minutes)

1. Close Android Studio.
2. In `H:\CherryCharm`, right-click **apply-update.ps1** → *Run with PowerShell*
   (or in a terminal: `powershell -ExecutionPolicy Bypass -File .\apply-update.ps1`).
   It backs up your current `app\src` + `app\build.gradle.kts` to `backup-before-v2-<date>.zip`,
   removes the old Kotlin package (old files clash with the new ones), and extracts `CherryCharm-v2.zip`.
3. Open the project → **File › Sync Project with Gradle Files** → Run.

Manual alternative: delete `app\src\main\java\com\example\cherrycharm`, then extract the zip into `H:\CherryCharm` and overwrite.

## Firebase setup

- Authentication → Sign-in method → enable **Email/Password** (fixes `CONFIGURATION_NOT_FOUND`).
- Deploy the new security rules: `firebase deploy --only firestore:rules,storage`
  (`firebase.json` now points at `firestore.rules` and `storage.rules`).
- Optional collections the app uses if present (falls back gracefully otherwise):
  - `categories/{id}` → `name`, `image`
  - `banners/{id}` → `title`, `subtitle`, `image`, `target` (`sale` | `new` | `best` | `featured` | a category name | `product:<id>`)
- Payments: the app calls your existing Cloud Functions (`initiateBkashPayment`, `executeBkashPayment`,
  `initiateNagadPayment`, `initiateSSLCommerzPayment`). Return URLs must stay on `your-app.web.app`
  (see `Constants.PAYMENT_RETURN_HOST`) — change both places together if you use a real domain.

## What's new

**Shopping**
- Home: personalised greeting, auto-scrolling banners, category row, flash sale with live countdown,
  new arrivals, best sellers, recently viewed, "Just for you" grid, pull-to-refresh.
- Explore tab: collections (Sale / New / Best / Featured) and category tiles with item counts.
- Search: instant results as you type, suggestions, recent + trending searches.
- Sort (7 options) and filters (category, price range, rating, colour, in-stock, on-sale) with live result count.
- Product page: swipeable gallery with pinch-to-zoom, colour swatches, quantity, stock badges,
  "You save" amount, delivery ETA, share, reviews summary, related products, Add to bag / Buy now.
- Reviews: rating histogram, filter by stars, one editable review per customer, "Verified purchase" badge.

**Bag & checkout**
- Bag uses live prices/stock, warns about price changes and sold-out items, free-delivery progress bar,
  undo on remove, "Save for later" (moves to wishlist).
- Checkout: address picker, Dhaka/outside-Dhaka delivery fees (৳60 / ৳120, free over ৳2,000),
  promo codes with clear errors, delivery note, COD / bKash / Nagad / Card.
- Online payment in a secure in-app page; if it fails you can retry or switch to cash on delivery.
  Bag is only emptied after payment succeeds.

**After purchase**
- Orders with status filters, tracking timeline with timestamps, cancel (while pending/confirmed),
  buy again, share receipt, "Pay now" for unpaid orders, rate delivered items.
- Push + in-app notifications grouped by day; tapping opens the right screen (deep links `bagsapp://…`).

**Account**
- Profile with order shortcuts (to pay / to ship / shipped / delivered), photo upload, edit profile.
- Address book (Home/Work/Other, default address, BD phone + postcode validation).
- Settings: light/dark/system theme, change password, delete account. Help & FAQ with call/WhatsApp/email.

**Under the hood**
- Clean architecture: `data` (models, repositories) → `ui/screens/*` (ViewModel + screen per feature), Hilt DI.
- One live Firestore listener per collection shared across screens; offline cache (100 MB).
- Manual Firestore mapping — fixes the old bug where `isActive`/`isFeatured`/`isDefault` never loaded.
- Removed unused libraries (Stripe, Retrofit, Paging, Lottie, Accompanist) for a smaller, faster app.

## Known follow-ups
- Payment confirmation for SSLCommerz/Nagad is written by the app after the gateway redirects.
  For production, move "mark as paid" into a Cloud Function that validates with the gateway (IPN),
  then tighten the `orders` update rule so customers can't set `paymentStatus`.
- Google sign-in button was removed (it wasn't wired up). Add it with Credential Manager when ready.
