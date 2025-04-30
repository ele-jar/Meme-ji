package com.example.memesji.repository

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.memesji.R
import com.example.memesji.data.CategoryItem
import com.example.memesji.data.Meme
import com.example.memesji.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import java.io.*
import java.util.Locale
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MemeRepository(
    private val apiService: ApiService,
    private val context: Context,
    private val okHttpClient: OkHttpClient
    ) {

    private var allMemesCache: List<Meme>? = null
    private val categoryImageTagSuffix = "-categorie"
    companion object {
        const val SENSITIVE_TAG = "18+"
    }

    data class ShareableData(val uri: Uri, val mimeType: String?)


    fun isCacheEmpty(): Boolean {
        return allMemesCache == null
    }

    suspend fun getMemes(forceRefresh: Boolean = false): Result<List<Meme>> {
         return try {

            if (forceRefresh) {
                 allMemesCache = null
                 Log.d("MemeRepository", "Cache cleared due to forceRefresh=true")
            }

            if (allMemesCache != null) {
                Log.d("MemeRepository", "Returning cached memes.")
                return Result.success(allMemesCache!!)
            }

            Log.d("MemeRepository", "Fetching memes from network (forceRefresh=$forceRefresh).")
            val response = apiService.getMemes()
            if (response.isSuccessful && response.body() != null) {
                val memes = response.body()!!
                allMemesCache = memes
                Log.d("MemeRepository", "Network fetch successful, cache updated with ${memes.size} items.")
                Result.success(memes)
            } else {
                Log.e("MemeRepository", "Error fetching memes: ${response.code()} ${response.message()}")
                Result.failure(Exception("Error fetching memes: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("MemeRepository", "Network error fetching memes", e)
            Result.failure(e)
        }
    }

    fun getCategoriesWithImages(): List<CategoryItem> {
        val cache = allMemesCache ?: return emptyList()

        val distinctCategories = cache.flatMap { it.tags }
            .filter {
                !it.endsWith(categoryImageTagSuffix, ignoreCase = true) &&
                !it.equals(SENSITIVE_TAG, ignoreCase = true)
            }
            .distinctBy { it.lowercase() }
            .sorted()

        Log.d("MemeRepository", "Distinct non-sensitive categories found: $distinctCategories")

        return distinctCategories.map { categoryName ->
            val targetTag = "$categoryName$categoryImageTagSuffix"

            val representativeMeme = cache.find { meme ->
                val hasBaseTag = meme.tags.any { it.equals(categoryName, ignoreCase = true) }
                val hasSpecificTag = meme.tags.any { it.equals(targetTag, ignoreCase = true) }
                val isSensitive = meme.tags.any { it.equals(SENSITIVE_TAG, ignoreCase = true)}

                hasBaseTag && hasSpecificTag && !isSensitive
            }

            if (representativeMeme != null) {
                 Log.d("MemeRepository", "Found image for category '$categoryName': ${representativeMeme.url}")
            } else {
                 val fallbackMeme = cache.find { meme ->
                     val hasBaseTag = meme.tags.any { it.equals(categoryName, ignoreCase = true) }
                     val isSensitive = meme.tags.any { it.equals(SENSITIVE_TAG, ignoreCase = true)}
                     hasBaseTag && !isSensitive
                 }
                 if (fallbackMeme != null) {
                      Log.w("MemeRepository", "No specific image found for category '$categoryName', using fallback: ${fallbackMeme.url}")
                 } else {
                      Log.w("MemeRepository", "No image found for category '$categoryName' (looked for tags '$categoryName' and '$targetTag', excluding sensitive)")
                 }
                 return@map CategoryItem(name = categoryName, imageUrl = fallbackMeme?.url)
            }

            CategoryItem(name = categoryName, imageUrl = representativeMeme.url)
        }
    }

    fun getMemesByCategory(category: String): List<Meme> {
        return allMemesCache?.filter { meme -> meme.tags.any { it.equals(category, ignoreCase = true) } } ?: emptyList()
    }

     suspend fun downloadImageToCache(imageUrl: String): Result<File> = withContext(Dispatchers.IO) {
         try {
             val file = Glide.with(context)
                 .asFile()
                 .load(imageUrl)
                 .diskCacheStrategy(DiskCacheStrategy.DATA)
                 .submit()
                 .get()

             if (file != null && file.exists()) {
                 Log.d("MemeRepository", "Image downloaded via Glide cache: ${file.path}")
                 Result.success(file)
             } else {
                 Log.e("MemeRepository", "Failed to download image using Glide cache: File is null or doesn't exist.")
                 Result.failure(IOException("Failed to download image using Glide"))
             }
         } catch (e: Exception) {
             Log.e("MemeRepository", "Error downloading image to cache", e)
             Result.failure(e)
         }
     }

     suspend fun getShareableUri(meme: Meme, fileFromCache: File): ShareableData? = withContext(Dispatchers.Main) {
         try {
             val cacheDir = File(context.cacheDir, "share_cache")
             cacheDir.mkdirs()

             val urlExtension = meme.url.substringAfterLast('.', "").lowercase(Locale.ROOT)
             val cacheExtension = fileFromCache.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
             val extension = if (urlExtension.isNotEmpty() && urlExtension.length <= 4) urlExtension else cacheExtension

             val safeBaseName = makeFilenameSafe(meme.name)
             val targetFileName = if (extension.isNotEmpty()) "$safeBaseName.$extension" else safeBaseName
             val targetFile = File(cacheDir, targetFileName)

             Log.d("MemeRepository", "Sharing Prep - URL: ${meme.url}")
             Log.d("MemeRepository", "Sharing Prep - Cache File: ${fileFromCache.path}")
             Log.d("MemeRepository", "Sharing Prep - Target File: ${targetFile.path}")
             Log.d("MemeRepository", "Sharing Prep - Determined Extension: '$extension'")

             withContext(Dispatchers.IO) {
                 if (!targetFile.exists()) {
                     targetFile.createNewFile()
                 }
             }

             val uri = FileProvider.getUriForFile(
                 context,
                 "${context.packageName}.provider",
                 targetFile
             )
             Log.d("MemeRepository", "Sharing Prep - Generated URI: $uri")

             withContext(Dispatchers.IO) {
                 fileFromCache.inputStream().use { input ->
                     targetFile.outputStream().use { output ->
                         input.copyTo(output)
                     }
                 }
                 Log.d("MemeRepository", "Sharing Prep - Copied content from cache to target file")
             }

             val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.also {
                 Log.d("MemeRepository", "Sharing Prep - MimeTypeMap result: $it")
             } ?: run {
                 Log.w("MemeRepository", "Sharing Prep - MimeTypeMap failed for extension '$extension', defaulting to image/*")
                 "image/*"
             }

             ShareableData(uri, mimeType)

         } catch (e: Exception) {
             Log.e("MemeRepository", "Error preparing shareable URI", e)
             null
         }
     }


     private fun makeFilenameSafe(input: String): String {
         val pattern = Pattern.compile("[^a-zA-Z0-9-_]")
         val safe = pattern.matcher(input).replaceAll("_")
         return safe.take(100)
     }

     fun downloadMemeWithManager(meme: Meme): Long {
         try {
             val extension = meme.url.substringAfterLast('.', "jpg")
             val sanitizedFileName = makeFilenameSafe(meme.name) + ".$extension"
             val request = DownloadManager.Request(Uri.parse(meme.url))
                 .setTitle(meme.name)
                 .setDescription("Downloading Meme...")
                 .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                 .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sanitizedFileName)
                 .setAllowedOverMetered(true)
                 .setAllowedOverRoaming(true)

             val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
             val downloadId = downloadManager.enqueue(request)
             Log.d("MemeRepository", "Enqueued download ($downloadId) for ${meme.name} to $sanitizedFileName")
             return downloadId
         } catch (e: Exception) {
             Log.e("MemeRepository", "Error starting download for ${meme.name}", e)
             return -1L
         }
     }

     // Removed downloadMemesAsZip function

    @RequiresApi(Build.VERSION_CODES.Q)
      private fun createOutputStreamQ(fileName: String, context: Context): OutputStream? {
          val resolver = context.contentResolver
          val downloadsCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
          val contentValues = ContentValues().apply {
              put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
              put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
              put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
              put(MediaStore.MediaColumns.IS_PENDING, 1)
          }

          val uri = resolver.insert(downloadsCollection, contentValues)
          var outputStream: OutputStream? = null
          if (uri != null) {
              try {
                   outputStream = resolver.openOutputStream(uri)
                   if (outputStream == null) {
                       Log.e("MemeRepository", "Failed to open output stream for URI: $uri, deleting entry.")
                       resolver.delete(uri, null, null)
                   }
              } catch (e: Exception) {
                  Log.e("MemeRepository", "Exception opening output stream for URI: $uri, deleting entry.", e)
                   try { resolver.delete(uri, null, null) } catch (deleteEx: Exception) { }
              }
          } else {
              Log.e("MemeRepository", "MediaStore insert failed for $fileName")
          }
          return outputStream
      }


     @RequiresApi(Build.VERSION_CODES.Q)
      private fun deleteIncompleteMediaStoreEntry(fileName: String, context: Context) {
          try {
              val resolver = context.contentResolver
              val downloadsCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
              val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.IS_PENDING}=1"
              val selectionArgs = arrayOf(Environment.DIRECTORY_DOWNLOADS + File.separator, fileName)
              val deletedRows = resolver.delete(downloadsCollection, selection, selectionArgs)
              if (deletedRows > 0) {
                  Log.d("MemeRepository", "Deleted $deletedRows incomplete/pending MediaStore entry for $fileName")
              } else {
                  Log.w("MemeRepository", "No pending MediaStore entry found to delete for $fileName")
              }
          } catch (e: Exception) {
              Log.e("MemeRepository", "Error deleting incomplete MediaStore entry for $fileName", e)
          }
      }

    @Throws(IOException::class)
    private fun downloadFile(url: String, destination: File) {
        val request = Request.Builder().url(url).build()
        val response: Response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) throw IOException("Failed to download file: ${response.code} $url")

        response.body.use { responseBody ->
             if (responseBody == null) throw IOException("Response body was null for $url")
             destination.sink().buffer().use { sink ->
                 sink.writeAll(responseBody.source())
             }
        }
    }


     suspend fun downloadAndUnzipBundle(bundleUrl: String, destinationDir: File): Result<Unit> = withContext(Dispatchers.IO) {
          try {
             Log.d("MemeRepository", "Starting download from $bundleUrl to $destinationDir")

             if (destinationDir.exists()) {
                 destinationDir.deleteRecursively()
             }
             destinationDir.mkdirs()


             val request = Request.Builder().url(bundleUrl).build()
             val response = okHttpClient.newCall(request).execute()

             if (!response.isSuccessful) {
                  throw IOException("Failed to download bundle: ${response.code} ${response.message}")
              }

             response.body?.let { body ->
                  val zipInputStream = ZipInputStream(BufferedInputStream(body.byteStream()))
                  var zipEntry: ZipEntry?

                  while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                      if (zipEntry == null) continue


                      val entryFile = File(destinationDir, zipEntry!!.name)
                      val entryPath = entryFile.canonicalPath
                      val destPath = destinationDir.canonicalPath

                      if (!entryPath.startsWith(destPath + File.separator)) {
                           throw SecurityException("Zip Slip vulnerability detected! Entry path $entryPath is outside destination $destPath")
                       }
                       Log.v("MemeRepository", "Processing entry: ${zipEntry!!.name}")


                      if (zipEntry!!.isDirectory) {
                          if (!entryFile.exists()) {
                              Log.v("MemeRepository", "Creating directory: ${entryFile.path}")
                              entryFile.mkdirs()
                          }
                      } else {

                          entryFile.parentFile?.mkdirs()

                          Log.v("MemeRepository", "Extracting file: ${entryFile.path}")
                          FileOutputStream(entryFile).use { fos ->
                              BufferedOutputStream(fos).use { output ->

                                   zipInputStream.copyTo(output)
                              }
                          }
                      }
                      zipInputStream.closeEntry()
                  }
                  zipInputStream.close()
                  Log.d("MemeRepository", "Download and unzip completed successfully.")
                  Result.success(Unit)

             } ?: Result.failure(IOException("Response body was null for bundle download"))

          } catch (e: Exception) {
              Log.e("MemeRepository", "Error downloading or unzipping bundle from $bundleUrl", e)
              destinationDir.deleteRecursively()
              Result.failure(e)
              }
     }
}
