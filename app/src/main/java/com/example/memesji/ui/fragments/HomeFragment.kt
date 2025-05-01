package com.example.memesji.ui.fragments

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.memesji.R
import com.example.memesji.data.Meme
import com.example.memesji.databinding.FragmentHomeBinding

import com.example.memesji.ui.MainActivity
import com.example.memesji.ui.adapter.MemeAdapter
import com.example.memesji.viewmodel.MemeViewModel

import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemeViewModel by activityViewModels()
    private lateinit var memeAdapter: MemeAdapter
    private lateinit var layoutManager: GridLayoutManager
    private var detailDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.swipeRefreshLayout.updatePadding(top = insets.top)
            view.updatePadding(bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeToRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        memeAdapter = MemeAdapter { meme ->
            showMemeDetailDialog(meme)
        }

        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        layoutManager = GridLayoutManager(context, spanCount)

        binding.recyclerViewMemes.apply {
            adapter = memeAdapter
            layoutManager = this@HomeFragment.layoutManager
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.md_theme_surface)
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("HomeFragment", "Swipe to refresh triggered.")
            viewModel.loadMemes(forceRefresh = true)
        }
    }

    private fun observeViewModel() {
        // Observe filtered memes list
        viewModel.filteredMemes.observe(viewLifecycleOwner) { memes ->
            val isLoading = viewModel.isLoading.value ?: false
            val error = viewModel.error.value
            val hasData = !memes.isNullOrEmpty()

            binding.recyclerViewMemes.isVisible = !isLoading && error == null && hasData
            memeAdapter.submitList(memes)

            // Update empty/error state (dependent on loading/error too)
            binding.textViewNoMemes.isVisible = !isLoading && error == null && !hasData
            if (binding.textViewNoMemes.isVisible) {
                updateNoMemesText()
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val error = viewModel.error.value
            val hasData = !(viewModel.filteredMemes.value.isNullOrEmpty())

            if (!isLoading) {
                 binding.swipeRefreshLayout.isRefreshing = false
            }
            // Show progress only when loading, not refreshing, and no data/error shown yet
            binding.progressBar.isVisible = isLoading && !binding.swipeRefreshLayout.isRefreshing && !hasData && error == null

            // Re-evaluate error/empty visibility when loading stops
            if (!isLoading) {
                binding.textViewError.isVisible = error != null
                binding.textViewNoMemes.isVisible = error == null && !hasData
                if(binding.textViewError.isVisible) {
                     binding.textViewError.text = error ?: getString(R.string.unknown_error)
                }
                if(binding.textViewNoMemes.isVisible) {
                    updateNoMemesText()
                }
                 binding.recyclerViewMemes.isVisible = error == null && hasData
            }
        }

        // Observe error state
        viewModel.error.observe(viewLifecycleOwner) { error ->
            val isLoading = viewModel.isLoading.value ?: false
            val hasData = !(viewModel.filteredMemes.value.isNullOrEmpty())

            binding.textViewError.isVisible = error != null && !isLoading
            if (binding.textViewError.isVisible) {
                 binding.textViewError.text = error ?: getString(R.string.unknown_error)
            }

            // Hide other views if error is shown (unless loading)
            if (error != null && !isLoading) {
                 binding.recyclerViewMemes.isVisible = false
                 binding.textViewNoMemes.isVisible = false
                 binding.progressBar.isVisible = false
            } else {
                 // Re-evaluate normal visibility if error is cleared
                 binding.recyclerViewMemes.isVisible = !isLoading && hasData
                 binding.textViewNoMemes.isVisible = !isLoading && !hasData
                 if (binding.textViewNoMemes.isVisible) updateNoMemesText()
                 binding.progressBar.isVisible = isLoading && !binding.swipeRefreshLayout.isRefreshing && !hasData
            }
        }

        // Observe share status
         viewModel.shareStatus.observe(viewLifecycleOwner) { event ->
             event?.getContentIfNotHandled()?.let { status ->
                 updateShareProgress(status)
             }
         }
    }


    private fun updateNoMemesText() {
         val query = viewModel.searchQuery.value
         binding.textViewNoMemes.text = if (query.isNullOrBlank()) {
             getString(R.string.no_memes_found)
         } else {
             getString(R.string.no_memes_match_search, query)
         }
     }

     private fun showMemeDetailDialog(meme: Meme) {
         detailDialog?.dismiss()

         context?.let { ctx ->
             val dialog = Dialog(ctx)
             dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
             dialog.setContentView(R.layout.dialog_meme_detail)
             dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
             dialog.window?.setDimAmount(0.7f)

             dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
             dialog.setCanceledOnTouchOutside(true)

             val memeImageView = dialog.findViewById<ImageView>(R.id.imageViewDialogMeme)
             val memeNameTextView = dialog.findViewById<TextView>(R.id.textViewDialogMemeName)
             val downloadButton = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonDialogDownload)
             val browserButton = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonDialogOpenBrowser)
             val shareButton = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonDialogShare)
             val backButton = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonDialogBack)

             memeNameTextView.text = meme.name
             Glide.with(ctx)
                 .load(meme.url)
                 .placeholder(R.drawable.ic_placeholder_image)
                 .error(R.drawable.ic_placeholder_image)
                 .transition(DrawableTransitionOptions.withCrossFade())
                 .into(memeImageView)

             downloadButton.setOnClickListener {
                 (activity as? MainActivity)?.downloadMeme(meme)
             }

             browserButton.setOnClickListener {
                 (activity as? MainActivity)?.openUrlInBrowser(meme.url)
             }

             shareButton.setOnClickListener {
                  shareMeme(meme)
             }

             backButton.setOnClickListener {
                 dialog.dismiss()
             }

             dialog.setOnDismissListener {
                 detailDialog = null
             }

             detailDialog = dialog
             dialog.show()
         }
     }

      private fun shareMeme(meme: Meme) {
          viewLifecycleOwner.lifecycleScope.launch {
              viewModel.prepareMemeForSharing(meme)
          }
      }

      private fun updateShareProgress(status: MemeViewModel.ShareStatus) {
         detailDialog?.let { dialog ->
             val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBarDialogShare)
             val statusText = dialog.findViewById<TextView>(R.id.textViewDialogShareStatus)

             progressBar?.isVisible = status.isLoading
             statusText?.isVisible = !status.message.isNullOrBlank()
             statusText?.text = status.message ?: ""

             if (!status.isLoading) {
                 if (!status.isError && status.shareUri != null && status.mimeType != null) {
                     startShareIntent(status.shareUri, status.mimeType)
                     viewModel.clearShareIntentUri()
                 } else if (status.isError) {
                     if (!status.message.isNullOrBlank()) {
                         Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                     }

                 }
             }
         }
     }

     private fun startShareIntent(imageUri: Uri, mimeType: String) {
         try {
             val shareIntent = Intent(Intent.ACTION_SEND).apply {
                 type = mimeType
                 putExtra(Intent.EXTRA_STREAM, imageUri)
                 addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
             }
             startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
         } catch (e: Exception) {
             Log.e("HomeFragment", "Error starting share intent", e)
             Toast.makeText(context, getString(R.string.share_error), Toast.LENGTH_SHORT).show()
         }
     }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val spanCount = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        layoutManager.spanCount = spanCount
    }

    override fun onPause() {
        super.onPause()
        detailDialog?.dismiss()
    }

    override fun onDestroyView() {
        detailDialog?.dismiss()
        detailDialog = null
        super.onDestroyView()
        binding.recyclerViewMemes.adapter = null
        _binding = null
    }
}
