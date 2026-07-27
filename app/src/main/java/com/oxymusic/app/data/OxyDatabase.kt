package com.oxymusic.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val trackId: String, val title: String, val artist: String,
    val thumbnailUrl: String, val durationMs: Long, val playedAt: Long,
)

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 100")
    fun observe(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class OxyDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: android.content.Context): OxyDatabase =
        androidx.room.Room.databaseBuilder(ctx, OxyDatabase::class.java, "oxy.db").build()

    @Provides
    fun provideHistoryDao(db: OxyDatabase): HistoryDao = db.historyDao()
}
