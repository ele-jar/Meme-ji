package com.example.memesji.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.memesji.BuildConfig
import com.example.memesji.R
import com.example.memesji.databinding.FragmentMoreBinding
import com.example.memesji.ui.MainActivity
import com.example.memesji.viewmodel.MemeViewModel
import com.google.android.material.transition.MaterialFadeThrough

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemeViewModel by activityViewModels()


    private val memeBundles = mapOf(
        "Classic Memes" to "https://github.com/ele-jar/meme-database/archive/refs/heads/main.zip",
        "Sad Memes" to "https://example.com/memes_sad.zip",
        "Relatable Memes" to "https://example.com/memes_relatable.zip"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.scrollViewMore.updatePadding(top = insets.top)
            view.updatePadding(bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaders()
        setupInfoSection()
        setupContributeSection()
        setupMoreSection()
        setupClickListeners()
        observeViewModel()
    }


    private fun setupHeaders() {
        binding.headerInfo.headerText.text = getString(R.string.more_header_info)
        binding.headerContribute.headerText.text = getString(R.string.more_header_contribute)
        binding.headerMore.headerText.text = getString(R.string.more_header_more)
        binding.headerDownloads.headerText.text = getString(R.string.more_header_downloads)
    }


    private fun setupInfoSection() {
        binding.itemAppVersion.apply {
            primaryText.text = getString(R.string.version)
            secondaryText.text = BuildConfig.VERSION_NAME
            secondaryText.isVisible = true
            icon.setImageResource(R.drawable.ic_info_outline)
        }
        binding.itemDeveloper.apply {
            primaryText.text = getString(R.string.developer)
            secondaryText.text = getString(R.string.developer_name) // Updated string used here
            secondaryText.isVisible = true
            icon.setImageResource(R.drawable.ic_person_outline)
        }
        binding.itemTotalMemes.apply {
            primaryText.text = getString(R.string.total_memes_title)
            secondaryText.text = getString(R.string.total_memes_loading) // Placeholder
            secondaryText.isVisible = true
            icon.setImageResource(R.drawable.ic_counter) // Replace with suitable icon if needed
        }
         binding.itemSourceCode.apply {
             primaryText.text = getString(R.string.source_code_title)
             secondaryText.text = getString(R.string.source_code_url)
             secondaryText.isVisible = true
             icon.setImageResource(R.drawable.ic_code)
             root.setOnClickListener { openUrl("https://" + getString(R.string.source_code_url)) }
         }
    }


    private fun setupContributeSection() {
        binding.itemShareApp.apply {
             primaryText.text = getString(R.string.share_app_title)
             secondaryText.text = getString(R.string.share_app_desc)
             secondaryText.isVisible = true
             icon.setImageResource(R.drawable.ic_share) // Use share icon
         }
         binding.itemReportBug.apply {
             primaryText.text = getString(R.string.more_report_bug)
             secondaryText.text = getString(R.string.more_report_bug_desc)
             secondaryText.isVisible = true
             icon.setImageResource(R.drawable.ic_bug_report)
         }
         binding.itemTranslate.apply {
             primaryText.text = getString(R.string.more_translate)
             secondaryText.text = getString(R.string.more_translate_desc)
             secondaryText.isVisible = true
             icon.setImageResource(R.drawable.ic_translate)
         }
    }


     private fun setupMoreSection() {
         binding.itemSettings.apply {
             primaryText.text = getString(R.string.settings)
             secondaryText.text = getString(R.string.more_settings_desc_action)
             secondaryText.isVisible = true
             icon.setImageResource(R.drawable.ic_settings)
         }
         binding.itemSocials.apply {
             primaryText.text = getString(R.string.more_socials)
             secondaryText.text = getString(R.string.more_socials_desc)
             secondaryText.isVisible = true
             icon.setImageResource(R.drawable.ic_link)
         }
     }


    private fun setupClickListeners() {

        binding.itemShareApp.root.setOnClickListener { shareApp() } // Share app listener
        binding.itemReportBug.root.setOnClickListener { openUrl(getString(R.string.url_report_bug)) } // Updated URL used
        binding.itemTranslate.root.setOnClickListener { openUrl(getString(R.string.url_translate)) }
        binding.itemSettings.root.setOnClickListener {
            findNavController().navigate(MoreFragmentDirections.actionMoreFragmentToSettingsFragment())
        }
        binding.itemSocials.root.setOnClickListener { openUrl(getString(R.string.url_developer_profile)) } // Updated URL used


        binding.buttonDownloadClassic.setOnClickListener {
            downloadBundle("Classic Memes", memeBundles["Classic Memes"])
        }
        binding.buttonDownloadSad.setOnClickListener {
            downloadBundle("Sad Memes", memeBundles["Sad Memes"])
        }
        binding.buttonDownloadRelatable.setOnClickListener {
            downloadBundle("Relatable Memes", memeBundles["Relatable Memes"])
        }

         // Removed itemSource click listener
         binding.itemSourceCode.root.setOnClickListener { openUrl("https://" + getString(R.string.source_code_url)) } // Updated URL used
    }

    private fun downloadBundle(bundleName: String, bundleUrl: String?) {
        if (bundleUrl == null || bundleUrl.contains("example.com")) {
            Toast.makeText(context, getString(R.string.download_url_not_available, bundleName), Toast.LENGTH_SHORT).show()
            return
        }
        (activity as? MainActivity)?.requestStoragePermission {
            viewModel.downloadBundle(bundleName, bundleUrl)
        }
    }

    private fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject))
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text))
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app_title)))
        } catch (e: Exception) {
            Toast.makeText(context, R.string.share_error, Toast.LENGTH_SHORT).show()
            Log.e("MoreFragment", "Could not launch share intent", e)
        }
    }

    private fun observeViewModel() {
         viewModel.bundleDownloadStatus.observe(viewLifecycleOwner) { status ->
             binding.textViewBundleDownloadStatus.isVisible = !status.isNullOrBlank()
             binding.textViewBundleDownloadStatus.text = status ?: ""

             val isDownloading = status?.contains("...") == true || status?.contains(getString(R.string.bundle_download_starting).substringBefore('%')) == true
             binding.progressBarBundleDownload.isVisible = isDownloading
             setDownloadButtonsEnabled(!isDownloading)
         }

         viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val currentStatus = viewModel.bundleDownloadStatus.value
            val isBundleDownloading = currentStatus?.contains("...") == true || currentStatus?.contains(getString(R.string.bundle_download_starting).substringBefore('%')) == true

             if (isLoading && isBundleDownloading) {
                 setDownloadButtonsEnabled(false)
                 binding.progressBarBundleDownload.isVisible = true
             } else if (!isBundleDownloading) {
                 setDownloadButtonsEnabled(true)
                 binding.progressBarBundleDownload.isVisible = false
                  if(!isLoading && currentStatus != null && !currentStatus.contains(getString(R.string.bundle_download_success).substringBefore('%')) && !currentStatus.contains(getString(R.string.bundle_download_failed).substringBefore('%')) ) {
                     viewModel.clearBundleDownloadStatus()
                  }
             }
         }

         // Observe total meme count
         viewModel.totalMemeCount.observe(viewLifecycleOwner) { count ->
             binding.itemTotalMemes.secondaryText.text = count?.toString() ?: getString(R.string.total_memes_loading)
             binding.itemTotalMemes.secondaryText.isVisible = true
         }
     }

    private fun setDownloadButtonsEnabled(enabled: Boolean) {
        binding.buttonDownloadClassic.isEnabled = enabled
        binding.buttonDownloadSad.isEnabled = enabled
        binding.buttonDownloadRelatable.isEnabled = enabled
    }

    private fun openPlayStoreForRating() { // Keep this function even if item removed, might be used elsewhere
        val packageName = context?.packageName ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        } catch (e: Exception) {
            Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(url: String) {
         if (url.isBlank() || !url.startsWith("http")) { // Basic check for valid URL start
             Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
             Log.w("MoreFragment", "Attempted to open invalid URL: $url")
             return
         }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MoreFragment", "Could not open URL: $url", e)
            Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
