package ua.nure.kryvko.hikeway.feature.pois

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.util.Locale
import ua.nure.kryvko.hikeway.R
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiComment
import ua.nure.kryvko.hikeway.core.model.PoiPhoto
import ua.nure.kryvko.hikeway.core.model.PoiPhotoUpload
import ua.nure.kryvko.hikeway.core.model.PointOfInterest

@Composable
fun PoiOverviewPopup(
    poi: PointOfInterest,
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onAddToRoute: (() -> Unit)? = null,
    isLoading: Boolean = false,
    isActionInProgress: Boolean = false,
    errorMessage: String? = null,
    onRemoveRating: (() -> Unit)? = null,
    onAddComment: ((String) -> Unit)? = null,
    onUpdateComment: ((Long, String) -> Unit)? = null,
    onDeleteComment: ((Long) -> Unit)? = null,
    onUploadPhoto: ((PoiPhotoUpload) -> Unit)? = null,
    onUpdatePhoto: ((Long, String?) -> Unit)? = null,
    onDeletePhoto: ((Long) -> Unit)? = null,
    onUpdatePoi: ((String, String, GeoPoint) -> Unit)? = null,
    onDeletePoi: (() -> Unit)? = null,
    isAdmin: Boolean = false,
) {
    val context = LocalContext.current
    var uploadCaption by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val upload = uri?.let {
            context.contentResolver.poiPhotoUploadFromUri(
                uri = it,
                caption = uploadCaption,
            )
        }
        if (upload != null) {
            onUploadPhoto?.invoke(upload)
            uploadCaption = ""
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Point of interest", style = MaterialTheme.typography.labelLarge)
                if (isLoading || isActionInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }
            Text(poi.name, style = MaterialTheme.typography.titleMedium)
            poi.ownerDisplayName?.let {
                Text("Added by $it", style = MaterialTheme.typography.bodySmall)
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            PoiPhotoStrip(
                poi = poi,
                onUpdatePhoto = onUpdatePhoto,
                onDeletePhoto = onDeletePhoto,
                isAdmin = isAdmin,
            )
            Text(poi.description, style = MaterialTheme.typography.bodyMedium)
            RatingRow(
                poi = poi,
                onRate = onRate,
                onRemoveRating = onRemoveRating,
            )
            onUploadPhoto?.let {
                UploadPhotoRow(
                    caption = uploadCaption,
                    onCaptionChange = { uploadCaption = it },
                    onPickPhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isActionInProgress,
                )
            }
            if (onUpdatePoi != null || onDeletePoi != null) {
                PoiOwnerActions(
                    poi = poi,
                    onUpdatePoi = onUpdatePoi,
                    onDeletePoi = onDeletePoi,
                    enabled = !isActionInProgress,
                    isAdmin = isAdmin,
                )
            }
            HorizontalDivider()
            CommentSection(
                comments = poi.comments,
                enabled = !isActionInProgress,
                onAddComment = onAddComment,
                onUpdateComment = onUpdateComment,
                onDeleteComment = onDeleteComment,
                isAdmin = isAdmin,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
                onAddToRoute?.let { addToRoute ->
                    Button(onClick = addToRoute) {
                        Text("Add to route")
                    }
                }
            }
        }
    }
}

@Composable
private fun PoiPhotoStrip(
    poi: PointOfInterest,
    onUpdatePhoto: ((Long, String?) -> Unit)?,
    onDeletePhoto: ((Long) -> Unit)?,
    isAdmin: Boolean,
) {
    if (poi.photos.isEmpty() && poi.photoResIds.isEmpty()) {
        Image(
            painter = painterResource(R.drawable.poi_photo_placeholder),
            contentDescription = "${poi.name} photo placeholder",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 160.dp, height = 90.dp),
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(poi.photos) { photo ->
            RemotePhotoCard(
                photo = photo,
                onUpdatePhoto = onUpdatePhoto,
                onDeletePhoto = onDeletePhoto,
                isAdmin = isAdmin,
            )
        }
        items(poi.photoResIds) { photoResId ->
            Image(
                painter = painterResource(photoResId),
                contentDescription = "${poi.name} photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 160.dp, height = 90.dp),
            )
        }
    }
}

@Composable
private fun RemotePhotoCard(
    photo: PoiPhoto,
    onUpdatePhoto: ((Long, String?) -> Unit)?,
    onDeletePhoto: ((Long) -> Unit)?,
    isAdmin: Boolean,
) {
    var caption by remember(photo.id, photo.caption) { mutableStateOf(photo.caption.orEmpty()) }
    Column(
        modifier = Modifier.size(width = 190.dp, height = 180.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AsyncImage(
            model = photo.url,
            contentDescription = photo.caption ?: "Point of interest photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 190.dp, height = 106.dp),
        )
        Text(
            photo.caption ?: "No caption",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
        if (photo.ownedByCurrentUser || isAdmin) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption") },
                    modifier = Modifier.size(width = 110.dp, height = 56.dp),
                    singleLine = true,
                )
                TextButton(onClick = { onUpdatePhoto?.invoke(photo.id, caption) }) {
                    Text("Save")
                }
                TextButton(onClick = { onDeletePhoto?.invoke(photo.id) }) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun RatingRow(
    poi: PointOfInterest,
    onRate: (Int) -> Unit,
    onRemoveRating: (() -> Unit)?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingStars(
            rating = poi.userRating ?: poi.averageRating.toInt().coerceIn(0, 5),
            onRate = onRate,
        )
        Text(
            "Avg ${"%.1f".format(Locale.US, poi.averageRating)} (${poi.ratingCount})",
            style = MaterialTheme.typography.bodySmall,
        )
        if (poi.userRating != null && onRemoveRating != null) {
            TextButton(onClick = onRemoveRating) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun RatingStars(
    rating: Int,
    onRate: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Rate $star stars",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .height(32.dp)
                    .clickable { onRate(star) },
            )
        }
    }
}

@Composable
private fun UploadPhotoRow(
    caption: String,
    onCaptionChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = caption,
            onValueChange = onCaptionChange,
            label = { Text("Photo caption") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
        )
        Button(onClick = onPickPhoto, enabled = enabled) {
            Text("Add photo")
        }
    }
}

@Composable
private fun PoiOwnerActions(
    poi: PointOfInterest,
    onUpdatePoi: ((String, String, GeoPoint) -> Unit)?,
    onDeletePoi: (() -> Unit)?,
    enabled: Boolean,
    isAdmin: Boolean,
) {
    if (!poi.ownedByCurrentUser && !isAdmin) return
    var name by remember(poi.id, poi.name) { mutableStateOf(poi.name) }
    var description by remember(poi.id, poi.description) { mutableStateOf(poi.description) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Manage PoI", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onUpdatePoi?.let {
                Button(
                    onClick = { it(name, description, poi.location) },
                    enabled = enabled,
                ) {
                    Text("Save PoI")
                }
            }
            onDeletePoi?.let {
                TextButton(onClick = it, enabled = enabled) {
                    Text("Delete PoI")
                }
            }
        }
    }
}

@Composable
private fun CommentSection(
    comments: List<PoiComment>,
    enabled: Boolean,
    onAddComment: ((String) -> Unit)?,
    onUpdateComment: ((Long, String) -> Unit)?,
    onDeleteComment: ((Long) -> Unit)?,
    isAdmin: Boolean,
) {
    var commentText by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Comments", style = MaterialTheme.typography.titleSmall)
        onAddComment?.let {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("Add a comment") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
            Button(
                onClick = {
                    it(commentText)
                    commentText = ""
                },
                enabled = enabled && commentText.isNotBlank(),
            ) {
                Text("Submit comment")
            }
        }
        if (comments.isEmpty()) {
            Text("No comments yet.", style = MaterialTheme.typography.bodySmall)
        }
        comments.forEach { comment ->
            CommentRow(
                comment = comment,
                enabled = enabled,
                onUpdateComment = onUpdateComment,
                onDeleteComment = onDeleteComment,
                isAdmin = isAdmin,
            )
        }
    }
}

@Composable
private fun CommentRow(
    comment: PoiComment,
    enabled: Boolean,
    onUpdateComment: ((Long, String) -> Unit)?,
    onDeleteComment: ((Long) -> Unit)?,
    isAdmin: Boolean,
) {
    var text by remember(comment.id, comment.text) { mutableStateOf(comment.text) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(comment.authorDisplayName, style = MaterialTheme.typography.labelMedium)
        if ((comment.ownedByCurrentUser || isAdmin) && onUpdateComment != null) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onUpdateComment(comment.id, text) }, enabled = enabled) {
                    Text("Save")
                }
                onDeleteComment?.let {
                    TextButton(onClick = { it(comment.id) }, enabled = enabled) {
                        Text("Delete")
                    }
                }
            }
        } else {
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun android.content.ContentResolver.poiPhotoUploadFromUri(
    uri: Uri,
    caption: String,
): PoiPhotoUpload? {
    val type = getType(uri) ?: return null
    val bytes = openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PoiPhotoUpload(
        bytes = bytes,
        contentType = type,
        caption = caption.trim().takeIf { it.isNotEmpty() },
    )
}
