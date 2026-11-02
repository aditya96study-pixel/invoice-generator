package com.amitabhtours.invoice.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "settings")

val DEFAULT_TRIP_TYPES = listOf(
    "Local",
    "Airport Transfer",
    "Outstation (One Way)",
    "Outstation (Round Trip)",
    "Hourly Rental",
)

val PAYMENT_MODES = listOf("Cash", "UPI", "Bank transfer", "Card", "Other")

val OTHER_INCOME_SOURCES = listOf("Cash trip without invoice", "Ola", "Uber", "Rapido", "Other")

data class AppSettings(
    val businessName: String = "Amitabh Tours & Travels",
    val address: String = "Kubera Garden, NIBM Road, Kondhwa, Pune – 411048",
    val phone: String = "+91 70203 70029",
    val email: String = "amitraul100@gmail.com",
    val gstin: String = "",
    val city: String = "Pune",

    val carNo: String = "MH 12 VF 7970",
    val vehicleType: String = "Hatchback",
    val driverName: String = "Amit Raul",
    val driverMobile: String = "+91 70203 70029",

    val rateChips: List<Int> = listOf(12, 15),
    val waitingRatePaise: Long = 100_00,
    val freeWaitingMinutes: Int = 0,
    val nightChargePaise: Long = 250_00,
    val allowancePerDayPaise: Long = 300_00,
    val tripTypes: List<String> = DEFAULT_TRIP_TYPES,

    val invoicePrefix: String = "ATT",
    val nextSeq: Int = 1,
    val counterFy: Int = 0,
    val fyReset: Boolean = true,

    val defaultTheme: Int = 5,
    val lastTheme: Int = 0,

    val upiEnabled: Boolean = true,
    val upiId: String = "amitraul100@okhdfcbank",
    val upiName: String = "Amit Raul",

    val customerSignatureEnabled: Boolean = false,
    val ownerSignatureEnabled: Boolean = true,
    val ownerSignaturePath: String? = null,
    val ownerStampEnabled: Boolean = false,
    val ownerStampPath: String? = null,

    val footerText: String = "Thank you for travelling with Amitabh Tours & Travels.",
    val gstNote: String = "GST not applicable. The supplier is not registered under GST.",
    val attachReceiptsDefault: Boolean = true,

    val onboardingDone: Boolean = false,
    val bundledAssetsInstalled: Boolean = false,
    val darkMode: String = "system",
) {
    val freeWaitingHours: Double get() = freeWaitingMinutes / 60.0
    val upiReady: Boolean get() = upiEnabled && upiId.contains("@")

    fun toJson(): String = JSONObject().apply {
        put("businessName", businessName); put("address", address); put("phone", phone)
        put("email", email); put("gstin", gstin); put("city", city)
        put("carNo", carNo); put("vehicleType", vehicleType); put("driverName", driverName)
        put("driverMobile", driverMobile)
        put("rateChips", JSONArray(rateChips)); put("waitingRatePaise", waitingRatePaise)
        put("freeWaitingMinutes", freeWaitingMinutes); put("nightChargePaise", nightChargePaise)
        put("allowancePerDayPaise", allowancePerDayPaise); put("tripTypes", JSONArray(tripTypes))
        put("invoicePrefix", invoicePrefix); put("nextSeq", nextSeq); put("counterFy", counterFy)
        put("fyReset", fyReset); put("defaultTheme", defaultTheme); put("lastTheme", lastTheme)
        put("upiEnabled", upiEnabled); put("upiId", upiId); put("upiName", upiName)
        put("customerSignatureEnabled", customerSignatureEnabled)
        put("ownerSignatureEnabled", ownerSignatureEnabled); put("ownerSignaturePath", ownerSignaturePath ?: "")
        put("ownerStampEnabled", ownerStampEnabled); put("ownerStampPath", ownerStampPath ?: "")
        put("footerText", footerText); put("gstNote", gstNote)
        put("attachReceiptsDefault", attachReceiptsDefault); put("onboardingDone", onboardingDone)
        put("bundledAssetsInstalled", bundledAssetsInstalled); put("darkMode", darkMode)
    }.toString()

    companion object {
        fun fromJson(s: String?): AppSettings {
            val d = AppSettings()
            if (s.isNullOrBlank()) return d
            val o = runCatching { JSONObject(s) }.getOrNull() ?: return d
            fun str(k: String, def: String) = if (o.has(k)) o.optString(k, def) else def
            fun nstr(k: String, def: String?) = if (o.has(k)) o.optString(k).ifBlank { null } else def
            fun int(k: String, def: Int) = if (o.has(k)) o.optInt(k, def) else def
            fun long(k: String, def: Long) = if (o.has(k)) o.optLong(k, def) else def
            fun bool(k: String, def: Boolean) = if (o.has(k)) o.optBoolean(k, def) else def
            fun ints(k: String, def: List<Int>): List<Int> {
                val a = o.optJSONArray(k) ?: return def
                return (0 until a.length()).map { a.optInt(it) }.filter { it > 0 }.ifEmpty { def }
            }
            fun strs(k: String, def: List<String>): List<String> {
                val a = o.optJSONArray(k) ?: return def
                return (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }.ifEmpty { def }
            }
            return AppSettings(
                businessName = str("businessName", d.businessName),
                address = str("address", d.address),
                phone = str("phone", d.phone),
                email = str("email", d.email),
                gstin = str("gstin", d.gstin),
                city = str("city", d.city),
                carNo = str("carNo", d.carNo),
                vehicleType = str("vehicleType", d.vehicleType),
                driverName = str("driverName", d.driverName),
                driverMobile = str("driverMobile", d.driverMobile),
                rateChips = ints("rateChips", d.rateChips),
                waitingRatePaise = long("waitingRatePaise", d.waitingRatePaise),
                freeWaitingMinutes = int("freeWaitingMinutes", d.freeWaitingMinutes),
                nightChargePaise = long("nightChargePaise", d.nightChargePaise),
                allowancePerDayPaise = long("allowancePerDayPaise", d.allowancePerDayPaise),
                tripTypes = strs("tripTypes", d.tripTypes),
                invoicePrefix = str("invoicePrefix", d.invoicePrefix),
                nextSeq = int("nextSeq", d.nextSeq),
                counterFy = int("counterFy", d.counterFy),
                fyReset = bool("fyReset", d.fyReset),
                defaultTheme = int("defaultTheme", d.defaultTheme),
                lastTheme = int("lastTheme", d.lastTheme),
                upiEnabled = bool("upiEnabled", d.upiEnabled),
                upiId = str("upiId", d.upiId),
                upiName = str("upiName", d.upiName),
                customerSignatureEnabled = bool("customerSignatureEnabled", d.customerSignatureEnabled),
                ownerSignatureEnabled = bool("ownerSignatureEnabled", d.ownerSignatureEnabled),
                ownerSignaturePath = nstr("ownerSignaturePath", d.ownerSignaturePath),
                ownerStampEnabled = bool("ownerStampEnabled", d.ownerStampEnabled),
                ownerStampPath = nstr("ownerStampPath", d.ownerStampPath),
                footerText = str("footerText", d.footerText),
                gstNote = str("gstNote", d.gstNote),
                attachReceiptsDefault = bool("attachReceiptsDefault", d.attachReceiptsDefault),
                onboardingDone = bool("onboardingDone", d.onboardingDone),
                bundledAssetsInstalled = bool("bundledAssetsInstalled", d.bundledAssetsInstalled),
                darkMode = str("darkMode", d.darkMode),
            )
        }
    }
}

class SettingsStore(private val context: Context) {
    private val key = stringPreferencesKey("settings_json")

    val flow: Flow<AppSettings> = context.dataStore.data.map { AppSettings.fromJson(it[key]) }

    suspend fun get(): AppSettings = flow.first()

    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        var result = AppSettings()
        context.dataStore.edit { prefs ->
            val updated = transform(AppSettings.fromJson(prefs[key]))
            prefs[key] = updated.toJson()
            result = updated
        }
        return result
    }
}
