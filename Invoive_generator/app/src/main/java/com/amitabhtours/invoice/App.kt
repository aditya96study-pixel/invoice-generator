package com.amitabhtours.invoice

import android.app.Application
import com.amitabhtours.invoice.data.AppDatabase
import com.amitabhtours.invoice.data.Repository
import com.amitabhtours.invoice.data.SettingsStore
import com.amitabhtours.invoice.pdf.PdfService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class App : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var pdf: PdfService
        private set
    lateinit var repo: Repository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.create(this)
        settings = SettingsStore(this)
        pdf = PdfService(this, db, settings)
        repo = Repository(this, db, settings, pdf)

        Configuration.getInstance().apply {
            userAgentValue = "AmitabhToursInvoice/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
            osmdroidBasePath = filesDir.resolve("osmdroid")
            osmdroidTileCache = cacheDir.resolve("osmdroid-tiles")
        }

        appScope.launch {
            runCatching { repo.installBundledAssets() }
            runCatching { repo.cleanOrphanFiles() }
        }
    }
}
