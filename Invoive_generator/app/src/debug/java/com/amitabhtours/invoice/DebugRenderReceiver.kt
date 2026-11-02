package com.amitabhtours.invoice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amitabhtours.invoice.domain.AmountInWords
import com.amitabhtours.invoice.domain.CustomLine
import com.amitabhtours.invoice.domain.Fare
import com.amitabhtours.invoice.domain.FareInput
import com.amitabhtours.invoice.pdf.ThemeCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** adb shell am broadcast -n com.amitabhtours.invoice/.DebugRenderReceiver [--ez online true] */
class DebugRenderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        val online = intent.getBooleanExtra("online", false)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val dir = File(context.getExternalFilesDir(null), "samples").apply { mkdirs() }
                val s = app.settings.get()
                val base = app.pdf.sampleDoc(s, withRoute = true)
                val normal = app.pdf.docFrom(
                    app.pdf.sampleEntity(s), s, base.fare, base.upiQr, null, base.ownerSignature, null,
                    base.route, base.tripActiveMs, online,
                )
                val longFare = Fare.calculate(
                    FareInput(
                        billedKm = 412.5, ratePerKmPaise = 13_50, tollPaise = 1_245_00, parkingPaise = 180_00,
                        waitingHours = 3.5, waitingRatePaise = 100_00, nightPaise = 250_00, allowanceDays = 3, allowancePerDayPaise = 300_00,
                        customLines = (1..9).map { CustomLine("Extra item number $it with a fairly long description to test wrapping", (it * 37_00).toLong()) },
                        advancePaise = 2_000_00,
                    )
                )
                val longDoc = normal.copy(
                    customerName = "Shrimati Venkatalakshmi Subramaniam-Raghunathan",
                    pickup = "Flat 1204, Tower B, Marvel Fria Society, Behind Kumar Pacific Mall, Near Swargate Bus Depot, Shankarsheth Road, Pune, Maharashtra 411037, India",
                    drop = "Hotel Ganga Ki Lehren, Plot 45, Near Laxman Jhula, Tapovan, Rishikesh, Dehradun District, Uttarakhand 249192, India",
                    feedback = "Very polite driver. Car was clean. Asked to stop twice for tea; waiting charged accordingly as agreed before the trip started.",
                    fare = longFare,
                    amountInWords = AmountInWords.rupees(longFare.balanceDuePaise),
                    paymentMode = "UPI",
                )
                val cancelled = normal.copy(cancelled = true, upiQr = null)
                val noRoute = normal.copy(route = null, mapProvider = null)
                for (t in ThemeCatalog.all) {
                    app.pdf.writeSample(normal, t, File(dir, "theme${t.id}-normal.pdf"))
                    app.pdf.writeSample(longDoc, t, File(dir, "theme${t.id}-long.pdf"))
                }
                app.pdf.writeSample(cancelled, ThemeCatalog.byId(5), File(dir, "theme5-cancelled.pdf"))
                app.pdf.writeSample(noRoute, ThemeCatalog.byId(9), File(dir, "theme9-noroute.pdf"))
                Log.i("DebugRender", "done ${dir.absolutePath}")
            } catch (e: Throwable) {
                Log.e("DebugRender", "failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
