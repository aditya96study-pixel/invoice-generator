package com.amitabhtours.invoice.ui.more

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.ui.graphics.vector.ImageVector

data class HelpTopic(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val intro: String,
    val steps: List<String>,
    val tips: List<String> = emptyList(),
)

val HELP_TOPICS = listOf(
    HelpTopic(
        "setup", "First-time setup", Icons.Rounded.Settings,
        "Check your details once. Every invoice uses them.",
        listOf(
            "Open More → Settings.",
            "Business details: check the name, address, mobile and email. These print at the top of every invoice.",
            "Car & driver defaults: your car number, vehicle type, and the driver's name and mobile.",
            "Rates: the quick rate buttons (₹12 and ₹15 to start with), waiting rate per hour, free waiting minutes, your usual night charge and driver allowance per day.",
            "Invoice numbers: to carry on from your paper bill book, type the next number (for example 151). Numbers look like ATT-2026-0151.",
            "Optional: UPI QR, owner signature and stamp, customer signature (see \"Signatures, stamp and UPI QR\").",
        ),
        listOf("Changes in Settings are saved automatically — there is no Save button."),
    ),
    HelpTopic(
        "first-invoice", "Making your first invoice", Icons.AutoMirrored.Rounded.ReceiptLong,
        "From Home, tap New Invoice. It takes about a minute.",
        listOf(
            "Customer: type the name and mobile. Saved customers appear as buttons under the name — tap one to fill the details.",
            "Pickup from / Drop to: type the addresses.",
            "Vehicle & driver: usually already filled in grey from last time.",
            "Trip: pick the trip type, then type the start and end km from the odometer. Set start and end time if you want them printed (tap Now for the current time).",
            "Charges: choose the rate per km and add tolls, parking or other charges. The total at the bottom updates as you type.",
            "Tap Review. Check the details — anything copied from the last invoice that may be wrong is highlighted in yellow.",
            "Tap Choose style, pick a design, then tap Create invoice.",
            "The PDF opens. Tap Share to send it on WhatsApp or email, Save to keep a copy in Downloads, or Print.",
        ),
        listOf("The invoice number, date and time are filled in automatically when you tap Create invoice."),
    ),
    HelpTopic(
        "grey-text", "What the grey text means", Icons.Rounded.EditNote,
        "Grey text in a box is the value from your last invoice.",
        listOf(
            "If you don't touch a box with grey text, the grey value is printed exactly as shown.",
            "To change it, just type — your text replaces the grey value.",
            "To leave the box empty on this invoice, tap the × at the right. The grey value disappears.",
            "Changed your mind? Tap the ↺ arrow to bring the grey value back.",
            "Customer name, mobile, pickup and drop also carry over, but the Review screen highlights them in yellow so you don't print the previous customer's details by mistake.",
            "Start km shows last invoice's end km, because the odometer continues from there.",
        ),
        listOf("Amounts that change every trip (tolls, parking, advance, end km) start empty on purpose."),
    ),
    HelpTopic(
        "rate", "Choosing the rate: ₹12, ₹15 or other", Icons.Rounded.CurrencyRupee,
        "The distance fare is km × rate per km, worked out for you.",
        listOf(
            "In Charges, tap ₹12 or ₹15.",
            "For any other rate, tap Other and type it (for example 13.5).",
            "The rate you used last time is selected automatically next time.",
            "To change the quick buttons, go to Settings → Rates.",
        ),
    ),
    HelpTopic(
        "tracking", "Tracking a trip with GPS", Icons.Rounded.GpsFixed,
        "GPS tracking measures the distance and draws the route. Map invoice styles need it.",
        listOf(
            "When the customer gets in, tap Start trip tracking on the Home screen.",
            "The first time, allow location (choose \"While using the app\" and Precise) and notifications.",
            "A notification shows the distance and time while you drive. You can switch to other apps.",
            "Stopping for tea or a break where the meter should not run? Tap Pause. Tap Resume to continue.",
            "At the drop, tap End trip. The app finds the start and end addresses (needs internet).",
            "Tap Create invoice for this trip. Km, times, pickup and drop are filled in for you.",
            "Trips without an invoice wait on the Home screen under \"Trips waiting for an invoice\".",
        ),
        listOf(
            "Map styles (Ride Receipt, Route Map) are greyed out and locked when the invoice has no tracked trip. That is normal.",
            "If the phone restarts during a trip, tracking pauses. Open the app and tap Continue.",
        ),
    ),
    HelpTopic(
        "gps-vs-odometer", "GPS km or odometer km?", Icons.Rounded.Straighten,
        "When a trip was tracked, you choose which distance to bill each time.",
        listOf(
            "Under Km to bill you see both: Odometer (end km − start km) and GPS.",
            "Tap the one you want to bill. You can also tap Type km to enter a number yourself.",
            "If the two differ by more than 10%, a yellow warning appears. Check the odometer readings.",
            "The invoice shows which one was used, for example \"42 km (GPS tracked)\".",
        ),
        listOf("GPS can read a little short in tunnels or with weak signal. The odometer is usually the most accurate."),
    ),
    HelpTopic(
        "charges", "Tolls, parking, waiting, night, allowance and other charges", Icons.Rounded.Payments,
        "Each charge line is printed only if it has an amount.",
        listOf(
            "Toll and parking: type the total paid.",
            "Waiting: use − and + to set the hours (half-hour steps). The rate per hour comes from last time or Settings. Free waiting minutes from Settings are taken off automatically.",
            "Night driver charges: type the amount, or tap the \"Usual\" button to use your usual amount.",
            "Driver allowance (outstation): set the number of days; the per-day amount comes from last time or Settings.",
            "Anything else: tap Add a charge, type what it is for and the amount. For a discount, type a minus sign before the amount (for example −100).",
            "The total is rounded to the nearest rupee. If rounding changed it, a \"Rounded off\" line is shown.",
        ),
    ),
    HelpTopic(
        "advance", "Advance, balance due and marking paid", Icons.Rounded.CurrencyRupee,
        "Took an advance? The invoice shows the balance the customer still owes.",
        listOf(
            "In Charges, type the advance in Advance paid.",
            "The invoice prints Total, Advance paid and Balance due.",
            "When the customer pays the balance, open the invoice (History or Earnings → Payments due) and tap Mark as paid. Choose how they paid.",
            "The PDF is updated to say \"Paid in full\" and the UPI QR is removed.",
        ),
    ),
    HelpTopic(
        "receipts", "Attaching toll and parking receipts", Icons.Rounded.AttachFile,
        "Add photos or PDFs of receipts. They are added to the end of the invoice PDF.",
        listOf(
            "In the form, under Receipts, tap Camera to take a photo, Gallery to choose a photo, or PDF to choose a PDF file.",
            "Choose what it is: Toll, Parking or Other.",
            "Optional: type the amount on the receipt. For toll and parking, it is added to the charges automatically.",
            "When creating the invoice, keep \"Attach receipts to the PDF\" on. Each receipt gets its own page called \"Annexure — Receipts\".",
        ),
    ),
    HelpTopic(
        "styles", "Choosing and changing the invoice style", Icons.Rounded.Palette,
        "There are 10 designs. Your last choice is remembered.",
        listOf(
            "After Review, you see a preview of each style with your real details.",
            "Tap a style, then Create invoice.",
            "To change the style later: open the invoice and tap Style. The number and details stay the same.",
            "Styles marked MAP show the route: Ride Receipt and Route Map need a GPS-tracked trip; Journey Pass shows a map when available and a simple route line otherwise.",
            "Print Saver uses black only — best for black-and-white printers and photocopies.",
            "Set the style used for new invoices in Settings → Default invoice style.",
        ),
    ),
    HelpTopic(
        "sharing", "Sharing on WhatsApp or email, saving and printing", Icons.Rounded.Share,
        "Every invoice opens with three buttons at the bottom.",
        listOf(
            "Share: choose WhatsApp, Gmail or any other app, then the customer. A short message with the amount is added for you.",
            "Save: puts a copy in Downloads › Amitabh Tours.",
            "Print: opens the phone's print screen (Wi-Fi printer or Save as PDF).",
        ),
    ),
    HelpTopic(
        "signature-qr", "Signatures, stamp and UPI QR", Icons.Rounded.Draw,
        "All three are optional. Turn them on or off in Settings.",
        listOf(
            "Owner signature: Settings → Signatures & stamp → Choose photo. Sign on plain white paper and take a straight photo — the white background is removed automatically.",
            "Business stamp: same as the signature. It prints next to the signature.",
            "Customer signs on the phone: turn it on, and after Review the customer signs on the screen with a finger. Tap Skip if they are in a hurry.",
            "UPI QR: enter your UPI ID and name in Settings. Each unpaid invoice gets a QR with the exact amount — the customer scans it with any UPI app (GPay, PhonePe, Paytm).",
            "Test the QR from Settings: scan it and check that your name appears. Don't complete the payment.",
        ),
    ),
    HelpTopic(
        "history", "History and cancelling an invoice", Icons.Rounded.History,
        "Every invoice you create is kept.",
        listOf(
            "Open the History tab. Search by name, mobile, invoice number or place.",
            "Use the filters for Balance due, Paid or Cancelled, and for dates.",
            "Made a mistake? Open the invoice and tap Cancel this invoice at the bottom. It gets a CANCELLED stamp, stays in History, and is not counted in earnings. Then create a new, correct one.",
            "Invoices cannot be deleted, and numbers are never reused — this keeps your records honest.",
        ),
    ),
    HelpTopic(
        "earnings", "Earnings and income from outside the app", Icons.Rounded.BarChart,
        "See what you earned this week, month or financial year.",
        listOf(
            "Open the Earnings tab and choose Week, Month, Fin. year or Custom dates.",
            "Net earnings = total billed minus tolls and parking (those are paid on the customer's behalf).",
            "Averages show your income per day worked, per calendar day, per week and per month.",
            "Charts: tap a bar or point to see its value. The circle chart shows where the money came from.",
            "Payments due lists unpaid balances. Tap Paid when you collect.",
            "Earned without the app (cash trip, Ola, Uber, Rapido)? Tap Add under Other income so the figures are complete.",
        ),
        listOf("These figures include only work recorded in this app."),
    ),
    HelpTopic(
        "customers", "Customers", Icons.Rounded.Groups,
        "Customers are saved automatically from your invoices.",
        listOf(
            "Open the Customers tab to see everyone, with trips, total billed and any balance due.",
            "Tap a customer to call them, open WhatsApp, or start a new invoice with their details filled in.",
            "Usual places shows where they are normally picked up and dropped.",
            "Same person saved twice? Open one, tap the merge icon at the top, and choose the duplicate.",
        ),
    ),
    HelpTopic(
        "battery", "Battery saver and phone settings", Icons.Rounded.BatterySaver,
        "Some phones stop apps in the background to save battery, which can stop trip tracking.",
        listOf(
            "Open the phone's Settings → Apps → Amitabh Tours → Battery.",
            "Choose \"Unrestricted\" or \"Don't optimise\" (the words differ by phone brand).",
            "On Xiaomi/Redmi/POCO: also turn on Autostart for the app (Settings → Apps → Permissions → Autostart).",
            "On Samsung: Settings → Battery → Background usage limits → make sure Amitabh Tours is not in \"Sleeping apps\".",
            "On Vivo/Oppo/Realme: allow \"Background activity\" or \"Allow background running\" for the app.",
            "Keep location On and set to high accuracy while tracking.",
        ),
        listOf("The More tab has a shortcut button to the app's settings page."),
    ),
    HelpTopic(
        "faq", "Questions and problems", Icons.Rounded.QuestionAnswer,
        "Quick answers to common problems.",
        listOf(
            "Location is off / GPS not found: turn on Location from the quick settings, go outdoors, and wait up to a minute for the first fix.",
            "No internet: invoices still work. Maps on invoices need internet once to download; without it a simple route drawing is used. Addresses for tracked trips may stay blank — just type them.",
            "Wrong invoice number: change Next number in Settings → Invoice numbers. Already-used numbers are skipped automatically.",
            "Wrong details on a sent invoice: cancel it (History → open it → Cancel this invoice) and make a new one.",
            "The customer's name is wrong on the Customers list: open the customer and tap the pencil to edit.",
            "Is my data backed up? No — it stays on this phone only. Use Settings → Your data → Export now and then, and keep the files somewhere safe.",
            "Why does the invoice say \"GST not applicable\"? Because you are not registered under GST. If you register later, enter your GSTIN in Settings and the note is replaced by your GSTIN.",
        ),
    ),
)
