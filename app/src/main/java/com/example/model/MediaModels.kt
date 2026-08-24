package com.example.model

enum class SortField(val displayName: String) {
    DATE("Fecha"),
    SIZE("Tamaño"),
    NAME("Nombre")
}

enum class SortDirection(val displayName: String) {
    DESCENDING("Mayor / Reciente"),
    ASCENDING("Menor / Antiguo")
}

enum class MediaTypeFilter(val displayName: String) {
    ALL("Todos"),
    PHOTOS("Fotos"),
    VIDEOS("Videos"),
    LARGE_ONLY("Pesados (>15MB)")
}

enum class NavigationTab(val title: String) {
    SWIPE("Limpiar"),
    FOLDERS("Carpetas"),
    TRASH("Papelera"),
    STATS("Resumen")
}

data class MediaItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val bucketName: String = "Galería",
    val bucketId: String = "0",
    val width: Int = 0,
    val height: Int = 0
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }

    val formattedDuration: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

data class MediaFolder(
    val id: String,
    val name: String,
    val itemCount: Int,
    val totalSizeBytes: Long,
    val latestDateMs: Long,
    val coverUri: String?
) {
    val formattedSize: String
        get() {
            val kb = totalSizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}

sealed class SwipeDecision {
    object Keep : SwipeDecision()
    object Delete : SwipeDecision()
}

data class SwipeAction(
    val item: MediaItem,
    val decision: SwipeDecision,
    val timestamp: Long = System.currentTimeMillis()
)
