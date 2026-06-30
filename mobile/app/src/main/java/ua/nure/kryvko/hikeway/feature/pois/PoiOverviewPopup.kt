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
