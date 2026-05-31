package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CommentEntity
import com.example.data.VideoEntity
import com.example.ui.components.VideoVisualizerContainer
import com.example.ui.theme.*
import com.example.ui.viewmodel.TikRoxTab
import com.example.ui.viewmodel.TikRoxViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TikRoxMainContainer(
    viewModel: TikRoxViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val videos by viewModel.allVideos.collectAsStateWithLifecycle()
    val currentVideoIndex by viewModel.currentVideoIndex.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            TikRoxBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = TikRoxBlack,
        contentWindowInsets = WindowInsets.navigationBars // Satisfies notch/safe-areas
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()) // Feed bleeds to top edge status bars
        ) {
            when (currentTab) {
                TikRoxTab.HOME -> {
                    if (videos.isNotEmpty()) {
                        HomeFeedScreen(
                            viewModel = viewModel,
                            videos = videos,
                            initialPageIndex = currentVideoIndex
                        )
                    } else {
                        // Seeding or loading placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TikRoxAccentPink)
                        }
                    }
                }
                TikRoxTab.SEARCH -> SearchDiscoverScreen(viewModel = viewModel)
                TikRoxTab.ADD -> AddVideoScreen(viewModel = viewModel)
                TikRoxTab.INBOX -> InboxScreen()
                TikRoxTab.PROFILE -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HomeFeedScreen(
    viewModel: TikRoxViewModel,
    videos: List<VideoEntity>,
    initialPageIndex: Int
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, videos.lastIndex),
        pageCount = { videos.size }
    )

    // Synchronize page scrolls back to viewmodel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setVideoIndex(pagerState.currentPage)
    }

    // Capture outside scrolling triggers
    LaunchedEffect(initialPageIndex) {
        if (pagerState.currentPage != initialPageIndex && initialPageIndex in videos.indices) {
            pagerState.scrollToPage(initialPageIndex)
        }
    }

    val activeCommentVideoId by viewModel.activeCommentVideoId.collectAsStateWithLifecycle()
    val comments by viewModel.activeComments.collectAsStateWithLifecycle()
    val filterType by viewModel.feedFilterType.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Vertical Pager containing full-bleed screens
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page in videos.indices) {
                val video = videos[page]
                // Active video is playing only if it matches current page and Comment section is closed
                val isVideoActive = (pagerState.currentPage == page && activeCommentVideoId == null)
                FeedItemView(
                    video = video,
                    isPlaying = isVideoActive,
                    onLikeTapped = { viewModel.toggleLikeVideo(video) },
                    onBookmarkTapped = { viewModel.toggleBookmarkVideo(video) },
                    onCommentTapped = { viewModel.showCommentsForVideo(video.id) }
                )
            }
        }

        // Top Filter Header Tabs styled exactly to Sleek Interface specifications
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.selectTab(com.example.ui.viewmodel.TikRoxTab.SEARCH) }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TikRoxWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.setFeedFilter(1) }
                ) {
                    Text(
                        text = "Following",
                        color = if (filterType == 1) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                        fontWeight = if (filterType == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(28.dp)
                            .background(if (filterType == 1) TikRoxWhite else Color.Transparent)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.setFeedFilter(0) }
                ) {
                    Text(
                        text = "For You",
                        color = if (filterType == 0) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                        fontWeight = if (filterType == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(24.dp)
                            .background(if (filterType == 0) TikRoxWhite else Color.Transparent)
                    )
                }
            }

            IconButton(
                onClick = { /* No-op, visual aesthetic */ }
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = "Live",
                    tint = TikRoxWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Floating heart feedback or overlay comment sheets integration
        AnimatedVisibility(
            visible = activeCommentVideoId != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            activeCommentVideoId?.let { videoId ->
                val currentVideo = videos.find { it.id == videoId }
                BottomCommentSheet(
                    video = currentVideo ?: videos.first(),
                    comments = comments,
                    newText = viewModel.newCommentText.collectAsState().value,
                    onTextChange = { viewModel.setNewCommentText(it) },
                    onSubmit = { viewModel.submitComment() },
                    onClose = { viewModel.showCommentsForVideo(null) },
                    onLikeComment = { viewModel.toggleLikeComment(it) }
                )
            }
        }
    }
}

@Composable
fun FeedItemView(
    video: VideoEntity,
    isPlaying: Boolean,
    onLikeTapped: () -> Unit,
    onBookmarkTapped: () -> Unit,
    onCommentTapped: () -> Unit
) {
    var isLocalPause by remember { mutableStateOf(false) }
    val playState = isPlaying && !isLocalPause

    var isFollowed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TikRoxBlack)
    ) {
        // Full screen live render canvas visualizer
        VideoVisualizerContainer(
            themeName = video.visualTheme,
            isPlaying = playState,
            onDoubleTap = {
                if (!video.isLiked) {
                    onLikeTapped()
                }
            },
            onSingleTap = {
                isLocalPause = !isLocalPause
            },
            modifier = Modifier.fillMaxSize()
        )

        // Backdrop gradient to ensure reader legibility at user descriptions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )

        // 1. Bottom Left Overlay Info Panel (Creator tag with Following badge, description tags, rolling music marquee)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.75f)
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = video.creatorHandle,
                    color = TikRoxWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FOLLOWING",
                        color = TikRoxWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Styled description formatting hashtag colors
            Text(
                text = video.description,
                color = TikRoxWhite,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Music rolling audio bar marquee emulator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music icon",
                    tint = TikRoxWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                ScrollingTextMarquee(
                    text = video.soundTitle,
                    isPlaying = playState
                )
            }
        }

        // 2. Right Side Icons Panel Action commands (Avatar, Like, Comment, Share, Bookmark, Vinyl)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile photo stack with Follow "+" plus animation
            Box(
                modifier = Modifier.padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .background(Color(0xFF404040)),
                    contentAlignment = Alignment.Center
                ) {
                    val firstChar = video.creatorNickname.firstOrNull()?.toString() ?: "U"
                    Text(
                        text = firstChar,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                if (!isFollowed) {
                    Box(
                        modifier = Modifier
                            .offset(y = 8.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(TikRoxAccentPink)
                            .clickable { isFollowed = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Follow icon",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .offset(y = 8.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(TikRoxAccentCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Followed",
                            tint = TikRoxBlack,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Like Action Item button state (with heart pulse scale)
            val heartScale by animateFloatAsState(
                targetValue = if (video.isLiked) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
            )
            FeedActionButton(
                icon = if (video.isLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                tint = if (video.isLiked) TikRoxAccentPink else Color.White,
                count = video.likesCount,
                onClick = onLikeTapped,
                modifier = Modifier.graphicsLayer(scaleX = heartScale, scaleY = heartScale)
            )

            // Comment Action Item button state
            FeedActionButton(
                icon = Icons.Outlined.ModeComment,
                tint = Color.White,
                count = video.commentsCount,
                onClick = onCommentTapped
            )

            // Bookmark Action Item button state
            FeedActionButton(
                icon = if (video.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                tint = if (video.isBookmarked) Color(0xFFFFD700) else Color.White,
                count = video.sharesCount, // display bookmarks
                onClick = onBookmarkTapped
            )

            // Share Action Item button state (no counts)
            FeedActionButton(
                icon = Icons.Outlined.Share,
                tint = Color.White,
                count = video.sharesCount / 2 + 15,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Premium Spinning Vinyl music disc matching Sleek Interface spec gradient
            val discRotation by rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "disc_turning"
            )
            val rotatingDegree = if (playState) discRotation else 0f
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .rotate(rotatingDegree)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF333333), Color(0xFF1A1A1A))
                        )
                    )
                    .border(width = 4.dp, color = Color.White.copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(width = 1.5.dp, color = Color(0xFF404040), shape = CircleShape)
                )
            }
        }

        // Overlay pause visual indicators feedback on central screen
        if (isLocalPause) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused Icon",
                    tint = TikRoxWhite.copy(alpha = 0.8f),
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
fun FeedActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatStatCount(count),
            color = TikRoxWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Custom Horizontal Scrolling Text Marquee
@Composable
fun ScrollingTextMarquee(
    text: String,
    isPlaying: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val textShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -300f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    val currentOffset = if (isPlaying) textShift else 0f

    Box(
        modifier = Modifier
            .width(110.dp)
            .clipToBounds()
    ) {
        Text(
            text = "$text   •   $text   •   ",
            color = TikRoxWhite,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(x = currentOffset.dp)
        )
    }
}

@Composable
fun BottomCommentSheet(
    video: VideoEntity,
    comments: List<CommentEntity>,
    newText: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onLikeComment: (CommentEntity) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = true, onClick = onClose)
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .clickable(enabled = false, onClick = {}) // block double clicks collapsing
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(TikRoxCommentBackground)
                .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        ) {
            // Header panel for Comment Drawer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "${video.commentsCount} Comments",
                    color = TikRoxWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close sheet",
                        tint = TikRoxWhite
                    )
                }
            }

            Divider(color = TikRoxLightGray, thickness = 0.5.dp)

            // Dynamic comments feed lists
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (comments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ModeComment,
                            contentDescription = "Empty",
                            tint = TikRoxMutedGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No comments yet.",
                            color = TikRoxMutedGray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Be the first to share your thoughts!",
                            color = TikRoxMutedGray,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentItemRow(
                                comment = comment,
                                onLikeClicked = { onLikeComment(comment) }
                            )
                        }
                    }
                }
            }

            Divider(color = TikRoxLightGray, thickness = 0.5.dp)

            // Footer comment typed inputs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newText,
                    onValueChange = onTextChange,
                    placeholder = { Text("Add comment...", color = TikRoxMutedGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1D1D20),
                        unfocusedContainerColor = Color(0xFF1D1D20),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TikRoxWhite,
                        unfocusedTextColor = TikRoxWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            onSubmit()
                            keyboardController?.hide()
                        }
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        onSubmit()
                        keyboardController?.hide()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TikRoxAccentPink
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Send", color = TikRoxWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CommentItemRow(
    comment: CommentEntity,
    onLikeClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // User initials avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TikRoxLightGray),
                contentAlignment = Alignment.Center
            ) {
                val startChar = comment.authorNickname.firstOrNull()?.toString() ?: "C"
                Text(
                    text = startChar,
                    color = TikRoxAccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = comment.authorNickname,
                    color = TikRoxMutedGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.content,
                    color = TikRoxWhite,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCommentTimestamp(comment.timestamp),
                    color = TikRoxMutedGray,
                    fontSize = 11.sp
                )
            }
        }

        // Comment like side buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onLikeClicked)
        ) {
            Icon(
                imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                contentDescription = "Like Comment",
                tint = if (comment.isLiked) TikRoxAccentPink else TikRoxMutedGray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (comment.likesCount > 0) {
                Text(
                    text = comment.likesCount.toString(),
                    color = TikRoxMutedGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SearchDiscoverScreen(viewModel: TikRoxViewModel) {
    val videos by viewModel.allVideos.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val filteredList = remember(searchQuery, videos) {
        if (searchQuery.isBlank()) {
            videos
        } else {
            videos.filter {
                it.creatorHandle.contains(searchQuery, ignoreCase = true) ||
                        it.creatorNickname.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Styled Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search creators or trending beats...", color = TikRoxMutedGray) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TikRoxMutedGray
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TikRoxWhite
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TikRoxDarkGray,
                unfocusedContainerColor = TikRoxDarkGray,
                focusedBorderColor = TikRoxAccentCyan,
                unfocusedBorderColor = TikRoxLightGray,
                focusedTextColor = TikRoxWhite,
                unfocusedTextColor = TikRoxWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Grid showing trending hashtag selections if search is active
        if (searchQuery.isEmpty()) {
            Text(
                text = "Trending Hashtags",
                color = TikRoxWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.trendingHashtags.forEach { hashtag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(TikRoxLightGray)
                            .clickable { viewModel.setSearchQuery(hashtag) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = hashtag,
                            color = TikRoxAccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Text(
            text = if (searchQuery.isEmpty()) "Discover Content" else "Search Results (${filteredList.size})",
            color = TikRoxWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Triple Column Grid Layout resembling real TikTok feeds
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Not found",
                        tint = TikRoxMutedGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No results matching \"$searchQuery\"", color = TikRoxGrayText)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList) { video ->
                    SearchVideoGridTile(
                        video = video,
                        onClick = {
                            val idx = videos.indexOfFirst { it.id == video.id }
                            if (idx != -1) {
                                viewModel.setVideoIndex(idx)
                                viewModel.selectTab(TikRoxTab.HOME)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchVideoGridTile(
    video: VideoEntity,
    onClick: () -> Unit
) {
    val gradientColor = when (video.visualTheme) {
        "NEON_BEAT" -> Brush.verticalGradient(listOf(TikRoxAccentPink.copy(alpha = 0.5f), Color(0xFF0F0F0F)))
        "CYBER_PARTICLES" -> Brush.verticalGradient(listOf(Color(0xFF8B00FF).copy(alpha = 0.5f), Color(0xFF0F0F0F)))
        "COSMIC_HELIX" -> Brush.verticalGradient(listOf(TikRoxAccentCyan.copy(alpha = 0.5f), Color(0xFF000511)))
        else -> Brush.verticalGradient(listOf(Color(0xFFFF5E00).copy(alpha = 0.5f), Color(0xFF150C0C)))
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(4.dp))
            .background(gradientColor)
            .clickable(onClick = onClick)
    ) {
        // Draw centered miniature graphics matching visual theme
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = TikRoxWhite,
                modifier = Modifier.size(24.dp)
            )
        }

        // Overlay text with stats at grid bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(4.dp)
        ) {
            Text(
                text = video.creatorHandle,
                color = TikRoxWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = TikRoxWhite,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = formatStatCount(video.likesCount),
                    color = TikRoxWhite,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AddVideoScreen(viewModel: TikRoxViewModel) {
    val description by viewModel.customPostDescription.collectAsStateWithLifecycle()
    val soundTitle by viewModel.customPostSoundTitle.collectAsStateWithLifecycle()
    val chosenTheme by viewModel.customPostTheme.collectAsStateWithLifecycle()

    var activeOptionPanel by remember { mutableStateOf(false) }

    val themes = listOf(
        Pair("NEON_BEAT", "Neon Beats"),
        Pair("CYBER_PARTICLES", "Cyber Storm"),
        Pair("COSMIC_HELIX", "Gravity Star"),
        Pair("VAPOR_DRIVE", "Vapor Sunset")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Tik Rox Recording Studio",
            color = TikRoxWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Animate custom audio visualizer canvases & publish posts!",
            color = TikRoxMutedGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Simulated camera preview background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, TikRoxAccentCyan, RoundedCornerShape(16.dp))
                .background(Color(0xFF090A0F))
        ) {
            // Live background visualizer demonstrating chosen state
            VideoVisualizerContainer(
                themeName = chosenTheme,
                isPlaying = true,
                modifier = Modifier.fillMaxSize()
            )

            // Retro overlays representing Camera frames
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE STUDIO",
                    color = TikRoxAccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Top right camera overlays
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Flip", tint = TikRoxWhite, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Flash", tint = TikRoxWhite, modifier = Modifier.size(16.dp))
                }
            }

            // Central progress recording indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aesthetic: ${themes.find { it.first == chosenTheme }?.second}",
                    color = TikRoxWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Choose visual style block tags
        Text(
            text = "1. Choose Creative Visual Theme",
            color = TikRoxWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.forEach { (themeKey, label) ->
                val isSelected = chosenTheme == themeKey
                val borderCol = if (isSelected) TikRoxAccentPink else Color.Transparent
                val bgCol = if (isSelected) TikRoxLightGray else TikRoxDarkGray

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgCol)
                        .border(1.5.dp, borderCol, RoundedCornerShape(12.dp))
                        .clickable { viewModel.customPostTheme.value = themeKey }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TikRoxWhite else TikRoxMutedGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Caption Form Fields
        Text(
            text = "2. Enter Video Capture Caption",
            color = TikRoxWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = { viewModel.customPostDescription.value = it },
            placeholder = { Text("What did you capture? Add hashtags #synth #lofi #dance here...", color = TikRoxMutedGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TikRoxDarkGray,
                unfocusedContainerColor = TikRoxDarkGray,
                focusedBorderColor = TikRoxAccentPink,
                unfocusedBorderColor = TikRoxLightGray,
                focusedTextColor = TikRoxWhite,
                unfocusedTextColor = TikRoxWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp)),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sound Track selection text
        OutlinedTextField(
            value = soundTitle,
            onValueChange = { viewModel.customPostSoundTitle.value = it },
            placeholder = { Text("SoundTrack Name (eg Original Sound - apex synth)", color = TikRoxMutedGray) },
            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = TikRoxMutedGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TikRoxDarkGray,
                unfocusedContainerColor = TikRoxDarkGray,
                focusedBorderColor = TikRoxAccentPink,
                unfocusedBorderColor = TikRoxLightGray,
                focusedTextColor = TikRoxWhite,
                unfocusedTextColor = TikRoxWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Major Post button trigger
        Button(
            onClick = { viewModel.createCustomVideoPost() },
            colors = ButtonDefaults.buttonColors(
                containerColor = TikRoxAccentPink
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = description.isNotBlank()
        ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = TikRoxWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "POST DYNAMIC VIDEO TO FEED",
                color = TikRoxWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun InboxScreen() {
    val messages = listOf(
        Pair("TikRox Support Bot", "Welcome to Tik Rox! Generate interactive 60fps audio visualizers on the Add section and show your creativity on the feed 🚀🚀"),
        Pair("Lucas Webb", "Yo! Lovin the twin stars gravity stellar rendering. How did you construct polar curves in Jetpack Compose canvas?"),
        Pair("Clara Croft", "Liked your cyberpunk post of dynamic particle streams. Wanna collaborate on synth aesthetics next week?"),
        Pair("Jane_D_Vibe", "That vapor endless neon road highway lo-fi scroll got me chilled for real. Can you upload a synth loop chord session?")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Inbox",
            color = TikRoxWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Direct Messages & System Updates",
            color = TikRoxMutedGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(messages) { (sender, snippet) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TikRoxDarkGray)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Chat initial visual badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TikRoxLightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sender.firstOrNull()?.toString() ?: "U",
                            color = TikRoxAccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sender,
                            color = TikRoxWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = snippet,
                            color = TikRoxGrayText,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: TikRoxViewModel) {
    val videos by viewModel.allVideos.collectAsStateWithLifecycle()
    val personalHandle by viewModel.userProfileHandle.collectAsStateWithLifecycle()
    val personalNickname by viewModel.userProfileNickname.collectAsStateWithLifecycle()
    val followersCount by viewModel.userFollowersCount.collectAsStateWithLifecycle()
    val followingCount by viewModel.userFollowingCount.collectAsStateWithLifecycle()

    // Filter videos created by the user (@me_tikrox / personalized handles)
    val userVideos = remember(videos, personalHandle) {
        videos.filter { it.creatorHandle.equals(personalHandle, ignoreCase = true) }
    }

    // Live Likes tally aggregated from database items
    val liveLikesTotal = remember(userVideos) {
        userVideos.sumOf { it.likesCount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Centered user profile header details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(TikRoxLightGray)
                    .border(2.dp, TikRoxAccentPink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = personalNickname.firstOrNull()?.toString() ?: "T",
                    color = TikRoxAccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = personalNickname,
                color = TikRoxWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                text = personalHandle,
                color = TikRoxMutedGray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Followers stats row bars
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatColumn(count = followingCount, label = "Following")
                Box(modifier = Modifier.size(width = 1.dp, height = 20.dp).background(TikRoxLightGray))
                ProfileStatColumn(count = followersCount, label = "Followers")
                Box(modifier = Modifier.size(width = 1.dp, height = 20.dp).background(TikRoxLightGray))
                ProfileStatColumn(count = liveLikesTotal, label = "Liked Score")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segment header showing Posted Videos
        Text(
            text = "My Posts Grid (${userVideos.size})",
            color = TikRoxWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (userVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TikRoxDarkGray),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = TikRoxMutedGray,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You haven't posted any creations yet.",
                        color = TikRoxMutedGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Go to the Add (+) tab to create a live beat!",
                        color = TikRoxAccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(userVideos, key = { it.id }) { post ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SearchVideoGridTile(
                            video = post,
                            onClick = {
                                val idx = videos.indexOfFirst { it.id == post.id }
                                if (idx != -1) {
                                    viewModel.setVideoIndex(idx)
                                    viewModel.selectTab(TikRoxTab.HOME)
                                }
                            }
                        )

                        // Add direct item Deletion options
                        IconButton(
                            onClick = { viewModel.deletePost(post.id) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = TikRoxAccentPink,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatColumn(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatStatCount(count),
            color = TikRoxWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = TikRoxMutedGray,
            fontSize = 12.sp
        )
    }
}

// Global Bottom Navigation Bar aligned to Material 3 standard specs
@Composable
fun TikRoxBottomNavigationBar(
    currentTab: TikRoxTab,
    onTabSelected: (TikRoxTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Thin elegant top line matching border-t border-white/10
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            val isHome = currentTab == TikRoxTab.HOME
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(TikRoxTab.HOME) }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (isHome) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Home",
                    color = if (isHome) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Discover/Friends Tab
            val isSearch = currentTab == TikRoxTab.SEARCH
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(TikRoxTab.SEARCH) }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Discover",
                    tint = if (isSearch) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Friends",
                    color = if (isSearch) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Central Add / Create button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(TikRoxTab.ADD) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 28.dp)
                ) {
                    // Left cyan highlight backing
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TikRoxAccentCyan)
                    )
                    // Right magenta highlight backing
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TikRoxAccentPink)
                    )
                    // Central white button
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TikRoxWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Post video",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Inbox Tab
            val isInbox = currentTab == TikRoxTab.INBOX
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(TikRoxTab.INBOX) }
            ) {
                Icon(
                    imageVector = Icons.Default.Mail,
                    contentDescription = "Inbox",
                    tint = if (isInbox) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Inbox",
                    color = if (isInbox) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Profile Tab
            val isProfile = currentTab == TikRoxTab.PROFILE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(TikRoxTab.PROFILE) }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = if (isProfile) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Profile",
                    color = if (isProfile) TikRoxWhite else TikRoxWhite.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Utility helper formats counts to (e.g. 1.2K, 38.9M)
fun formatStatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

// Formats comments dates nicely
fun formatCommentTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "just now"
    }
}
