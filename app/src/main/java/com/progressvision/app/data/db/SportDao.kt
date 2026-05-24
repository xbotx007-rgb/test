package com.progressvision.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportExercise
import com.progressvision.app.data.entity.SportWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface SportDao {
    @Query("SELECT * FROM sport_workout ORDER BY createdAt DESC")
    fun observeWorkouts(): Flow<List<SportWorkout>>

    @Query("SELECT * FROM sport_workout WHERE id = :id")
    suspend fun getWorkout(id: Long): SportWorkout?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: SportWorkout): Long

    @Update
    suspend fun updateWorkout(workout: SportWorkout)

    @Delete
    suspend fun deleteWorkout(workout: SportWorkout)

    @Query("SELECT * FROM sport_exercise WHERE workoutId = :workoutId ORDER BY orderIndex ASC, createdAt ASC")
    fun observeExercises(workoutId: Long): Flow<List<SportExercise>>

    @Query("SELECT * FROM sport_exercise WHERE id = :id")
    suspend fun getExercise(id: Long): SportExercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: SportExercise): Long

    @Update
    suspend fun updateExercise(exercise: SportExercise)

    @Delete
    suspend fun deleteExercise(exercise: SportExercise)

    @Query("SELECT * FROM sport_entry WHERE exerciseId = :exerciseId ORDER BY date DESC")
    fun observeEntries(exerciseId: Long): Flow<List<SportEntry>>

    @Query("SELECT * FROM sport_entry WHERE exerciseId = :exerciseId AND date >= :since ORDER BY date ASC")
    fun observeEntriesSince(exerciseId: Long, since: Long): Flow<List<SportEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: SportEntry): Long

    @Delete
    suspend fun deleteEntry(entry: SportEntry)
}
