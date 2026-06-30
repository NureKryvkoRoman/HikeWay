package ua.nure.kryvko.hikeway.feature.pois

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import ua.nure.kryvko.hikeway.core.model.PoiComment

@Composable
fun CommentSection(
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
