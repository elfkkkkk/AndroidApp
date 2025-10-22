package com.example.myapplication

import kotlinx.coroutines.flow.Flow

class GameRepository(private val database: AppDatabase) {

    fun getAllPlayers(): Flow<List<PlayerEntity>> {
        return database.playerDao().getAllPlayers()
    }

    suspend fun getPlayerById(playerId: Long): PlayerEntity? {
        return database.playerDao().getPlayerById(playerId)
    }

    suspend fun insertPlayer(player: PlayerEntity): Long {
        return database.playerDao().insertPlayer(player)
    }

    suspend fun deletePlayer(player: PlayerEntity) {
        database.playerDao().deletePlayer(player)
    }

    fun getTopScores(): Flow<List<ScoreEntity>> {
        return database.scoreDao().getTopScores()
    }

    fun getTopScoresWithPlayerInfo(): Flow<List<ScoreWithPlayerInfo>> {
        return database.scoreDao().getTopScoresWithPlayerInfo()
    }

    fun getScoresByPlayer(playerId: Long): Flow<List<ScoreEntity>> {
        return database.scoreDao().getScoresByPlayer(playerId)
    }

    suspend fun insertScore(score: ScoreEntity): Long {
        return database.scoreDao().insertScore(score)
    }

    suspend fun deleteScoresByPlayer(playerId: Long) {
        database.scoreDao().deleteScoresByPlayer(playerId)
    }
}