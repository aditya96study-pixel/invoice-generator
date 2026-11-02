# Build Spec — Amitabh Tours & Travels Invoice App

Written 2026-10-27 from `ORIGINAL_PROMPT.md` and the Q&A answers in it. Where this spec makes a design choice the user did not state, it is marked **(decision)** so it can be revisited.

---

## 1. Business details (defaults, editable in Settings)

Taken from his paper bill book (`Use for data not theme.jpeg`), with spelling corrected.

| Field | Value |
|-------|-------|
| Business name | **Amitabh Tours & Travels** |
| Address | Kubera Garden, NIBM Road, Kondhwa, Pune – 411048 |
| Email | amitraul100@gmail.com *(read from a photo — confirm the spelling with the user at build start)* |
| Mobile | +91 70203 70029 |
| GSTIN | *(empty — not registered)* |
| Default car | MH 12 VF 7970 |
| Default vehicle type | Hatchback |

**Name correction:** "Amitabh Tour And Travels" → "Amitabh Tours & Travels". "Tours" is plural because the business runs many tours; "&" is the normal form inside a business name.

**No GST:** the document title is **"INVOICE"**, never "Tax Invoice". Footer note (decision, editable): *"GST not applicable. The supplier is not registered under GST."* If a GSTIN is entered in Settings later, print it under the address and drop the note (no tax calculation is in scope).

---

## 2. Tech stack

| Area | Choice |
|------|--------|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Min / target SDK | minSdk 26 (Android 8.0), target/compile SDK 36 |
| Architecture | Single-activity, MVVM, Navigation Compose, Kotlin coroutines + Flow |
| DI | Hilt (or manual DI if Hilt slows the build — keep it simple) |
| Database | Room (all records stay on the phone; **no backup**, per user) |
| Settings / sticky values | DataStore (Preferences) |
| PDF output | Android `android.graphics.pdf.PdfDocument` + `Canvas`, A4 (595 × 842 pt). No iText (AGPL). |
| Reading attached PDFs | Android `PdfRenderer` (render pages to bitmaps, draw into output PDF) |
| Location | Google Play Services `FusedLocationProviderClient` in a foreground service |
| Maps in app | osmdroid (OpenStreetMap) |
| Maps in PDF | Download OSM tiles (OkHttp, proper `User-Agent`, cached on disk), stitch into a bitmap, draw route polyline on top. Always print "© OpenStreetMap contributors". If offline, draw the route on a plain light background with start/end pins. |
| Addresses from GPS | Android `Geocoder` (reverse geocode start/end points); driver can edit |
| Charts | Vico (Compose charts) |
| QR code | ZXing core (generate UPI QR bitmap) |
| Images | Coil; camera via `ActivityResultContracts.TakePicture` + `FileProvider` |
| Files picked | `ActivityResultContracts.OpenDocument` for image/* and application/pdf; copy into app storage |

Package name (decision): `com.amitabhtours.invoice`. App name on the home screen: **"Amitabh Tours"** (full name is too long for a launcher label).

---

## 3. App structure

Bottom navigation with 5 tabs:

1. **Home** — big "New Invoice" button, "Start Trip Tracking" button, live tracking card when a trip is running, last 3 invoices, today's total.
2. **History** — all invoices, search (name / mobile / invoice no.), filter by date range and status (Paid / Balance due / Cancelled).
3. **Earnings** — charts and averages (section 9).
4. **Customers** — saved customers (section 10).
5. **More** — Settings, How to Use (Help), About.

UI style: clean, large touch targets (the user is a driver, often using it one-handed), navy + amber accent from the logo, light and dark mode. All screen text in English (Hindi/Marathi were not requested).

---

## 4. New Invoice form

### 4.1 Fields (from the paper bill, corrected)

**Invoice** — Invoice No. (auto, read-only), Date & Time (auto, at the moment the PDF is generated)

**Customer** — Customer Name, Customer Mobile, Pickup From, Drop To

**Vehicle & driver** — Car No., Vehicle Type, Driver Name, Driver Mobile

**Trip** — Trip Type, Start Km, End Km, Total Km (auto), Start Date/Time, End Date/Time, Total Hours (auto)

**Charges** (each line prints only if it has a value):
- Distance fare = billed km × rate per km
- Toll charges
- Parking charges
- Waiting charges = chargeable hours × rate per hour
- Night driver charges
- Driver allowance = days × amount per day
- Custom lines ("Add charge") — any label + amount, as many as needed
- Advance paid

**Totals** — Total Fare, Advance Paid, **Balance Due**, Amount in words

**Optional** — Payment mode (Cash / UPI / Bank transfer / Card / Other), Customer feedback / remarks, attachments, customer signature

Fields crossed out on his paper bill (Package, Per Km, Per Hour, Extra Km/Hours, SGST/CGST) are **not** included.

### 4.2 Trip types (editable list in Settings)

Local, Airport Transfer, Outstation (One Way), Outstation (Round Trip), Hourly Rental. Driver can type a new one; it gets added to the list.

### 4.3 Greyed-text memory (key requirement)

Each field shows the value from the previous invoice as **greyed placeholder text**. If the driver does not touch the field, the greyed value is used and printed as-is. Typing replaces it. A small "×" clears it to truly blank.

- Applies to every field, as the user asked.
- **(decision) Safety for customer-specific fields** (customer name, mobile, pickup, drop, km readings, times): these still carry over as grey text, but the **review screen** before generating highlights any of them that were carried over unchanged ("Same as last invoice — check"). Prevents printing the previous customer's name by accident.
- **Start Km** defaults to the previous invoice's **End Km** (odometer continues).
- Typing a mobile number or name that matches a saved customer offers to fill name/mobile/usual pickup.
- If the invoice comes from a tracked trip, trip data (times, GPS km, pickup/drop addresses) comes from the trip instead of the previous invoice.

### 4.4 Rate per km

Quick chips **₹12** and **₹15** plus "Other" for a custom value. Last-used rate is pre-selected (greyed rule). Rates are editable in Settings.

### 4.5 Km to bill (GPS vs odometer)

User answer: **driver picks each time.** When both exist, show both side by side with a selector:

- Odometer: End Km − Start Km = **X km**
- GPS tracked: **Y km**
- (also allow "Enter manually")

If only one exists, use it with no selector. Show a gentle warning if they differ by more than 10%.

### 4.6 Fare calculation (all automatic, live as the driver types)

```
distanceFare   = billedKm × ratePerKm
waitingFare    = max(0, waitingHours − freeWaitingHours) × waitingRatePerHour   (hours in 0.5 steps, decision)
driverAllow    = days × allowancePerDay
total          = distanceFare + toll + parking + waitingFare + nightCharge + driverAllow + Σ customLines
balanceDue     = total − advancePaid
```
- Round the final total to the nearest rupee (decision; show "Rounded off" line only if rounding changed it).
- Money in Indian format: ₹1,23,456.00.
- Amount in words, Indian system: "Rupees One Thousand Two Hundred Thirty-Four Only" (lakh/crore).
- Live total shown in a sticky bar at the bottom of the form.

### 4.7 Review → Theme → Generate

1. **Review screen** — read-only summary, carried-over fields highlighted, edit buttons.
2. **Theme picker** — thumbnails of all 10 themes (see `THEMES.md`). Map themes are **greyed out, not clickable**, with the caption "Track the trip with GPS to use this style" when the invoice has no tracked trip. Last-used theme pre-selected.
3. **Generate** — saves the invoice to the database, creates the PDF, opens preview with buttons: **Share** (Android share sheet: WhatsApp, email…), **Save to Downloads**, **Print**, **Change theme** (re-renders the same invoice in another theme, same invoice number).

---

## 5. Invoice numbering

- Format: `ATT-2026-0001` → prefix `ATT`, the **financial-year start year**, 4-digit running number.
- FY runs April–March. On the first invoice after 1 April, the counter resets to 0001 and the year changes.
- Prefix and next number editable in Settings (so he can continue from his paper bill book).
- Numbers are never reused. Deleting is not offered; an invoice can be **Cancelled** (kept in history, stamped "CANCELLED" on the PDF, excluded from earnings).

---

## 6. GPS trip tracking

- **Start Trip** button (Home). Asks for location + notification permission the first time, with a plain explanation. Only "while using the app" location is needed because tracking runs in a **foreground service** started from the screen.
- While running: persistent notification "Trip in progress — 12.4 km · 0:42", Home shows a live card with distance, time, and a small map. **Pause/Resume** (for breaks where the meter should not run) and **End Trip**.
- Point filtering: ignore fixes with accuracy worse than 30 m, ignore jumps implying > 150 km/h, sample every ~5 s / 10 m. Store points in Room as they arrive so a crash or phone restart does not lose the trip; resume the service after reboot if a trip was active.
- On **End Trip**: reverse-geocode start and end points to addresses, then offer **"Create invoice for this trip"** (pre-fills times, GPS km, pickup, drop, route).
- A tracked trip can also be linked to a new invoice later from a "Recent trips" list (trips not yet invoiced).
- Help section must explain battery-saver settings (some phones kill background apps; guide to "Don't optimise" for this app).

---

## 7. Attachments (optional)

- On the form: **Add receipt** → Camera / Gallery (png, jpg) / PDF file. Tag each as Toll, Parking or Other.
- Files are copied into app storage and linked to the invoice.
- When generating, option (default on): **"Attach receipts to the invoice PDF"** → appended after the invoice as an "Annexure — Receipts" section, each image scaled to fit an A4 page with its tag and amount as a caption; PDF receipts have each page rendered and appended.
- Optional quick feature (decision): when adding a toll/parking receipt the driver can type its amount, and it is added to the toll/parking total.

---

## 8. Optional features (all OFF until set up in Settings)

The user will put the images in `user-assets/` before the build.

| Feature | Setting | On the PDF |
|---------|---------|-----------|
| **UPI QR code** | UPI ID + payee name, toggle | QR for `upi://pay?pa=<id>&pn=<name>&am=<balanceDue>&cu=INR&tn=<invoiceNo>` with caption "Scan to pay ₹X via any UPI app". Hidden if balance due is 0. |
| **Customer signs on screen** | toggle | After review, a full-screen signature pad (landscape-friendly, Clear / Done / Skip). Printed above "Customer Signature". |
| **Owner signature / stamp** | upload image (PNG with transparent background preferred; offer a simple background remover = make near-white pixels transparent) | Printed above "For Amitabh Tours & Travels / Authorised Signatory". |

---

## 9. Earnings tab

**Top banner (always visible, required by user):** *"These figures include only trips recorded in this app. Income from other sources is not shown unless you add it below."*

- **Period switch:** This week / This month / This financial year / Custom range.
- **Summary cards:** Total billed, **Net earnings** (total billed minus tolls and parking, since those are pass-through costs — decision), number of trips, total km, average per trip, average per km, outstanding balance.
- **Averages:** average daily income (over days worked and over calendar days, both shown), average weekly income, average monthly income.
- **Charts:** bar chart of earnings per day (last 30 days) / per week (last 12 weeks) / per month (last 12 months); donut of fare breakdown (distance, waiting, night, allowance, other); line of trips per week.
- **Add other income (optional):** manual entries with date, amount, source (Cash trip without invoice, Ola, Uber, Rapido, Other) and note. Shown in charts in a different colour and can be toggled on/off ("Include other income").
- **Payments due:** list of invoices with balance due → "Mark as paid" (records date and mode).
- Optional (decision, low priority): simple expense log (fuel, maintenance) to show profit. Build only if time allows.

---

## 10. Customers tab

- Created automatically from invoices; matched by mobile number (fallback: exact name).
- List with search; each card shows name, mobile, number of trips, total billed, balance due.
- Detail: all their invoices, usual pickup/drop places, buttons for **Call**, **WhatsApp**, **New invoice for this customer**.
- Edit / merge duplicates.

---

## 11. Settings

- Business details (name, address, phone, email, GSTIN optional, logo — default is the app logo)
- Default car no., vehicle type, driver name, driver mobile
- Rates: per-km quick chips (12, 15, editable), waiting rate per hour + free waiting minutes, night charge default, driver allowance per day default
- Trip types list
- Invoice prefix, next number, FY reset on/off
- Default theme
- Optional features: UPI QR, customer signature, owner signature/stamp (section 8)
- Invoice footer / terms text (default: "Thank you for travelling with Amitabh Tours & Travels." + GST note)
- Attach receipts by default on/off
- **Data:** "Your data is stored only on this phone. If the phone is lost or reset, the data cannot be recovered." (user chose no backup; still allow **Export all invoices as PDFs/CSV** to Downloads as a manual safety net — decision, low effort.)

---

## 12. How to Use (Help) — detailed, required by user

Built into the app, works offline, searchable, with simple step-by-step text and screenshots/illustrations:

1. First-time setup (business details, car, rates, optional features)
2. Making your first invoice
3. What the grey text means and how to change or clear it
4. Choosing rate ₹12 / ₹15 / other
5. Tracking a trip with GPS (start, pause, end) and why map styles are greyed out without it
6. GPS km vs odometer km — which to choose
7. Adding tolls, parking, waiting, night charges, driver allowance, custom charges
8. Advance and balance due; marking an invoice paid
9. Attaching toll/parking receipts (camera, gallery, PDF)
10. Choosing an invoice style; changing the style later
11. Sharing on WhatsApp / email, saving, printing
12. Customer signature, owner stamp, UPI QR
13. History, cancelling an invoice
14. Earnings screen and adding income from outside the app
15. Customers screen
16. Battery saver / phone settings so tracking is not stopped
17. FAQ & troubleshooting (location off, no internet for map, wrong number, etc.)

Also a 3-screen welcome guide on first launch, with a link to Help.

---

## 13. Data model (Room, outline)

- `Invoice` — id, number, fyYear, seq, createdAt, status (ACTIVE/CANCELLED), customerId, customer snapshot fields, pickup, drop, carNo, vehicleType, driverName, driverMobile, tripType, startKm, endKm, gpsKm, billedKmSource (ODOMETER/GPS/MANUAL), billedKm, startTime, endTime, ratePerKm, toll, parking, waitingHours, waitingRate, freeWaitingHours, nightCharge, allowanceDays, allowancePerDay, total, roundOff, advance, balanceDue, paymentMode, paidAt, feedback, themeId, tripId?, customerSignaturePath?, pdfPath
- `CustomCharge` — id, invoiceId, label, amount
- `Attachment` — id, invoiceId, type (TOLL/PARKING/OTHER), mime, path, amount?
- `Customer` — id, name, mobile, notes, createdAt
- `Trip` — id, startedAt, endedAt, distanceMeters, startAddress, endAddress, status, invoiceId?
- `TripPoint` — tripId, time, lat, lng, accuracy, paused flag
- `OtherIncome` — id, date, amount, source, note
- (optional) `Expense` — id, date, amount, category, note

Store money as `Long` paise to avoid rounding errors.

---

## 14. Build order (suggested)

1. Install JDK 17/21, create Gradle project, app icon from `branding/`, theme, navigation shell.
2. Room + DataStore, Settings screen with business defaults.
3. New Invoice form with greyed-text memory, fare engine (+ unit tests for fare math, rounding, amount-in-words, FY numbering).
4. PDF engine with shared layout helpers; themes 1, 2, 3, then the 6 non-map/map-optional new themes.
5. Preview / share / save / print.
6. History, cancel, change theme.
7. GPS tracking service, trip → invoice, map rendering, themes 4 + other map themes, greying logic.
8. Attachments + annexure.
9. Customers tab.
10. Earnings tab with charts, other income, payments due.
11. Optional features: UPI QR, customer signature, owner stamp (use files from `user-assets/`).
12. How to Use section + welcome guide.
13. Test on a real phone / emulator: full invoice flow, a tracked drive, every theme rendered with short and very long data (long addresses must wrap, not overflow).
