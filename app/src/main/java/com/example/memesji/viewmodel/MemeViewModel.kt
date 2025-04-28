package com.example.memesji.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import com.example.memesji.R
import com.example.memesji.data.CategoryItem
import com.example.memesji.data.Meme
import com.example.memesji.data.remote.RetrofitInstance
import com.example.memesji.repository.MemeRepository
import com.example.memesji.util.Event
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class MemeViewModel(application: Application) : AndroidViewModel(application) {

    private val _applicationContext = application.applicationContext

    private val repository = MemeRepository(
        RetrofitInstance.api,
        _applicationContext,
        RetrofitInstance.okHttpClient
    )


    private val _memes = MutableLiveData<List<Meme>>()
    private val _categories = MutableLiveData<List<CategoryItem>>()
    private val _memesForCategory = MutableLiveData<List<Meme>>()


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error


    private val _bundleDownloadStatus = MutableLiveData<String?>()
    val bundleDownloadStatus: LiveData<String?> get() = _bundleDownloadStatus

    private val _singleMemeDownloadStatus = MutableLiveData<String?>()
    val singleMemeDownloadStatus: LiveData<String?> get() = _singleMemeDownloadStatus


    private val _searchQuery = MutableLiveData<String?>()
    val searchQuery: LiveData<String?> get() = _searchQuery

    private val _categorySearchQuery = MutableLiveData<String?>()
    val categorySearchQuery: LiveData<String?> get() = _categorySearchQuery


    private val _filteredMemes = MediatorLiveData<List<Meme>>()
    val filteredMemes: LiveData<List<Meme>> get() = _filteredMemes

    private val _filteredCategories = MediatorLiveData<List<CategoryItem>>()
    val filteredCategories: LiveData<List<CategoryItem>> get() = _filteredCategories


    val categories: LiveData<List<CategoryItem>> get() = _filteredCategories
    val memesForCategory: LiveData<List<Meme>> get() = _filteredMemes

     data class ShareStatus(val message: String?, val isLoading: Boolean, val isError: Boolean = false)
     private val _shareStatus = MutableLiveData<Event<ShareStatus>>()
     val shareStatus: LiveData<Event<ShareStatus>> get() = _shareStatus
     var shareIntentUri: Uri? = null
         private set


     private val _categoryDownloadStatus = MutableLiveData<String?>()
     val categoryDownloadStatus: LiveData<String?> get() = _categoryDownloadStatus

     private var pendingDownloadAction: (() -> Unit)? = null


    init {
        setupMediators()
        loadMemes(forceRefresh = false)
    }

    private fun setupMediators() {
        _filteredMemes.addSource(_memes) { filterMemesBasedOnContext() }
        _filteredMemes.addSource(_memesForCategory) { filterMemesBasedOnContext() }
        _filteredMemes.addSource(_searchQuery) { filterMemesBasedOnContext() }

        _filteredCategories.addSource(_categories) { filterCategories(it, _categorySearchQuery.value) }
        _filteredCategories.addSource(_categorySearchQuery) { query -> filterCategories(_categories.value, query)}
    }

    private fun filterMemesBasedOnContext() {
        val source = if (_memesForCategory.value?.isNotEmpty() == true) {
            _memesForCategory.value
        } else {
            _memes.value
        }
        filterMemes(source, _searchQuery.value)
    }

    fun loadMemes(forceRefresh: Boolean = false) {
        if (_isLoading.value == true && !forceRefresh) {
            Log.d("MemeViewModel", "Load already in progress, skipping.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val currentSourceIsAllMemes = _memesForCategory.value.isNullOrEmpty()
            if(currentSourceIsAllMemes || forceRefresh){
                 _memesForCategory.value = emptyList()
            }


            Log.d("MemeViewModel", "Fetching memes (forceRefresh=$forceRefresh)")

            val result = repository.getMemes(forceRefresh = forceRefresh)
            result.onSuccess { memeList ->

                 if(currentSourceIsAllMemes || forceRefresh){
                     _memes.value = memeList
                     _categories.value = repository.getCategoriesWithImages()
                 } else {
                     
                 }

                Log.d("MemeViewModel", "Loaded ${memeList.size} memes, ${_categories.value?.size ?: 0} categories.")
            }.onFailure { throwable ->
                handleError(throwable, "Failed to load memes")
                _memes.value = emptyList()
                _categories.value = emptyList()
                 _memesForCategory.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun loadMemesForCategory(category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _memes.value = emptyList() 

            if (repository.isCacheEmpty()) {
                 Log.d("MemeViewModel", "Cache empty, loading all memes before filtering for category '$category'")
                 val loadAllResult = repository.getMemes(forceRefresh = false)
                 loadAllResult.onFailure {
                      handleError(it, "Failed to load memes before category filter")
                      _isLoading.value = false
                      _memesForCategory.value = emptyList()
                      return@launch
                 }
             }

            val memes = repository.getMemesByCategory(category)
            _memesForCategory.value = memes
            Log.d("MemeViewModel", "Loaded ${memes.size} memes for category '$category'")
            _isLoading.value = false
        }
    }


    fun setSearchQuery(query: String?) {
        val trimmedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        if (_searchQuery.value != trimmedQuery) {
            _searchQuery.value = trimmedQuery
            Log.d("MemeViewModel", "Search query set to: $trimmedQuery")
        }
    }

    fun setCategorySearchQuery(query: String?) {
        val trimmedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        if (_categorySearchQuery.value != trimmedQuery) {
            _categorySearchQuery.value = trimmedQuery
            Log.d("MemeViewModel", "Category search query set to: $trimmedQuery")
        }
    }


    private fun filterMemes(sourceList: List<Meme>?, query: String?) {
        val currentQuery = query?.lowercase()
        val memes = sourceList ?: emptyList()

        _filteredMemes.value = if (currentQuery.isNullOrBlank()) {
            memes
        } else {
            memes.filter { meme ->
                meme.name.lowercase().contains(currentQuery) ||
                        meme.tags.any { tag -> tag.lowercase().contains(currentQuery) }
            }
        }
        
    }


    private fun filterCategories(sourceList: List<CategoryItem>?, query: String?) {
        val currentQuery = query?.lowercase()
        val cats = sourceList ?: emptyList()

        _filteredCategories.value = if (currentQuery.isNullOrBlank()) {
            cats
        } else {
            cats.filter { category ->
                category.name.lowercase().contains(currentQuery)
            }
        }
        
    }


    fun downloadBundle(bundleName: String, bundleUrl: String) {
        viewModelScope.launch {
            postBundleDownloadStatus(getString(R.string.bundle_download_starting, bundleName))
            _isLoading.value = true
            val destinationDir = File(_applicationContext.filesDir, "meme_bundles/$bundleName")

            val result = repository.downloadAndUnzipBundle(bundleUrl, destinationDir)

            result.onSuccess {
                postBundleDownloadStatus(getString(R.string.bundle_download_success, bundleName))
            }.onFailure { throwable ->
                 if (throwable is CancellationException) {
                      postBundleDownloadStatus(getString(R.string.bundle_download_cancelled, bundleName))
                  } else {
                      postBundleDownloadStatus(getString(R.string.bundle_download_failed, bundleName, throwable.localizedMessage ?: "Unknown error"), isError = true)
                      Log.e("MemeViewModel", "Download failed for $bundleName", throwable)
                  }
            }
            _isLoading.value = false
        }
    }

     suspend fun prepareMemeForSharing(meme: Meme) {
         _shareStatus.postValue(Event(ShareStatus(getString(R.string.preparing_share), isLoading = true)))
         val downloadResult = repository.downloadImageToCache(meme.url)

         downloadResult.onSuccess { file ->
             val uri = repository.getShareableUri(file)
             if (uri != null) {
                 shareIntentUri = uri
                 _shareStatus.postValue(Event(ShareStatus(getString(R.string.share_via), isLoading = false)))
             } else {
                 _shareStatus.postValue(Event(ShareStatus(getString(R.string.error_preparing_share), isLoading = false, isError = true)))
             }
         }.onFailure {
             _shareStatus.postValue(Event(ShareStatus(getString(R.string.error_preparing_share) + ": ${it.localizedMessage}", isLoading = false, isError = true)))
         }
     }

      fun clearShareIntentUri() {
          shareIntentUri = null
          
      }

     fun downloadMeme(meme: Meme) {
         val downloadId = repository.downloadMemeWithManager(meme)
         if (downloadId == -1L) {
             _singleMemeDownloadStatus.value = getString(R.string.download_failed_single, meme.name)
             clearStatusAfterDelay(_singleMemeDownloadStatus, 5000L, true)
         } else {
              _singleMemeDownloadStatus.value = getString(R.string.download_started, meme.name)
              clearStatusAfterDelay(_singleMemeDownloadStatus, 3000L, false)
         }
     }

     fun downloadMemesOneByOne(memes: List<Meme>) {
         viewModelScope.launch {
              var successCount = 0
              var failCount = 0
              memes.forEachIndexed { index, meme ->
                  val downloadId = repository.downloadMemeWithManager(meme)
                  if (downloadId == -1L) {
                      Log.e("MemeViewModel", "Failed to start one-by-one download for ${meme.name}")
                      failCount++
                  } else {
                      successCount++
                      Log.d("MemeViewModel", "Started one-by-one download ${index + 1}/${memes.size}: ${meme.name}")
                  }
                  
                  if (index < memes.size - 1) {
                      delay(500) 
                  }
              }
               val message = getString(R.string.download_one_by_one_complete, successCount, memes.size)
               _singleMemeDownloadStatus.value = message
               clearStatusAfterDelay(_singleMemeDownloadStatus, 5000L, failCount > 0)
          }
      }

     
     

     fun downloadCategoryAsZip(categoryName: String, memes: List<Meme>) {
          viewModelScope.launch {
              postCategoryDownloadStatus(getString(R.string.preparing_zip))
              _isLoading.postValue(true)

              val result = repository.downloadMemesAsZip(categoryName, memes) { progressMessage ->
                   
                   postCategoryDownloadStatus(progressMessage)
               }

               result.onSuccess { file ->
                   
                   val finalName = getFileNameFromMediaStoreUri(file) ?: file.name
                   postCategoryDownloadStatus(getString(R.string.zip_download_success, finalName))
                   
                   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                       markMediaStoreDownloadComplete(file)
                   }

               }.onFailure { throwable ->
                   Log.e("MemeViewModel", "ZIP Creation/Save failed for category $categoryName", throwable)
                   if (throwable is CancellationException) {
                        postCategoryDownloadStatus(getString(R.string.zip_download_cancelled))
                   } else {
                        postCategoryDownloadStatus(getString(R.string.zip_download_failed, throwable.localizedMessage ?: "Unknown error"))
                   }
               }
               _isLoading.postValue(false)
          }
     }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun markMediaStoreDownloadComplete(pendingFile: File) {
         try {
            val resolver = _applicationContext.contentResolver
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.IS_PENDING}=1"
             val selectionArgs = arrayOf(pendingFile.name)
             val uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

             val updatedRows = resolver.update(uri, values, selection, selectionArgs)
             if (updatedRows > 0) {
                Log.d("MemeViewModel", "Marked MediaStore file as complete: ${pendingFile.name}")
            } else {
                 Log.w("MemeViewModel", "Could not find pending MediaStore file to mark complete: ${pendingFile.name}")
             }
         } catch (e: Exception) {
             Log.e("MemeViewModel", "Error marking MediaStore file complete", e)
         }
    }


     private fun getFileNameFromMediaStoreUri(file: File): String? {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
               return file.name
           }
          var fileName: String? = null
          try {
              val resolver = _applicationContext.contentResolver
              
              val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
               val selectionArgs = arrayOf(file.name)
               val uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

              resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), selection, selectionArgs, null)?.use { cursor ->
                  if (cursor.moveToFirst()) {
                      val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                      if (nameIndex != -1) {
                          fileName = cursor.getString(nameIndex)
                      }
                  }
              }
          } catch (e: Exception) {
              Log.e("MemeViewModel", "Could not get file name from MediaStore for ${file.name}", e)
          }
          return fileName ?: file.name
      }


      fun clearCategoryDownloadStatus() {
          _categoryDownloadStatus.value = null
      }

     fun clearSingleMemeDownloadStatus() {
         _singleMemeDownloadStatus.value = null
     }

     private fun postCategoryDownloadStatus(message: String, duration: Long = 5000L) {
          _categoryDownloadStatus.postValue(message)
          
          
      }

     fun setPendingDownloadAction(action: () -> Unit) {
         pendingDownloadAction = action
     }

     fun executePendingDownloadAction() {
         pendingDownloadAction?.invoke()
         pendingDownloadAction = null
     }

     fun clearPendingDownloadAction() {
         pendingDownloadAction = null
     }


     private fun makeFilenameSafe(input: String): String {
         val pattern = Pattern.compile("[^a-zA-Z0-9-_\\.]")
         val safe = pattern.matcher(input).replaceAll("_")
         return safe.take(100)
     }




    fun clearBundleDownloadStatus() {
        _bundleDownloadStatus.value = null
    }

    private fun clearStatusAfterDelay(liveData: MutableLiveData<String?>, delayMillis: Long, isError: Boolean) {
        val delay = if (isError) delayMillis * 2 else delayMillis
        viewModelScope.launch {
            delay(delay)
            
            
            liveData.postValue(null)
        }
    }


    private fun postBundleDownloadStatus(message: String, duration: Long = 4000L, isError: Boolean = false) {
        _bundleDownloadStatus.value = message
        clearStatusAfterDelay(_bundleDownloadStatus, duration, isError)
    }


    private fun handleError(throwable: Throwable, contextMessage: String) {
        Log.e("MemeViewModel", "$contextMessage: ${throwable.message}", throwable)

        val userMessage = when (throwable) {
            is IOException -> getString(R.string.error_network)
            
            else -> throwable.localizedMessage ?: getString(R.string.unknown_error)
        }
        _error.value = "$contextMessage: $userMessage"
    }


    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return _applicationContext.getString(resId, *formatArgs)
    }
}
