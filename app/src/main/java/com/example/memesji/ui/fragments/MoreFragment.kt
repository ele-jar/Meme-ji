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

    // Removed memeBundles map

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
        viewModel.fetchAppUpdateInfo() // Fetch info when view is created
    }


    private fun setupHeaders() {
        binding.headerInfo.headerText.text = getString(R.string.more_header_info)
        binding.headerContribute.headerText.text = getString(R.string.more_header_contribute)
        binding.headerMore.headerText.text = getString(R.string.more_header_more)
        binding.headerUpdate.headerText.text = getString(R.string.more_header_update) // Updated header ID
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
            secondaryText.text = getString(R.string.developer_name)
            secondaryText.isVisible = true
            icon.setImageResource(R.drawable.ic_person_outline)
        }
        binding.itemTotalMemes.apply {
            primaryText.text = getString(R.string.total_memes_title)
            secondaryText.text = getString(R.string.total_memes_loading)
            secondaryText.isVisible = true
            icon.setImageResource(R.drawable.ic_counter)
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
             icon.setImageResource(R.drawable.ic_share)
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

        binding.itemShareApp.root.setOnClickListener { shareApp() }
        binding.itemReportBug.root.setOnClickListener { openUrl(getString(R.string.url_report_bug)) }
        binding.itemTranslate.root.setOnClickListener { openUrl(getString(R.string.url_translate)) }
        binding.itemSettings.root.setOnClickListener {
            findNavController().navigate(MoreFragmentDirections.actionMoreFragmentToSettingsFragment())
        }
        binding.itemSocials.root.setOnClickListener { openUrl(getString(R.string.url_developer_profile)) }

        // Removed download bundle listeners

         binding.itemSourceCode.root.setOnClickListener { openUrl("https://" + getString(R.string.source_code_url)) }

        // Click listener for the new download button is set in observeViewModel when data is available
    }

    // Removed downloadBundle function

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
         // Observe total meme count
         viewModel.totalMemeCount.observe(viewLifecycleOwner) { count ->
             binding.itemTotalMemes.secondaryText.text = count?.toString() ?: getString(R.string.total_memes_loading)
             binding.itemTotalMemes.secondaryText.isVisible = true
         }

         // Observe App Info Loading State
         viewModel.isAppInfoLoading.observe(viewLifecycleOwner) { isLoading ->
             binding.progressBarAppInfo.isVisible = isLoading
             if (isLoading) {
                 binding.textViewAppInfoError.isVisible = false // Hide error while loading
                 binding.updateSection.isVisible = false // Hide content while loading
             }
         }

         // Observe App Info Error State
         viewModel.appInfoError.observe(viewLifecycleOwner) { error ->
             val isLoading = viewModel.isAppInfoLoading.value ?: false
             binding.textViewAppInfoError.isVisible = error != null && !isLoading
             if(binding.textViewAppInfoError.isVisible) {
                 binding.textViewAppInfoError.text = error ?: getString(R.string.error_loading_app_info)
                 binding.updateSection.isVisible = false // Hide content on error
             }
         }

         // Observe App Info Data
         viewModel.appInfo.observe(viewLifecycleOwner) { appInfo ->
            val error = viewModel.appInfoError.value
            val isLoading = viewModel.isAppInfoLoading.value ?: false
            val shouldShowUpdate = appInfo != null && appInfo.showDownload && error == null && !isLoading

            binding.updateSection.isVisible = shouldShowUpdate
            binding.headerUpdate.root.isVisible = shouldShowUpdate // Show/hide header with section

            if (shouldShowUpdate) {
                binding.textViewUpdateVersion.text = getString(R.string.version_info_format, appInfo?.version ?: "N/A")
                binding.textViewUpdateDate.text = getString(R.string.release_date_format, appInfo?.buildDate ?: "N/A")
                binding.textViewChangelog.text = appInfo?.changelog ?: "No changelog available."
                binding.buttonDownloadUpdate.setOnClickListener {
                    appInfo?.downloadUrl?.let { url -> openUrl(url) }
                        ?: Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
                }
            }
         }
     }

    // Removed setDownloadButtonsEnabled function

    private fun openPlayStoreForRating() {
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
         if (url.isBlank() || !url.startsWith("http")) {
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
