package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CommentEntity
import com.example.data.VideoEntity
import com.example.data.VideoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class TikRoxTab {
    HOME, SEARCH, ADD, INBOX, PROFILE
}

@OptIn(ExperimentalCoroutinesApi::class)
class TikRoxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    
    // Bottom Tab State
    private val _currentTab = MutableStateFlow(TikRoxTab.HOME)
    val currentTab: StateFlow<TikRoxTab> = _currentTab.asStateFlow()

    // Feed Video Index
    private val _currentVideoIndex = MutableStateFlow(0)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex.asStateFlow()

    // Search query states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Home feed filter: "For You" (0) and "Following" (1)
    private val _feedFilterType = MutableStateFlow(0) // 0 = For You, 1 = Following
    val feedFilterType: StateFlow<Int> = _feedFilterType.asStateFlow()

    // Active Comments Section
    private val _activeCommentVideoId = MutableStateFlow<String?>(null)
    val activeCommentVideoId: StateFlow<String?> = _activeCommentVideoId.asStateFlow()

    // New Comment Input Text state
    private val _newCommentText = MutableStateFlow("")
    val newCommentText: StateFlow<String> = _newCommentText.asStateFlow()

    // Reactive Videos Flow
    val allVideos: StateFlow<List<VideoEntity>>

    // User's Customizable Profile Meta values
    val userProfileHandle = MutableStateFlow("@me_tikrox")
    val userProfileNickname = MutableStateFlow("Tik Rox Creator")
    val userFollowersCount = MutableStateFlow(2480)
    val userFollowingCount = MutableStateFlow(192)

    // Direct Comments Flow mapping based on active video
    val activeComments: StateFlow<List<CommentEntity>>

    // Search suggestions & details
    val trendingHashtags = listOf("#mathart", "#synth", "#kotlin", "#cyberpunk", "#space", "#relax", "#dance", "#indiegamedev")

    // Camera/Recording Studio overlay states
    val activeFilter = MutableStateFlow("Normal")
    val recordingProgress = MutableStateFlow(0f)
    val isRecording = MutableStateFlow(false)

    // Form inputs for Custom Video creations
    val customPostDescription = MutableStateFlow("")
    val customPostSoundTitle = MutableStateFlow("Original Sound - me_tikrox")
    val customPostTheme = MutableStateFlow("NEON_BEAT") // NEON_BEAT, CYBER_PARTICLES, COSMIC_HELIX, VAPOR_DRIVE

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VideoRepository(database.videoDao())

        // Feed list
        allVideos = repository.allVideos
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Listen for comment triggers
        activeComments = _activeCommentVideoId
            .flatMapLatest { videoId ->
                if (videoId == null) {
                    flowOf(emptyList())
                } else {
                    repository.getCommentsForVideo(videoId)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed details on initial boot
        viewModelScope.launch {
            repository.prepSeedsIfEmpty()
        }
    }

    fun selectTab(tab: TikRoxTab) {
        _currentTab.value = tab
    }

    fun setVideoIndex(index: Int) {
        _currentVideoIndex.value = index
    }

    fun setFeedFilter(filterType: Int) {
        _feedFilterType.value = filterType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLikeVideo(video: VideoEntity) {
        viewModelScope.launch {
            val updated = video.copy(
                isLiked = !video.isLiked,
                likesCount = if (video.isLiked) video.likesCount - 1 else video.likesCount + 1
            )
            repository.updateVideo(updated)
        }
    }

    fun toggleBookmarkVideo(video: VideoEntity) {
        viewModelScope.launch {
            val updated = video.copy(
                isBookmarked = !video.isBookmarked,
                sharesCount = if (video.isBookmarked) video.sharesCount - 1 else video.sharesCount + 1
            )
            repository.updateVideo(updated)
        }
    }

    fun showCommentsForVideo(videoId: String?) {
        _activeCommentVideoId.value = videoId
        _newCommentText.value = ""
    }

    fun setNewCommentText(text: String) {
        _newCommentText.value = text
    }

    fun submitComment() {
        val videoId = _activeCommentVideoId.value ?: return
        val contentText = _newCommentText.value.trim()
        if (contentText.isEmpty()) return

        viewModelScope.launch {
            // Find current video to increment comment counter
            val videoList = allVideos.value
            val targetVideo = videoList.find { it.id == videoId }
            if (targetVideo != null) {
                repository.updateVideo(targetVideo.copy(commentsCount = targetVideo.commentsCount + 1))
            }

            // Save new comment
            val newComment = CommentEntity(
                videoId = videoId,
                authorHandle = userProfileHandle.value,
                authorNickname = userProfileNickname.value,
                content = contentText,
                likesCount = 0,
                isLiked = false
            )
            repository.insertComment(newComment)
            _newCommentText.value = "" // Reset text field
        }
    }

    fun toggleLikeComment(comment: CommentEntity) {
        viewModelScope.launch {
            val updated = comment.copy(
                isLiked = !comment.isLiked,
                likesCount = if (comment.isLiked) comment.likesCount - 1 else comment.likesCount + 1
            )
            repository.updateComment(updated)
        }
    }

    // Submit custom posted video creations
    fun createCustomVideoPost() {
        val desc = customPostDescription.value.trim()
        if (desc.isEmpty()) return

        val finalDesc = if (desc.contains("#")) desc else "$desc #tikrox #creative"
        val sound = customPostSoundTitle.value.ifBlank { "Original Sound - me_tikrox" }

        viewModelScope.launch {
            val newVideo = VideoEntity(
                id = "user_post_${UUID.randomUUID().toString().take(6)}",
                creatorHandle = userProfileHandle.value,
                creatorNickname = userProfileNickname.value,
                description = finalDesc,
                soundTitle = sound,
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                isLiked = false,
                isBookmarked = false,
                visualTheme = customPostTheme.value
            )

            repository.insertVideo(newVideo)
            
            // Increment personal creator upload totals / states (visual only)
            userFollowersCount.value = userFollowersCount.value + 12 // gain some followers for posting!

            // Reset form details
            customPostDescription.value = ""
            customPostSoundTitle.value = "Original Sound - me_tikrox"
            
            // Switch tab back to feed
            _currentVideoIndex.value = 0
            _currentTab.value = TikRoxTab.HOME
        }
    }

    fun deletePost(videoId: String) {
        viewModelScope.launch {
            repository.deleteVideoById(videoId)
        }
    }
}
