package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val creatorHandle: String,
    val creatorNickname: String,
    val description: String,
    val soundTitle: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val visualTheme: String = "NEON_BEAT", // NEON_BEAT, CYBER_PARTICLES, COSMIC_HELIX, VAPOR_DRIVE
    val timestamp: Long = System.currentTimeMillis()
)
