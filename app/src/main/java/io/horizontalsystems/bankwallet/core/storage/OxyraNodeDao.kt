package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.OxyraNodeRecord

@Dao
interface OxyraNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: OxyraNodeRecord)

    @Query("SELECT * FROM OxyraNodeRecord")
    fun getAll(): List<OxyraNodeRecord>

    @Query("DELETE FROM OxyraNodeRecord WHERE url = :url")
    fun delete(url: String)

}
