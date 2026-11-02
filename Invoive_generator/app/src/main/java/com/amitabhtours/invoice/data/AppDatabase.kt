package com.amitabhtours.invoice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        InvoiceEntity::class,
        CustomChargeEntity::class,
        AttachmentEntity::class,
        CustomerEntity::class,
        TripEntity::class,
        TripPointEntity::class,
        OtherIncomeEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoices(): InvoiceDao
    abstract fun charges(): ChargeDao
    abstract fun attachments(): AttachmentDao
    abstract fun customers(): CustomerDao
    abstract fun trips(): TripDao
    abstract fun income(): IncomeDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "amitabh_tours.db").build()
    }
}
