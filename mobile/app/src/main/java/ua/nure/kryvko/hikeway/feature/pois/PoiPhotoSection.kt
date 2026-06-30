package ua.nure.kryvko.hikeway.feature.pois

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ua.nure.kryvko.hikeway.R
import ua.nure.kryvko.hikeway.core.model.PoiPhoto
import ua.nure.kryvko.hikeway.core.model.PointOfInterest

@Composable
fun PoiPhotoStrip(
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
