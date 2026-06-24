package ua.nure.kryvko.hikeway.data.sync

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_conflicts",
    indices = [Index(value = ["ownerUserId", "resourceType", "clientId"], unique = true)],
)
data class SyncConflictEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUserId: String,
    val resourceType: String,
    val clientId: String,
    val submittedBaseVersion: Long,
    val serverVersion: Long,
    val serverRecordJson: String,
    val createdAtEpochMillis: Long,
)
