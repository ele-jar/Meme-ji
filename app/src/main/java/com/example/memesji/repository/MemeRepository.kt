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
                 Result.failure(IOException("Failed to download image using Glide"))
             }
         } catch (e: Exception) {
             Log.e("MemeRepository", "Error downloading image to cache", e)
             Result.failure(e)
         }
     }

     suspend fun getShareableUri(file: File): ShareableData? = withContext(Dispatchers.Main) {
         try {
             val cacheDir = File(context.cacheDir, "share_cache")
             cacheDir.mkdirs()

             val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
             val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension?.lowercase()) ?: "image/*"
             val safeFileNameWithExt = makeFilenameSafe(file.name) + if (extension != null) ".$extension" else ""
             val targetFile = File(cacheDir, safeFileNameWithExt)


             if (file.canonicalPath != targetFile.canonicalPath) {
                  withContext(Dispatchers.IO) {
                       file.copyTo(targetFile, overwrite = true)
                       Log.d("MemeRepository", "Copied cached file to share dir: ${targetFile.path}")
                  }
             } else {
                  Log.d("MemeRepository", "File already in share cache dir: ${targetFile.path}")
             }


             val uri = FileProvider.getUriForFile(
                 context,
                 "${context.packageName}.provider",
                 targetFile
             )
             ShareableData(uri, mimeType)
         } catch (e: Exception) {
             Log.e("MemeRepository", "Error creating FileProvider URI or getting MIME type", e)
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

     suspend fun downloadMemesAsZip(
         categoryName: String,
         memes: List<Meme>,
         progressCallback: (String) -> Unit
     ): Result<File> = withContext(Dispatchers.IO) {
         val tempDir = File(context.cacheDir, "temp_zip_$categoryName")
         val safeCategoryName = makeFilenameSafe(categoryName)
         val zipFileName = "${safeCategoryName}_Memes_${memes.size}.zip"
         var zipFile: File? = null
         var outputStream: OutputStream? = null
         var zipOutputStream: ZipOutputStream? = null

         try {
             if (tempDir.exists()) tempDir.deleteRecursively()
             tempDir.mkdirs()
             Log.d("MemeRepository", "Created temp directory: ${tempDir.path}")

             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                  outputStream = createOutputStreamQ(zipFileName, context)
                  zipFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), zipFileName)
                  Log.d("MemeRepository", "Using MediaStore for $zipFileName")
             } else {
                  val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                  if (!downloadsDir.exists()) downloadsDir.mkdirs()
                  zipFile = File(downloadsDir, zipFileName)
                  outputStream = FileOutputStream(zipFile)
                  Log.d("MemeRepository", "Using legacy storage for ${zipFile.path}")
             }

             if (outputStream == null) {
                 throw IOException("Could not create output stream for ZIP file")
             }

             zipOutputStream = ZipOutputStream(BufferedOutputStream(outputStream))

             memes.forEachIndexed { index, meme ->
                 if (!isActive) {
                      Log.w("MemeRepository", "Zip creation cancelled")
                      throw kotlinx.coroutines.CancellationException("Zip creation cancelled")
                  }
                 val fileExtension = meme.url.substringAfterLast('.', "jpg")
                 val safeEntryName = makeFilenameSafe(meme.name)
                 val entryName = "$safeEntryName.$fileExtension"
                 val tempFile = File(tempDir, entryName)

                 progressCallback(context.getString(R.string.downloading_for_zip, index + 1, memes.size))
                 Log.d("MemeRepository", "Downloading meme ${index + 1}/${memes.size}: ${meme.name} from ${meme.url}")

                 try {
                     downloadFile(meme.url, tempFile)
                     Log.d("MemeRepository", "Downloaded ${meme.name} to ${tempFile.path}")

                     zipOutputStream.putNextEntry(ZipEntry(entryName))
                     FileInputStream(tempFile).use { fis ->
                         fis.copyTo(zipOutputStream)
                     }
                     zipOutputStream.closeEntry()
                     Log.v("MemeRepository", "Added $entryName to zip")
                     tempFile.delete()
                  } catch (e: Exception) {
                      Log.e("MemeRepository", "Failed to download or add ${meme.name} to zip", e)
                  }
             }

             progressCallback(context.getString(R.string.creating_zip, zipFileName))
             zipOutputStream.flush()
             Log.d("MemeRepository", "Zip file creation finished for: $zipFileName")
             Result.success(zipFile ?: File(zipFileName))

         } catch (e: Exception) {
             Log.e("MemeRepository", "Error creating zip file for $categoryName", e)
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && outputStream != null) {
                  deleteIncompleteMediaStoreEntry(zipFileName, context)
              } else {
                  zipFile?.delete()
              }
             Result.failure(e)
         } finally {
             try {
                 zipOutputStream?.close()
                 outputStream?.close()
                 if (tempDir.exists()) {
                     tempDir.deleteRecursively()
                     Log.d("MemeRepository", "Deleted temp directory: ${tempDir.path}")
                 }
             } catch (ioe: IOException) {
                  Log.e("MemeRepository", "Error closing streams or deleting temp dir", ioe)
             }
         }
     }


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
