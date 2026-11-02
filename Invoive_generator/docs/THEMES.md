# Invoice Themes (10)

Themes 1–4 are based on the sample images, with **slightly different colour shades** as requested. Themes 5–10 are new. Every theme uses the Amitabh Tours & Travels logo and name — **never** the names, logos or wording of TaxiForSure, Bharat Taxi, Shashank Travel or Uber. Only the layout idea is borrowed.

"Map: required" = greyed out and not clickable unless the invoice is linked to a GPS-tracked trip.

## Content every theme must show

- Logo, business name, address, mobile, email (GSTIN only if set)
- Title **INVOICE**, invoice number, date & time generated
- Customer name + mobile; pickup and drop; trip type
- Car no., vehicle type, driver name + mobile
- Start/end km, total km (and which source was billed), start/end time, total hours
- Charge lines — **only lines with a value**, including custom lines
- Total, Advance paid, **Balance due**, amount in words, payment mode (if set)
- Feedback/remarks (if set)
- Optional: UPI QR, customer signature, owner signature/stamp
- Signature area: "Customer Signature" and "For Amitabh Tours & Travels — Authorised Signatory"
- Footer: thank-you line + "GST not applicable. The supplier is not registered under GST."
- If cancelled: large diagonal "CANCELLED" stamp
- Long addresses wrap cleanly; a normal invoice fits on one A4 page; overflow continues on page 2 with a repeated header strip and page numbers.

---

### 1. Marigold Trip Bill — based on `Type1.jpeg`
- **Map:** no
- **Colours:** band `#F5B82E` (marigold, a little deeper and warmer than the sample's lemon yellow), text `#262626`, rules `#BDBDBD`, accent arrow `#E39B12`
- **Layout:** full-width coloured header band with round logo badge left, "INVOICE" centred, invoice no. right. Greeting "Dear {name}, thank you for travelling with us." Big "TRIP" + invoice number. Two columns: pickup → drop with an arrow, times and dates under each. "Billed by" driver name, car no., vehicle type. "FARE" table with columns Item / Rate / Quantity / Amount. Bold "Total Fare Payable" row.

### 2. Saffron Route — based on `Type2.jpeg`
- **Map:** no
- **Colours:** arrows and highlights `#EE8A2B` (saffron instead of yellow), text `#1E2A3A` (deep navy-black), rules `#2F2F2F`
- **Layout:** logo top-left, boxed "INVOICE COPY" top-right. Greeting line. "TRIP" heading + number. Leg rows (from → to, with time and date). "Billed by" block, car no. + vehicle type. "FARE BREAKUP" as a 4-column grid (label, value, label, value). Totals rows, then "Paid through {mode}" line.

### 3. Apricot Ledger — based on `Type3.jpeg`
- **Map:** no
- **Colours:** business name in serif `#C4553E` (softer brick red), header strip and table header `#F8CFAE` (apricot, lighter than the sample's peach), table lines `#9A9A9A`, watermark car silhouette at ~6% opacity in `#7A7A7A`
- **Layout:** small italic jurisdiction-style line at top replaced with "Pune" city line (decision). Serif business name + address left, "TAXI SERVICE INVOICE" stacked right. Apricot strip with invoice no. left and date right. Customer block, pickup line. Table: Description / Hours / Km / Amount, with empty ruled rows to fill the space. "Comments or Special Instructions" box bottom-left; Subtotal / Advance / **Balance** box bottom-right with total in red.

### 4. Ride Receipt — based on `Uber_style.jpeg`
- **Map: required**
- **Colours:** text `#111111`, secondary `#6B6B6B`, dividers `#E6E6E6` (warm light grey), route line `#111111`, start pin filled circle, end pin square (as in sample), credit lines in green `#1E8E3E` only if needed
- **Layout:** business wordmark top-left, date/time top-right. Big "Thank you for riding, {first name}". Subtitle "We hope you enjoyed your ride this {morning/afternoon/evening}." Big **Total** row. Item lines. "Payments" section with mode icon and time. Note paragraph replaced with the GST note. "Trip details": vehicle type icon, "{km} kilometres, {minutes} minutes", licence plate right. Bottom: vertical timeline (time + address for pickup and drop) on the left, **route map** on the right with rounded corners. "Your driver: {name}" row.

### 5. Executive Navy — new, formal/standard
- **Map:** no
- **Colours:** header `#1B2A4A` (navy), gold rule `#C9A227`, table zebra `#F3F5F9`, text `#1F2937`
- **Fonts:** serif headings (Noto Serif / Merriweather), sans body
- **Layout:** solid navy header with white logo + business details left, "INVOICE" in gold right with number and date. Two cards: "Billed To" and "Trip Details". Itemised table with zebra rows. Navy totals box bottom-right. Amount in words full-width under the table. Signature lines.

### 6. Print Saver — new, black & white
- **Map:** no
- **Colours:** black only, no filled areas (cheap to print, clear on photocopies and B/W printers)
- **Layout:** like a clean, typed version of his paper bill book: thin outer border, centred business name, labelled rows with values, a simple boxed totals area. Uses the monochrome logo (`branding/logo-mono.svg`).

### 7. Route Map — new, map-led
- **Map: required**
- **Colours:** accent `#0F766E` (teal), light panel `#ECF7F5`, text `#17302D`
- **Layout:** large map banner across the top third with the route and start/end pins; business logo in a white pill over the map's top-left. Below: stats row of three tiles (Distance, Duration, Average speed). Pickup → drop vertical timeline with times. Fare table, teal total bar.

### 8. Heritage Bill Book — new, traditional
- **Map:** no
- **Colours:** border and headings `#1F5F4A` (deep green), dotted fill lines `#9DB8AD`, values `#111111`
- **Layout:** deliberately mirrors his existing paper bill book, so regular customers recognise it: double-line border, centred bold business name with address and contact, then labelled rows ("Customer Name: ....") with typed values on dotted lines, two-column rows for Car No./Vehicle Type, Start/End Km, etc. Boxed Total / Advance / Balance at the lower right. "Feedback" line and signature lines at the bottom.

### 9. Journey Pass — new, unique
- **Map:** optional (shows a map strip if the trip was tracked; otherwise a stylised dotted route line, so this theme is **never** greyed out)
- **Colours:** `#3F3D8F` (indigo), accent `#F26D5B` (coral), background `#FFFFFF`, stub tint `#F1F0FA`
- **Layout:** looks like a boarding pass / ticket. Top: big FROM → TO with short place names (first part of each address) and times underneath, car icon between. Dashed perforation line with half-circle notches on both edges. Lower stub: passenger (customer), vehicle, driver, date, trip type in a grid of small labelled cells; fare breakdown; total in a coral pill. UPI QR placed on the stub like a ticket barcode (if enabled).

### 10. Royal Maroon — new, elegant
- **Map:** no
- **Colours:** `#7A1F2B` (maroon), background `#FBF6EC` (cream), thin gold `#B8963E` ornamental lines
- **Layout:** double ornamental border, centred logo and serif business name, "INVOICE" between two decorative rules. Centred, balanced layout suited to outstation / family / wedding trips. Itemised table with fine maroon rules, totals in maroon.

---

## Theme picker behaviour

- Grid of thumbnails rendered from sample data (generated once and cached).
- Map-required themes (4, 7) show a lock/map icon, 40% opacity, and the caption **"Track the trip with GPS to use this style"**; tapping shows a short explanation with a link to the Help page on tracking.
- Last-used theme is pre-selected; default theme set in Settings.
- Any saved invoice can be re-rendered in another theme from History without changing its number or data.
