package com.tdc.subvectio.data

import androidx.room.*
import com.tdc.subvectio.entities.Delivery

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM delivery WHERE id = :id")
    fun get(id: Long): Delivery

    @Query("SELECT * FROM delivery")
    fun list(): List<Delivery>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(delivery: Delivery): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(vararg delivery: Delivery)

    @Query("DELETE FROM delivery WHERE id = :id")
    fun delete(id: Long)
}