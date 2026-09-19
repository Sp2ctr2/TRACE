package dev.sp2ctr2.saeon.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/** One aggregate row keeps the demo ledger, receipt and policy mutation atomic. */
@Entity(tableName = "bank_aggregate")
data class BankRow(@PrimaryKey val id: Int = 1, val schema: Int = 1, val payload: String)
@Dao interface BankDao {
    @Query("SELECT * FROM bank_aggregate WHERE id = 1") suspend fun read(): BankRow?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun write(row: BankRow)
}
@Database(entities = [BankRow::class], version = 1, exportSchema = true)
abstract class BankDatabase : RoomDatabase() { abstract fun bankDao(): BankDao }
