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
import com.example.memesji.util.EventObserver
import com.example.memesji.viewmodel.MemeViewModel
import com.google.android.material.transition.MaterialFadeThrough

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemeViewModel by activityViewModels()

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
        setupUpdateSection() // Setup the initial state of the update section
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Removed automatic update check from onResume
    }


    private fun setupHeaders() {
        binding.headerInfo.headerText.text = getString(R.string.more_header_info)
        binding.headerContribute.headerText.text = getString(R.string.more_header_contribute)
        binding.headerMore.headerText.text = getString(R.string.more_header_more)
        binding.headerUpdate.headerText.text = getString(R.string.more_header_update)
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

     private fun setupUpdateSection() {
         binding.itemCheckForUpdate.apply {
             primaryText.text = getString(R.string.check_for_update_title)
             secondaryText.text = getString(R.string.check_for_update_desc)
             secondaryText.isVisible = true
             icon.setImageResource(R.drawable.ic_update)
         }
         binding.progressBarAppInfo.isVisible = false
         binding.textViewAppInfoError.isVisible = false
         binding.updateDetailsSection.isVisible = false
     }


    private fun setupClickListeners() {
        binding.itemShareApp.root.setOnClickListener { shareApp() }
        binding.itemReportBug.root.setOnClickListener { openUrl(getString(R.string.url_report_bug)) }
        binding.itemTranslate.root.setOnClickListener { openUrl(getString(R.string.url_translate)) }
        binding.itemSettings.root.setOnClickListener {
            findNavController().navigate(MoreFragmentDirections.actionMoreFragmentToSettingsFragment())
        }
        binding.itemSocials.root.setOnClickListener { openUrl(getString(R.string.url_developer_profile)) }
        binding.itemSourceCode.root.setOnClickListener { openUrl("https://" + getString(R.string.source_code_url)) }
        binding.itemCheckForUpdate.root.setOnClickListener {
            Log.d("MoreFragment", "Check for update clicked.")
            binding.itemCheckForUpdate.secondaryText.text = getString(R.string.checking_for_updates)
            viewModel.fetchAppUpdateInfo()
        }
        // Download button click listener is set dynamically in observeViewModel
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
         viewModel.totalMemeCount.observe(viewLifecycleOwner) { count ->
             binding.itemTotalMemes.secondaryText.text = count?.toString() ?: getString(R.string.total_memes_loading)
             binding.itemTotalMemes.secondaryText.isVisible = true
         }

         viewModel.isAppInfoLoading.observe(viewLifecycleOwner) { isLoading ->
             binding.progressBarAppInfo.isVisible = isLoading
             binding.itemCheckForUpdate.root.isEnabled = !isLoading // Disable click while loading
             if (isLoading) {
                 binding.textViewAppInfoError.isVisible = false
                 binding.updateDetailsSection.isVisible = false // Hide details while checking
                 binding.itemCheckForUpdate.secondaryText.text = getString(R.string.checking_for_updates)
             } else {
                 // Re-enable click after loading finishes (success or error)
                 binding.itemCheckForUpdate.root.isEnabled = true
             }
         }

         viewModel.appInfoError.observe(viewLifecycleOwner) { error ->
             val isLoading = viewModel.isAppInfoLoading.value ?: false
             binding.textViewAppInfoError.isVisible = error != null && !isLoading
             if(binding.textViewAppInfoError.isVisible) {
                 binding.textViewAppInfoError.text = error ?: getString(R.string.error_loading_app_info)
                 binding.updateDetailsSection.isVisible = false // Hide details on error
                 binding.itemCheckForUpdate.secondaryText.text = getString(R.string.check_for_update_desc_error)
             } else if (!isLoading) {
                 // Hide error text if there's no error and not loading
                 binding.textViewAppInfoError.isVisible = false
             }
         }

         viewModel.appInfo.observe(viewLifecycleOwner) { appInfo ->
            val error = viewModel.appInfoError.value
            val isLoading = viewModel.isAppInfoLoading.value ?: false

            if (!isLoading && error == null) {
                if (appInfo != null && appInfo.showDownload) {
                    binding.updateDetailsSection.isVisible = true
                    binding.itemCheckForUpdate.secondaryText.text = getString(R.string.update_available)
                    binding.textViewUpdateVersion.text = getString(R.string.version_info_format, appInfo.version ?: "N/A")
                    binding.textViewUpdateDate.text = getString(R.string.release_date_format, appInfo.buildDate ?: "N/A")
                    binding.textViewChangelog.text = appInfo.changelog ?: "No changelog available."
                    binding.buttonDownloadUpdate.setOnClickListener {
                        appInfo.downloadUrl?.let { url -> openUrl(url) }
                            ?: Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // No update available or appInfo is null after successful fetch
                    binding.updateDetailsSection.isVisible = false
                    if (appInfo != null) { // Only update text if fetch was successful but no update needed
                         binding.itemCheckForUpdate.secondaryText.text = getString(R.string.no_update_available_status)
                    } else if (error == null) { // Reset to default if appInfo is null and no error/loading
                         binding.itemCheckForUpdate.secondaryText.text = getString(R.string.check_for_update_desc)
                    }
                }
            } else if (isLoading || error != null) {
                 // Ensure details are hidden if loading or error occurred
                 binding.updateDetailsSection.isVisible = false
            }
         }

         // Observe the result message event to show toasts
         viewModel.updateCheckResultMessage.observe(viewLifecycleOwner, EventObserver { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
         })
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
