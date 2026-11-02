package com.amitabhtours.invoice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amitabhtours.invoice.data.InvoiceEntity
import com.amitabhtours.invoice.data.OtherIncomeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Debug only: fills a year of invoices (some very large) to check charts and tiles. adb shell am broadcast -n com.amitabhtours.invoice/.DebugSeedReceiver */
class DebugSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rnd = Random(7)
                val now = System.currentTimeMillis()
                val day = 86_400_000L
                val names = listOf("Rohan Deshpande", "Sneha Joshi", "Venkatalakshmi Subramaniam-Raghunathan", "Imran Shaikh", "Kavita Patil")
                var n = 0
                for (back in 0 until 365) {
                    if (rnd.nextInt(3) == 0) continue
                    repeat(1 + rnd.nextInt(2)) {
                        n++
                        val km = 20.0 + rnd.nextInt(400)
                        val distance = (km * 1500).toLong()
                        val toll = if (rnd.nextBoolean()) 150_00L + rnd.nextInt(800) * 100L else 0L
                        val big = if (n % 97 == 0) 2_45_000_00L else 0L
                        val total = distance + toll + big
                        val paid = rnd.nextInt(5) != 0
                        app.db.invoices().insert(
                            InvoiceEntity(
                                number = "DBG-%04d".format(n), fyYear = 0, seq = 0,
                                createdAt = now - back * day - rnd.nextInt(10) * 3_600_000L,
                                customerName = names[n % names.size], customerMobile = "98220%05d".format(n % names.size),
                                pickup = "Kondhwa, Pune", dropAt = "Mumbai Airport",
                                billedKm = km, ratePerKmPaise = 15_00, distanceFarePaise = distance + big, tollPaise = toll,
                                subtotalPaise = total, totalPaise = total, balanceDuePaise = total,
                                paidAt = if (paid) now else null, paidMode = if (paid) "UPI" else null,
                            )
                        )
                    }
                    if (back % 9 == 0) app.db.income().insert(OtherIncomeEntity(date = now - back * day, amountPaise = (800 + rnd.nextInt(4000)) * 100L, source = "Uber"))
                }
                Log.i("DebugSeed", "inserted $n invoices")
            } catch (e: Throwable) {
                Log.e("DebugSeed", "failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
