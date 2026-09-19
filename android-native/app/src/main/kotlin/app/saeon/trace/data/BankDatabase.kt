package app.saeon.trace.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** A single small demo ledger snapshot is one atomic durable unit: balance,
 * intents, auth challenges, receipts and holds cannot be committed separately.
 */
@Entity(tableName = "bank_snapshot")
data class SnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val payload: String,
    val checksum: String,
    val revision: Long,
    val storedAtWall: Long
)
@Dao
interface SnapshotDao {
    @Query("SELECT * FROM bank_snapshot WHERE id = 1") suspend fun read(): SnapshotEntity?
    @Query("SELECT * FROM bank_snapshot WHERE id = 1") fun observe(): Flow<SnapshotEntity?>
    @Upsert suspend fun write(value: SnapshotEntity)
}
@Database(entities = [SnapshotEntity::class], version = 1, exportSchema = true)
abstract class BankDatabase : RoomDatabase() { abstract fun snapshots(): SnapshotDao }
