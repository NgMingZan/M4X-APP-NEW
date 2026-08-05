package com.m4xtheme.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun ThemeCommunitySection(
    api: SupabaseApi,
    session: Session,
    themeId: String,
    purchased: Boolean,
    canModerate: Boolean,
    initialRating: Double,
    onRatingChanged: (Double) -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var reviews by remember(themeId) {
        mutableStateOf<List<ThemeCommunityReview>>(emptyList())
    }
    var summary by remember(themeId) {
        mutableStateOf(
            ThemeCommunitySummary(
                averageRating = initialRating,
                reviewCount = 0
            )
        )
    }
    var loading by remember(themeId) {
        mutableStateOf(true)
    }
    var saving by remember(themeId) {
        mutableStateOf(false)
    }
    var selectedStars by remember(themeId) {
        mutableIntStateOf(5)
    }
    var comment by remember(themeId) {
        mutableStateOf("")
    }

    val myReview = reviews.firstOrNull { it.mine }

    suspend fun reload() {
        loading = true
        api.themeCommunitySummary(session, themeId)
            .onSuccess {
                summary = it
                onRatingChanged(it.averageRating)
            }
            .onFailure {
                onMessage(
                    it.message ?: "Không tải được tổng hợp đánh giá"
                )
            }

        api.themeCommunityReviews(session, themeId)
            .onSuccess {
                reviews = it
            }
            .onFailure {
                onMessage(
                    it.message ?: "Không tải được bình luận"
                )
            }
        loading = false
    }

    LaunchedEffect(themeId) {
        reload()
    }

    LaunchedEffect(myReview?.id, myReview?.updatedAt) {
        if (myReview != null) {
            selectedStars = myReview.stars
            comment = myReview.comment
        } else {
            selectedStars = 5
            comment = ""
        }
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF141414)
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.RateReview,
                    null,
                    tint = Color(0xFFFFC857)
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Đánh giá & bình luận",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (summary.reviewCount > 0) {
                            "${formatRating(summary.averageRating)} ★ • ${summary.reviewCount} lượt đánh giá"
                        } else {
                            "Chưa có đánh giá"
                        },
                        color = Color.White.copy(alpha = .66f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (loading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFFC857)
                    )
                }
            }

            RatingStars(
                stars = summary.averageRating,
                enabled = false,
                onSelect = {}
            )

            if (purchased) {
                Surface(
                    color = Color.White.copy(alpha = .055f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (myReview == null) {
                                "Đánh giá theme này"
                            } else {
                                "Sửa đánh giá của bạn"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )

                        RatingStars(
                            stars = selectedStars.toDouble(),
                            enabled = !saving,
                            onSelect = {
                                selectedStars = it
                            }
                        )

                        OutlinedTextField(
                            value = comment,
                            onValueChange = {
                                comment = it.take(600)
                            },
                            label = {
                                Text("Bình luận (${comment.length}/600)")
                            },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !saving
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    saving = true
                                    api.submitThemeCommunityReview(
                                        session = session,
                                        themeId = themeId,
                                        stars = selectedStars,
                                        comment = comment
                                    ).onSuccess {
                                        summary = it
                                        onRatingChanged(
                                            it.averageRating
                                        )
                                        onMessage(
                                            if (myReview == null) {
                                                "Đã gửi đánh giá"
                                            } else {
                                                "Đã cập nhật đánh giá"
                                            }
                                        )
                                        reload()
                                    }.onFailure {
                                        onMessage(
                                            it.message
                                                ?: "Không thể gửi đánh giá"
                                        )
                                    }
                                    saving = false
                                }
                            },
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC857),
                                contentColor = Color.Black
                            )
                        ) {
                            if (saving) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            } else {
                                Icon(
                                    Icons.Default.Send,
                                    null,
                                    Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(7.dp))
                            Text(
                                if (myReview == null) {
                                    "Gửi đánh giá"
                                } else {
                                    "Cập nhật đánh giá"
                                },
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = Color(0xFF2A2418),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Mua hoặc nhận theme để được đánh giá và bình luận.",
                        color = Color(0xFFFFD780),
                        modifier = Modifier.padding(13.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "Bình luận gần đây",
                color = Color.White,
                fontWeight = FontWeight.Black
            )

            when {
                loading && reviews.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                reviews.isEmpty() -> {
                    Text(
                        "Chưa có bình luận nào.",
                        color = Color.White.copy(alpha = .58f)
                    )
                }

                else -> {
                    reviews.take(30).forEach { review ->
                        ThemeCommentCard(
                            review = review,
                            canModerate = canModerate,
                            moderating = saving,
                            onModerate = {
                                scope.launch {
                                    saving = true
                                    api.moderateThemeCommunityReview(
                                        session = session,
                                        reviewId = review.id,
                                        hidden = !review.hidden
                                    ).onSuccess {
                                        onMessage(
                                            if (review.hidden) {
                                                "Đã hiện lại bình luận"
                                            } else {
                                                "Đã ẩn bình luận"
                                            }
                                        )
                                        reload()
                                    }.onFailure {
                                        onMessage(
                                            it.message
                                                ?: "Không thể kiểm duyệt bình luận"
                                        )
                                    }
                                    saving = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingStars(
    stars: Double,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..5).forEach { value ->
            IconButton(
                onClick = { onSelect(value) },
                enabled = enabled,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (value <= stars) {
                        Icons.Default.Star
                    } else {
                        Icons.Default.StarBorder
                    },
                    contentDescription = "$value sao",
                    tint = if (value <= stars) {
                        Color(0xFFFFC857)
                    } else {
                        Color.White.copy(alpha = .35f)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeCommentCard(
    review: ThemeCommunityReview,
    canModerate: Boolean,
    moderating: Boolean,
    onModerate: () -> Unit
) {
    Surface(
        color = if (review.hidden) {
            Color(0xFF311C1C)
        } else {
            Color.White.copy(alpha = .045f)
        },
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (review.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = review.avatarUrl,
                        contentDescription = review.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        color = Color(0xFF2D2D2D),
                        shape = CircleShape
                    ) {
                        Box(
                            Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                review.displayName
                                    .take(1)
                                    .uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            review.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (review.mine) {
                            Text(
                                " • Bạn",
                                color = Color(0xFF62DDB0),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        compactDate(review.updatedAt),
                        color = Color.White.copy(alpha = .45f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (review.hidden) {
                    Text(
                        "Đã ẩn",
                        color = Color(0xFFFF8C86),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                (1..5).forEach { value ->
                    Icon(
                        imageVector = if (
                            value <= review.stars
                        ) {
                            Icons.Default.Star
                        } else {
                            Icons.Default.StarBorder
                        },
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (value <= review.stars) {
                            Color(0xFFFFC857)
                        } else {
                            Color.White.copy(alpha = .25f)
                        }
                    )
                }
            }

            if (review.comment.isNotBlank()) {
                Text(
                    review.comment,
                    color = Color.White.copy(
                        alpha = if (review.hidden) .55f else .88f
                    )
                )
            } else {
                Text(
                    "Người dùng chỉ đánh giá sao.",
                    color = Color.White.copy(alpha = .42f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (canModerate) {
                TextButton(
                    onClick = onModerate,
                    enabled = !moderating,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = if (review.hidden) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (review.hidden) {
                            "Hiện lại"
                        } else {
                            "Ẩn bình luận"
                        }
                    )
                }
            }
        }
    }
}

private fun compactDate(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace("T", " ")
        .replace("Z", "")
        .take(16)
}

private fun formatRating(value: Double): String =
    java.lang.String.format(
        java.util.Locale.US,
        "%.1f",
        value
    )
