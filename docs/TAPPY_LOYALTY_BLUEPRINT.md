# Tappy.ma Analysis & Zero-Cost Implementation Blueprint for Qahwa Manager

## 1. What Tappy.ma Is
**Tappy.ma (طابي)** is a Moroccan customer-retention and NFC/QR table stand solution for cafés and restaurants. It replaces paper loyalty cards with:
1. **NFC + QR Counter/Table Stands:** Customers tap their phone or scan a QR code with zero app install.
2. **Digital Loyalty Stamp Cards:** Visual stamp card (e.g., 8 coffees = 9th free) with a unique customer QR code that the waiter scans to add stamps or redeem rewards.
3. **100m Proximity & "Lost Customer" Win-Back (Gbrine):** Re-engages regulars who haven't visited in 15+ days.
4. **Google Reviews Booster:** Redirects happy customers to leave a 5-star Google Maps review in one tap.

## 2. Zero-Monthly-Fee Stack (On Our Existing Firebase Spark Plan)
- **Database:** Cloud Firestore (`qahwa-manager-pos-2026`, `/cafes/{cafeId}/loyalty_customers` & `/cafes/{cafeId}/public_profile/config`).
- **Customer Web Card & Live Digital Menu:** Firebase Hosting (Free Spark Plan — `https://qahwa-manager-pos-2026.web.app/?cafe={cafeId}`).
- **Waiter Scanner & NFC Programmer:** Built into the Qahwa Manager Android app (ML Kit Barcode Scanner + Android `NfcAdapter` NDEF URI writer).
- **Win-Back Messaging:** 1-tap WhatsApp deep link (`https://wa.me/...`) for customers inactive > 15 days.
- **Hardware:** Blank `NTAG213` / `NTAG215` NFC stickers (~3 MAD each).
