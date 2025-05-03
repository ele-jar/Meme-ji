package com.elejar.memeji.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import com.elejar.memeji.R
import com.elejar.memeji.data.AppInfo
import com.elejar.memeji.data.CategoryItem
import com.elejar.memeji.data.Meme
import com.elejar.memeji.data.remote.RetrofitInstance
import com.elejar.memeji.repository.MemeRepository
import com.elejar.memeji.util.Event
import com.elejar.memeji.util.PreferencesHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import android.content.ContentValues


class MemeViewModel(application: Application) : AndroidViewModel(application) {

    private val _applicationContext = application.applicationContext

    private val repository = MemeRepository(
        RetrofitInstance.api,
        _applicationContext,
        RetrofitInstance.okHttpClient
    )

    private val _rawMemes = MutableLiveData<List<Meme>>()
    private val _totalMemeCount = MediatorLiveData<Int>().apply { value = 0 }
    val totalMemeCount: LiveData<Int> get() = _totalMemeCount

    private val _rawCategories = MutableLiveData<List<CategoryItem>>()

    private val _isCutieModeEnabled = MutableLiveData<Boolean>()
    val isCutieModeEnabled: LiveData<Boolean> get() = _isCutieModeEnabled

    private val _baseFilteredMemes = MediatorLiveData<List<Meme>>()

    private val _memesForCategory = MutableLiveData<List<Meme>?>()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // Holds the data only if an update is available and fetched successfully
    private val _availableUpdateInfo = MutableLiveData<AppInfo?>()
    val availableUpdateInfo: LiveData<AppInfo?> get() = _availableUpdateInfo

    private val _isCheckingForUpdate = MutableLiveData<Boolean>()
    val isCheckingForUpdate: LiveData<Boolean> get() = _isCheckingForUpdate

    // Simplified event for showing results (success or failure)
    private val _updateCheckResultEvent = MutableLiveData<Event<Int>>() // Holds String Resource ID
    val updateCheckResultEvent: LiveData<Event<Int>> get() = _updateCheckResultEvent


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

    data class ShareStatus(
        val message: String?,
        val isLoading: Boolean,
        val isError: Boolean = false,
        val shareUri: Uri? = null,
        val mimeType: String? = null
    )
    private val _shareStatus = MutableLiveData<Event<ShareStatus>>()
    val shareStatus: LiveData<Event<ShareStatus>> get() = _shareStatus

    private var pendingDownloadAction: (() -> Unit)? = null


    init {
        _isCutieModeEnabled.value = PreferencesHelper.isCutieModeEnabled(_applicationContext)
        setupMediators()
        _totalMemeCount.addSource(_rawMemes) { memes ->
            _totalMemeCount.value = memes?.size ?: 0
        }
        loadMemes(forceRefresh = false)
    }

    private fun setupMediators() {
        _baseFilteredMemes.addSource(_rawMemes) { memes ->
            filterMemesByCutieMode(memes, _isCutieModeEnabled.value ?: true)
        }
        _baseFilteredMemes.addSource(_isCutieModeEnabled) { isEnabled ->
            filterMemesByCutieMode(_rawMemes.value, isEnabled)
        }

        _filteredMemes.addSource(_baseFilteredMemes) { memes ->
             filterMemesBySearch(memes, _searchQuery.value, _memesForCategory.value)
        }
        _filteredMemes.addSource(_searchQuery) { query ->
             filterMemesBySearch(_baseFilteredMemes.value, query, _memesForCategory.value)
        }
        _filteredMemes.addSource(_memesForCategory) { categoryMemes ->
             filterMemesBySearch(_baseFilteredMemes.value, _searchQuery.value, categoryMemes)
        }

        _filteredCategories.addSource(_rawCategories) { categories ->
            filterCategoriesBySearch(categories, _categorySearchQuery.value)
        }
        _filteredCategories.addSource(_categorySearchQuery) { query ->
            filterCategoriesBySearch(_rawCategories.value, query)
        }
    }

    private fun filterMemesByCutieMode(memes: List<Meme>?, cutieModeEnabled: Boolean) {
        val source = memes ?: emptyList()
        _baseFilteredMemes.value = if (cutieModeEnabled) {
            source.filter { meme ->
                !meme.tags.any { it.equals(MemeRepository.SENSITIVE_TAG, ignoreCase = true) }
            }
        } else {
            source
        }
        filterMemesBySearch(_baseFilteredMemes.value, _searchQuery.value, _memesForCategory.value)
    }

    private fun filterMemesBySearch(baseMemes: List<Meme>?, query: String?, categoryMemesSource: List<Meme>?) {
         val currentQuery = query?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
         val sourceList = if (!categoryMemesSource.isNullOrEmpty()) {
             val cutieFilteredCategoryMemes = if (_isCutieModeEnabled.value == true) {
                 categoryMemesSource.filter { meme ->
                     !meme.tags.any { it.equals(MemeRepository.SENSITIVE_TAG, ignoreCase = true) }
                 }
             } else {
                 categoryMemesSource
             }
             cutieFilteredCategoryMemes

         } else {
             baseMemes ?: emptyList()
         }

        _filteredMemes.value = if (currentQuery.isNullOrBlank()) {
            sourceList
        } else {
            sourceList.filter { meme ->
                meme.name.lowercase().contains(currentQuery) ||
                        meme.tags.any { tag ->
                            !(_isCutieModeEnabled.value == true && tag.equals(MemeRepository.SENSITIVE_TAG, ignoreCase = true)) &&
                            tag.lowercase().contains(currentQuery)
                        }
            }
        }
    }

    private fun filterCategoriesBySearch(categories: List<CategoryItem>?, query: String?) {
        val currentQuery = query?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val source = categories ?: emptyList()

        _filteredCategories.value = if (currentQuery.isNullOrBlank()) {
            source
        } else {
            source.filter { category ->
                category.name.lowercase().contains(currentQuery)
            }
        }
    }


    fun loadMemes(forceRefresh: Boolean = false) {
        if (_isLoading.value == true && !forceRefresh && repository.isCacheEmpty()) {

        } else if (_isLoading.value == true && !forceRefresh) {
            Log.d("MemeViewModel", "Load already in progress, skipping.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            if (forceRefresh || _memesForCategory.value != null) {
                 _memesForCategory.value = null
            }

            Log.d("MemeViewModel", "Fetching memes (forceRefresh=$forceRefresh)")

            val result = repository.getMemes(forceRefresh = forceRefresh)
            result.onSuccess { memeList ->
                _rawMemes.value = memeList
                _rawCategories.value = repository.getCategoriesWithImages()
                Log.d("MemeViewModel", "Loaded ${memeList.size} raw memes, ${_rawCategories.value?.size ?: 0} categories.")
            }.onFailure { throwable ->
                handleError(throwable, "Failed to load memes")

            }
            _isLoading.value = false
        }
    }

    fun loadMemesForCategory(category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            if (repository.isCacheEmpty()) {
                 Log.d("MemeViewModel", "Cache empty, loading all memes before filtering for category '$category'")
                 val loadAllResult = repository.getMemes(forceRefresh = false)
                 if (loadAllResult.isFailure) {
                      handleError(loadAllResult.exceptionOrNull()!!, "Failed to load memes before category filter")
                      _isLoading.value = false
                      _memesForCategory.value = emptyList()
                      return@launch
                 }
             }

            val memes = repository.getMemesByCategory(category)
            _memesForCategory.value = memes
            Log.d("MemeViewModel", "Set ${memes.size} memes for category '$category' (pre-filter)")
            _isLoading.value = false
        }
    }

    fun switchToHomeView() {
        if (_memesForCategory.value != null || _error.value != null) {
            Log.d("MemeViewModel", "Switching to Home View, clearing category filter and error.")
            _memesForCategory.value = null
            _error.value = null
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


    fun updateCutieMode(enabled: Boolean) {
        if (_isCutieModeEnabled.value != enabled) {
            _isCutieModeEnabled.value = enabled
            PreferencesHelper.setCutieModeEnabled(_applicationContext, enabled)
            Log.d("MemeViewModel", "Cutie Mode updated to: $enabled")
        }
    }

    // Renamed for clarity
    fun checkForUpdates() {
        if (_isCheckingForUpdate.value == true) {
            Log.d("MemeViewModel", "Update check already in progress.")
            return
        }

        viewModelScope.launch {
            _isCheckingForUpdate.value = true
            _availableUpdateInfo.value = null // Clear previous details immediately
            Log.d("MemeViewModel", "Attempting to fetch app update info manually...")
            val result = repository.getAppUpdateInfo()
            result.onSuccess { info ->
                if (info.showDownload) {
                    _availableUpdateInfo.value = info // Store details only if update is available
                    _updateCheckResultEvent.value = Event(R.string.update_available) // Event indicates update available
                } else {
                    _updateCheckResultEvent.value = Event(R.string.no_update_available) // Event indicates no update
                }
                Log.d("MemeViewModel", "Successfully checked for updates.")
            }.onFailure { throwable ->
                Log.e("MemeViewModel", "Failed to fetch app update info", throwable)
                _updateCheckResultEvent.value = Event(R.string.error_checking_update) // Event indicates error
            }
            _isCheckingForUpdate.value = false
        }
    }


     suspend fun prepareMemeForSharing(meme: Meme) {
         if (_isCutieModeEnabled.value == true && meme.tags.any { it.equals(MemeRepository.SENSITIVE_TAG, ignoreCase = true) }) {
             _shareStatus.postValue(Event(ShareStatus("Cannot share this meme in Cutie Mode", isLoading = false, isError = true)))
             return
         }

         _shareStatus.postValue(Event(ShareStatus(getString(R.string.preparing_share), isLoading = true)))
         val downloadResult = repository.downloadImageToCache(meme.url)

         downloadResult.onSuccess { fileFromCache ->
             val shareableData = repository.getShareableUri(meme, fileFromCache)
             if (shareableData != null) {
                 _shareStatus.postValue(Event(ShareStatus(
                     message = getString(R.string.share_via),
                     isLoading = false,
                     shareUri = shareableData.uri,
                     mimeType = shareableData.mimeType
                 )))
             } else {
                 _shareStatus.postValue(Event(ShareStatus(getString(R.string.error_preparing_share), isLoading = false, isError = true)))
             }
         }.onFailure {
             _shareStatus.postValue(Event(ShareStatus(getString(R.string.error_preparing_share) + ": ${it.localizedMessage}", isLoading = false, isError = true)))
         }
     }

      fun clearShareIntentUri() {
           _shareStatus.value = Event(ShareStatus(null, isLoading = false))
      }

     fun downloadMeme(meme: Meme) {
          if (_isCutieModeEnabled.value == true && meme.tags.any { it.equals(MemeRepository.SENSITIVE_TAG, ignoreCase = true) }) {
             _singleMemeDownloadStatus.value = "Cannot download this meme in Cutie Mode"
             clearStatusAfterDelay(_singleMemeDownloadStatus, 4000L, true)
             return
         }
         val downloadId = repository.downloadMemeWithManager(meme)
         if (downloadId == -1L) {
             _singleMemeDownloadStatus.value = getString(R.string.download_failed_single, meme.name)
             clearStatusAfterDelay(_singleMemeDownloadStatus, 5000L, true)
         } else {
              _singleMemeDownloadStatus.value = getString(R.string.download_started, meme.name)
              clearStatusAfterDelay(_singleMemeDownloadStatus, 3000L, false)
         }
     }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun markMediaStoreDownloadComplete(pendingFile: File) {
         try {
            val resolver = _applicationContext.contentResolver
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.IS_PENDING}=1"
             val selectionArgs = arrayOf(Environment.DIRECTORY_DOWNLOADS + File.separator, pendingFile.name)
             val uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

             val updatedRows = resolver.update(uri, values, selection, selectionArgs)
             if (updatedRows > 0) {
                Log.d("MemeViewModel", "Marked MediaStore file as complete: ${pendingFile.name}")
            } else {
                 Log.w("MemeViewModel", "Could not find pending MediaStore file to mark complete or already marked: ${pendingFile.name}")
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


     fun clearSingleMemeDownloadStatus() {
         _singleMemeDownloadStatus.value = null
     }


    private fun clearStatusAfterDelay(liveData: MutableLiveData<String?>, delayMillis: Long, isError: Boolean) {
        val delayVal = if (isError) delayMillis * 2 else delayMillis
        viewModelScope.launch {
            delay(delayVal)
            if (liveData.value == liveData.value) {
               liveData.postValue(null)
            }
        }
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

    private fun handleError(throwable: Throwable, contextMessage: String) {
        Log.e("MemeViewModel", "$contextMessage: ${throwable.message}", throwable)

        val userMessage = when (throwable) {
            is IOException -> getString(R.string.error_network)
            is SecurityException -> "Permission denied: ${throwable.localizedMessage}"
            else -> throwable.localizedMessage ?: getString(R.string.unknown_error)
        }
        _error.value = "$contextMessage: $userMessage"
    }

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return _applicationContext.getString(resId, *formatArgs)
    }
}
