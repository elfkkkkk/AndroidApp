package com.example.myapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores ORDER BY score DESC LIMIT 10")
    fun getTopScores(): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE playerId = :playerId ORDER BY gameDate DESC")
    fun getScoresByPlayer(playerId: Long): Flow<List<ScoreEntity>>

    @Insert
    suspend fun insertScore(score: ScoreEntity): Long

    @Query("DELETE FROM scores WHERE playerId = :playerId")
    suspend fun deleteScoresByPlayer(playerId: Long)

    @Query("""
        SELECT s.*, p.fullName as playerName, p.course as playerCourse 
        FROM scores s 
        INNER JOIN players p ON s.playerId = p.id 
        ORDER BY s.score DESC 
        LIMIT 10
    """)
    fun getTopScoresWithPlayerInfo(): Flow<List<ScoreWithPlayerInfo>>
}

data class ScoreWithPlayerInfo(
    val id: Long,
    val playerId: Long,
    val score: Int,
    val difficulty: Int,
    val gameDate: Long,
    val playerName: String,
    val playerCourse: Int
)