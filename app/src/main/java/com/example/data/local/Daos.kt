package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM cached_user LIMIT 1")
    fun getCachedUser(): Flow<CachedUserEntity?>

    @Query("SELECT * FROM cached_user LIMIT 1")
    suspend fun getCachedUserOnce(): CachedUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: CachedUserEntity)

    @Query("DELETE FROM cached_user")
    suspend fun clearUser()
}

@Dao
interface RepoDao {
    @Query("SELECT * FROM cached_repositories ORDER BY stars DESC, updatedAt DESC")
    fun getAllRepos(): Flow<List<CachedRepoEntity>>

    @Query("SELECT * FROM cached_repositories ORDER BY stars DESC, updatedAt DESC")
    suspend fun getAllReposOnce(): List<CachedRepoEntity>

    @Query("SELECT * FROM cached_repositories WHERE fullName = :fullName LIMIT 1")
    fun getRepoByFullName(fullName: String): Flow<CachedRepoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepos(repos: List<CachedRepoEntity>)

    @Query("DELETE FROM cached_repositories")
    suspend fun clearRepos()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM cached_notifications ORDER BY updatedAt DESC")
    fun getNotifications(): Flow<List<CachedNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<CachedNotificationEntity>)

    @Query("UPDATE cached_notifications SET unread = 0 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM cached_notifications")
    suspend fun clearNotifications()
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM cached_searches ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<CachedSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSearch(search: CachedSearchEntity)

    @Query("DELETE FROM cached_searches")
    suspend fun clearSearches()
}
