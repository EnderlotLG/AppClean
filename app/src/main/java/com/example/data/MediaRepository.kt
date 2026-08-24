package com.example.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.MediaFolder
import com.example.model.MediaItem
import com.example.model.MediaTypeFilter
import com.example.model.SortDirection
import com.example.model.SortField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository {

    suspend fun queryDeviceMedia(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val contentResolver: ContentResolver = context.contentResolver

        // 1. Query Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Foto_${id}"
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000L
                    val mime = cursor.getString(mimeColumn) ?: "image/jpeg"
                    val bucketName = if (bucketNameColumn != -1) cursor.getString(bucketNameColumn) ?: "Cámara" else "Cámara"
                    val bucketId = if (bucketIdColumn != -1) cursor.getString(bucketIdColumn) ?: "camera" else "camera"
                    val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0

                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri.toString(),
                            displayName = name,
                            sizeBytes = size,
                            dateAddedMs = dateAdded,
                            mimeType = mime,
                            isVideo = false,
                            bucketName = bucketName,
                            bucketId = bucketId,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Query Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val bucketNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val bucketIdColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_${id}"
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000L
                    val mime = cursor.getString(mimeColumn) ?: "video/mp4"
                    val bucketName = if (bucketNameColumn != -1) cursor.getString(bucketNameColumn) ?: "Videos" else "Videos"
                    val bucketId = if (bucketIdColumn != -1) cursor.getString(bucketIdColumn) ?: "videos" else "videos"
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0

                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri.toString(),
                            displayName = name,
                            sizeBytes = size,
                            dateAddedMs = dateAdded,
                            mimeType = mime,
                            isVideo = true,
                            durationMs = duration,
                            bucketName = bucketName,
                            bucketId = bucketId,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If device has no user photos yet (e.g. fresh emulator/test instance),
        // provide high quality demo gallery items so the user can test immediately
        if (mediaList.isEmpty()) {
            mediaList.addAll(getDemoMediaItems())
        }

        mediaList
    }

    fun extractFolders(mediaList: List<MediaItem>): List<MediaFolder> {
        val grouped = mediaList.groupBy { it.bucketId }
        return grouped.map { (bucketId, items) ->
            val bucketName = items.firstOrNull()?.bucketName ?: "Carpeta"
            val totalSize = items.sumOf { it.sizeBytes }
            val latestDate = items.maxOfOrNull { it.dateAddedMs } ?: 0L
            val cover = items.firstOrNull()?.uri
            MediaFolder(
                id = bucketId,
                name = bucketName,
                itemCount = items.size,
                totalSizeBytes = totalSize,
                latestDateMs = latestDate,
                coverUri = cover
            )
        }
    }

    fun filterAndSort(
        items: List<MediaItem>,
        folderId: String?,
        sortField: SortField,
        sortDirection: SortDirection,
        typeFilter: MediaTypeFilter
    ): List<MediaItem> {
        var filtered = if (folderId != null) {
            items.filter { it.bucketId == folderId }
        } else {
            items
        }

        filtered = when (typeFilter) {
            MediaTypeFilter.ALL -> filtered
            MediaTypeFilter.PHOTOS -> filtered.filter { !it.isVideo }
            MediaTypeFilter.VIDEOS -> filtered.filter { it.isVideo }
            MediaTypeFilter.LARGE_ONLY -> filtered.filter { it.sizeBytes >= 15 * 1024 * 1024L }
        }

        return when (sortField) {
            SortField.DATE -> {
                if (sortDirection == SortDirection.DESCENDING) {
                    filtered.sortedByDescending { it.dateAddedMs }
                } else {
                    filtered.sortedBy { it.dateAddedMs }
                }
            }
            SortField.SIZE -> {
                if (sortDirection == SortDirection.DESCENDING) {
                    filtered.sortedByDescending { it.sizeBytes }
                } else {
                    filtered.sortedBy { it.sizeBytes }
                }
            }
            SortField.NAME -> {
                if (sortDirection == SortDirection.DESCENDING) {
                    filtered.sortedByDescending { it.displayName.lowercase() }
                } else {
                    filtered.sortedBy { it.displayName.lowercase() }
                }
            }
        }
    }

    suspend fun createDeletePendingIntent(
        context: Context,
        items: List<MediaItem>
    ): PendingIntent? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = items.mapNotNull {
                try {
                    Uri.parse(it.uri)
                } catch (e: Exception) {
                    null
                }
            }.filter { it.scheme == "content" }

            if (uris.isNotEmpty()) {
                try {
                    MediaStore.createDeleteRequest(context.contentResolver, uris)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        } else {
            // Direct deletion for API < 30 or mock items
            null
        }
    }

    suspend fun deleteDirectly(context: Context, items: List<MediaItem>): Boolean = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var successCount = 0
        for (item in items) {
            try {
                val uri = Uri.parse(item.uri)
                if (uri.scheme == "content") {
                    val rows = resolver.delete(uri, null, null)
                    if (rows > 0) successCount++
                } else {
                    // Demo item
                    successCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        successCount > 0 || items.isEmpty()
    }

    private fun getDemoMediaItems(): List<MediaItem> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        return listOf(
            MediaItem(
                id = 1001L,
                uri = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&q=80",
                displayName = "IMG_2026_Montaña_Paisaje.jpg",
                sizeBytes = 18_450_000L,
                dateAddedMs = now - (dayMs * 1),
                mimeType = "image/jpeg",
                isVideo = false,
                bucketName = "Cámara",
                bucketId = "camera_1",
                width = 3840,
                height = 2160
            ),
            MediaItem(
                id = 1002L,
                uri = "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=800&q=80",
                displayName = "Gatito_Jugando_Video.mp4",
                sizeBytes = 85_200_000L,
                dateAddedMs = now - (dayMs * 2),
                mimeType = "video/mp4",
                isVideo = true,
                durationMs = 45000L,
                bucketName = "Videos",
                bucketId = "videos_1",
                width = 1920,
                height = 1080
            ),
            MediaItem(
                id = 1003L,
                uri = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&q=80",
                displayName = "DSC_0049_Niebla_Bosque.jpg",
                sizeBytes = 24_600_000L,
                dateAddedMs = now - (dayMs * 3),
                mimeType = "image/jpeg",
                isVideo = false,
                bucketName = "Cámara",
                bucketId = "camera_1",
                width = 4000,
                height = 3000
            ),
            MediaItem(
                id = 1004L,
                uri = "https://images.unsplash.com/photo-1517849845537-4d257902454a?w=800&q=80",
                displayName = "Screenshot_2026_WhatsApp_Meme.png",
                sizeBytes = 3_400_000L,
                dateAddedMs = now - (dayMs * 4),
                mimeType = "image/png",
                isVideo = false,
                bucketName = "Capturas",
                bucketId = "screenshots_1",
                width = 1080,
                height = 2400
            ),
            MediaItem(
                id = 1005L,
                uri = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800&q=80",
                displayName = "VID_Vacaciones_Playa.mp4",
                sizeBytes = 142_800_000L,
                dateAddedMs = now - (dayMs * 6),
                mimeType = "video/mp4",
                isVideo = true,
                durationMs = 124000L,
                bucketName = "Videos",
                bucketId = "videos_1",
                width = 1920,
                height = 1080
            ),
            MediaItem(
                id = 1006L,
                uri = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
                displayName = "Aurora_Boreal_Noche.jpg",
                sizeBytes = 32_100_000L,
                dateAddedMs = now - (dayMs * 8),
                mimeType = "image/jpeg",
                isVideo = false,
                bucketName = "Descargas",
                bucketId = "downloads_1",
                width = 3840,
                height = 2160
            ),
            MediaItem(
                id = 1007L,
                uri = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
                displayName = "IMG_PuestaDeSol_Oceano.jpg",
                sizeBytes = 16_900_000L,
                dateAddedMs = now - (dayMs * 10),
                mimeType = "image/jpeg",
                isVideo = false,
                bucketName = "Cámara",
                bucketId = "camera_1",
                width = 4032,
                height = 3024
            ),
            MediaItem(
                id = 1008L,
                uri = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=800&q=80",
                displayName = "Screenshot_Factura_Recibo.png",
                sizeBytes = 2_150_000L,
                dateAddedMs = now - (dayMs * 14),
                mimeType = "image/png",
                isVideo = false,
                bucketName = "Capturas",
                bucketId = "screenshots_1",
                width = 1080,
                height = 2340
            ),
            MediaItem(
                id = 1009L,
                uri = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=800&q=80",
                displayName = "Fiesta_Cumpleanos_Video.mp4",
                sizeBytes = 210_500_000L,
                dateAddedMs = now - (dayMs * 18),
                mimeType = "video/mp4",
                isVideo = true,
                durationMs = 190000L,
                bucketName = "WhatsApp Video",
                bucketId = "wa_videos_1",
                width = 1280,
                height = 720
            ),
            MediaItem(
                id = 1010L,
                uri = "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=800&q=80",
                displayName = "IMG_Colinas_Primavera.jpg",
                sizeBytes = 19_800_000L,
                dateAddedMs = now - (dayMs * 25),
                mimeType = "image/jpeg",
                isVideo = false,
                bucketName = "Cámara",
                bucketId = "camera_1",
                width = 3840,
                height = 2160
            )
        )
    }
}
