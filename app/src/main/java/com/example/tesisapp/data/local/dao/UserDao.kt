package com.example.tesisapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tesisapp.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow // <--- IMPORTANTE

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Opción 1: Para obtener el dato una vez (suspend)
    @Query("SELECT * FROM user_table LIMIT 1")
    suspend fun getUser(): UserEntity?

    // Opción 2: Para observar cambios en tiempo real (Flow) <- ESTA ES LA QUE NECESITAS
    @Query("SELECT * FROM user_table LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("DELETE FROM user_table")
    suspend fun clearUser()
}