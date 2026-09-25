package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_user")
data class CachedUserEntity(
    @PrimaryKey val id: Long,
    val login: String,
    val name: String?,
    val avatarUrl: String,
    val bio: String?,
    val company: String?,
    val location: String?,
    val blog: String?,
    val publicRepos: Int,
    val totalPrivateRepos: Int,
    val followers: Int,
    val following: Int,
    val htmlUrl: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_repositories")
data class CachedRepoEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val fullName: String,
    val isPrivate: Boolean,
    val ownerLogin: String,
    val ownerAvatar: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val openIssues: Int,
    val defaultBranch: String,
    val cloneUrl: String,
    val sshUrl: String,
    val isArchived: Boolean,
    val isFork: Boolean,
    val visibility: String,
    val updatedAt: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_notifications")
data class CachedNotificationEntity(
    @PrimaryKey val id: String,
    val repoFullName: String,
    val title: String,
    val type: String,
    val reason: String,
    val unread: Boolean,
    val updatedAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_searches")
data class CachedSearchEntity(
    @PrimaryKey val query: String,
    val resultsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
