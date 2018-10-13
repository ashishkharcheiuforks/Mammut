package io.github.jamiesanson.mammut.data.database.dao

import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.jamiesanson.mammut.data.database.entities.feed.Status

@Dao
interface StatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNewPage(page: List<Status>)

    @Query("SELECT * from status ORDER BY createdAt DESC")
    fun getAllPaged(): DataSource.Factory<Int, Status>

    @Query("SELECT * FROM status ORDER BY createdAt DESC LIMIT 1")
    fun getLatest(): Status?

    @Query("SELECT * FROM status ORDER BY createdAt ASC LIMIT 1")
    fun getEarliest(): Status?
}