package ua.nure.kryvko.hikeway.data.sync

import ua.nure.kryvko.hikeway.domain.auth.CurrentUserProvider

class SyncTrigger(
    private val coordinator: SyncCoordinator,
    private val currentUserProvider: CurrentUserProvider,
) {
    suspend operator fun invoke() {
        val userId = currentUserProvider.currentUserId.value ?: return
        coordinator.synchronize(userId)
    }
}
