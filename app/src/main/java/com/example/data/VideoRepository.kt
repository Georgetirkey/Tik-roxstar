package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class VideoRepository(private val videoDao: VideoDao) {

    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()

    fun getCommentsForVideo(videoId: String): Flow<List<CommentEntity>> = 
        videoDao.getCommentsForVideo(videoId)

    suspend fun insertVideo(video: VideoEntity) {
        videoDao.insertVideo(video)
    }

    suspend fun updateVideo(video: VideoEntity) {
        videoDao.updateVideo(video)
    }

    suspend fun deleteVideoById(id: String) {
        videoDao.deleteVideoById(id)
    }

    suspend fun insertComment(comment: CommentEntity) {
        videoDao.insertComment(comment)
    }

    suspend fun updateComment(comment: CommentEntity) {
        videoDao.updateComment(comment)
    }

    suspend fun deleteCommentById(id: Int) {
        videoDao.deleteCommentById(id)
    }

    suspend fun prepSeedsIfEmpty() {
        val current = videoDao.getAllVideos().first()
        if (current.isEmpty()) {
            val defaultVideos = listOf(
                VideoEntity(
                    id = "vid_1",
                    creatorHandle = "@neon_lucas",
                    creatorNickname = "Lucas Webb",
                    description = "Interactive audio pulse synthesizer built on Jetpack Compose Canvas! Wait for the drop 🎹🔥 #synth #music #visualizer #kotlin #android",
                    soundTitle = "Original Sound - @neon_lucas",
                    likesCount = 14205,
                    commentsCount = 248,
                    sharesCount = 590,
                    isLiked = false,
                    isBookmarked = false,
                    visualTheme = "NEON_BEAT"
                ),
                VideoEntity(
                    id = "vid_2",
                    creatorHandle = "@clara_rocks",
                    creatorNickname = "Clara Croft",
                    description = "Hypnotic Cyber Particle stream on Android. Double-tap to interact with the magnetic vector fields! 💫👾 #interactive #particles #cyberpunk #viral",
                    soundTitle = "Cyber Odyssey (Continuous Mix) - Clara",
                    likesCount = 28941,
                    commentsCount = 512,
                    sharesCount = 1391,
                    isLiked = true, // We start liked to show filled state out-of-the-box
                    isBookmarked = false,
                    visualTheme = "CYBER_PARTICLES"
                ),
                VideoEntity(
                    id = "vid_3",
                    creatorHandle = "@cosmic_explorer",
                    creatorNickname = "Dr. Stella",
                    description = "Simulating gravitational orbits of twin stars spinning into a mathematical singularity. Astrophysics matches fine art 💫🛰️🌌 #space #science #astronomy #relax",
                    soundTitle = "Ambient Cosmos Vol. 4 - Stella",
                    likesCount = 8530,
                    commentsCount = 87,
                    sharesCount = 240,
                    isLiked = false,
                    isBookmarked = true,
                    visualTheme = "COSMIC_HELIX"
                ),
                VideoEntity(
                    id = "vid_4",
                    creatorHandle = "@sunset_glider",
                    creatorNickname = "Vapor Cruiser",
                    description = "Retro highway endless grid drift. Sunset vibes & lo-fi organic beats. Chill with me for a minute 🌴🌅🌉 #vaporwave #retrowave #lofi #chill",
                    soundTitle = "Vapor Cruiser - Late Night Chillout",
                    likesCount = 37400,
                    commentsCount = 981,
                    sharesCount = 4210,
                    isLiked = false,
                    isBookmarked = false,
                    visualTheme = "VAPOR_DRIVE"
                )
            )

            for (video in defaultVideos) {
                videoDao.insertVideo(video)
            }

            // Seed default comments
            val defaultComments = listOf(
                // Comments for Video 1
                CommentEntity(videoId = "vid_1", authorHandle = "@clara_rocks", authorNickname = "Clara Croft", content = "The reactive bounce looks absolutely brilliant! What buffer size are you using? 🔥", likesCount = 455),
                CommentEntity(videoId = "vid_1", authorHandle = "@sound_wizard", authorNickname = "Apex DJ", content = "Kotlin canvas audio waveforms go ridiculously hard.", likesCount = 120),
                CommentEntity(videoId = "vid_1", authorHandle = "@casual_coder", authorNickname = "Alek", content = "Woah, Tik Rox UI is so fast. Love from Seattle!", likesCount = 31),
                
                // Comments for Video 2
                CommentEntity(videoId = "vid_2", authorHandle = "@neon_lucas", authorNickname = "Lucas Webb", content = "Tapping the screen actually updates the gravitational center pointer? That is high level Compose logic!", likesCount = 890, isLiked = true),
                CommentEntity(videoId = "vid_2", authorHandle = "@giga_dev", authorNickname = "Giga", content = "Bro this looks cleaner than regular apps, congrats on the launch!", likesCount = 412),
                CommentEntity(videoId = "vid_2", authorHandle = "@sunset_glider", authorNickname = "Vapor Cruiser", content = "Perfect color palette choice. Matches the cyberpunk mood gracefully.", likesCount = 230),

                // Comments for Video 3
                CommentEntity(videoId = "vid_3", authorHandle = "@astronomy_fan", authorNickname = "Luna", content = "Beautiful simulation. The physics loop flows so smoothly.", likesCount = 104),
                CommentEntity(videoId = "vid_3", authorHandle = "@clara_rocks", authorNickname = "Clara Croft", content = "Stella, can you drop the mathematical formulas for this?", likesCount = 88),

                // Comments for Video 4
                CommentEntity(videoId = "vid_4", authorHandle = "@chill_vibes", authorNickname = "RyZen", content = "Instantly added this synth sound to my favorites. Pure relaxation 🌅🌴", likesCount = 1421),
                CommentEntity(videoId = "vid_4", authorHandle = "@retro_girl", authorNickname = "Maya", content = "This is making me nostalgic for an era I didn't even live in.", likesCount = 948),
                CommentEntity(videoId = "vid_4", authorHandle = "@cosmic_explorer", authorNickname = "Dr. Stella", content = "Incredible depth perspective on that scrolling grid!", likesCount = 328)
            )

            for (comment in defaultComments) {
                videoDao.insertComment(comment)
            }
        }
    }
}
