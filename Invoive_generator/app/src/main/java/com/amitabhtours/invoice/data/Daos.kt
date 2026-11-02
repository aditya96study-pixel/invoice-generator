package com.amitabhtours.invoice.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CustomerSummary(
    val id: Long,
    val name: String,
    val mobile: String,
    val notes: String,
    val trips: Int,
    val totalPaise: Long,
    val duePaise: Long,
    val lastTripAt: Long?,
)

data class PlaceCount(val place: String, val count: Int)

@Dao
interface InvoiceDao {
    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun get(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observe(id: Long): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC LIMIT :n")
    fun observeRecent(n: Int): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): InvoiceEntity?

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    suspend fun all(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeForCustomer(customerId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT COUNT(*) FROM invoices WHERE number = :number")
    suspend fun countWithNumber(number: String): Int

    @Query("UPDATE invoices SET customerId = :to WHERE customerId = :from")
    suspend fun reassignCustomer(from: Long, to: Long)

    @Query(
        "SELECT pickup AS place, COUNT(*) AS count FROM invoices WHERE customerId = :customerId AND pickup != '' " +
            "GROUP BY pickup ORDER BY count DESC LIMIT 3"
    )
    suspend fun topPickups(customerId: Long): List<PlaceCount>

    @Query(
        "SELECT dropAt AS place, COUNT(*) AS count FROM invoices WHERE customerId = :customerId AND dropAt != '' " +
            "GROUP BY dropAt ORDER BY count DESC LIMIT 3"
    )
    suspend fun topDrops(customerId: Long): List<PlaceCount>
}

@Dao
interface ChargeDao {
    @Insert
    suspend fun insertAll(items: List<CustomChargeEntity>)

    @Query("SELECT * FROM custom_charges WHERE invoiceId = :invoiceId ORDER BY position")
    suspend fun forInvoice(invoiceId: Long): List<CustomChargeEntity>
}

@Dao
interface AttachmentDao {
    @Insert
    suspend fun insertAll(items: List<AttachmentEntity>)

    @Query("SELECT * FROM attachments WHERE invoiceId = :invoiceId ORDER BY createdAt")
    suspend fun forInvoice(invoiceId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE invoiceId = :invoiceId ORDER BY createdAt")
    fun observeForInvoice(invoiceId: Long): Flow<List<AttachmentEntity>>
}

@Dao
interface CustomerDao {
    @Insert
    suspend fun insert(c: CustomerEntity): Long

    @Update
    suspend fun update(c: CustomerEntity)

    @Delete
    suspend fun delete(c: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun get(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observe(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE mobileKey = :key AND mobileKey != '' LIMIT 1")
    suspend fun byMobileKey(key: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun byName(name: String): CustomerEntity?

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    suspend fun all(): List<CustomerEntity>

    @Query(
        """
        SELECT c.id, c.name, c.mobile, c.notes,
            COUNT(i.id) AS trips,
            COALESCE(SUM(i.totalPaise), 0) AS totalPaise,
            COALESCE(SUM(CASE WHEN i.paidAt IS NULL THEN i.balanceDuePaise ELSE 0 END), 0) AS duePaise,
            MAX(i.createdAt) AS lastTripAt
        FROM customers c
        LEFT JOIN invoices i ON i.customerId = c.id AND i.status = 'ACTIVE'
        GROUP BY c.id
        ORDER BY lastTripAt IS NULL, lastTripAt DESC, c.name COLLATE NOCASE
        """
    )
    fun observeSummaries(): Flow<List<CustomerSummary>>
}

@Dao
interface TripDao {
    @Insert
    suspend fun insert(t: TripEntity): Long

    @Update
    suspend fun update(t: TripEntity)

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun get(id: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observe(id: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE status != 'ENDED' ORDER BY startedAt DESC LIMIT 1")
    suspend fun current(): TripEntity?

    @Query("SELECT * FROM trips WHERE status != 'ENDED' ORDER BY startedAt DESC LIMIT 1")
    fun observeCurrent(): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE status = 'ENDED' AND invoiceId IS NULL ORDER BY startedAt DESC")
    fun observeUninvoiced(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertPoint(p: TripPointEntity): Long

    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY time")
    suspend fun points(tripId: Long): List<TripPointEntity>

    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY time")
    fun observePoints(tripId: Long): Flow<List<TripPointEntity>>

    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY time DESC LIMIT 1")
    suspend fun lastPoint(tripId: Long): TripPointEntity?

    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY time ASC LIMIT 1")
    suspend fun firstPoint(tripId: Long): TripPointEntity?

    @Query("DELETE FROM trip_points WHERE tripId = :tripId")
    suspend fun deletePoints(tripId: Long)
}

@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(e: OtherIncomeEntity): Long

    @Delete
    suspend fun delete(e: OtherIncomeEntity)

    @Query("SELECT * FROM other_income ORDER BY date DESC")
    fun observeAll(): Flow<List<OtherIncomeEntity>>
}
