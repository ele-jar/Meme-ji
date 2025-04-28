package com.example.memesji.ui

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.memesji.R
import com.example.memesji.data.Meme
import com.example.memesji.databinding.ActivityMainBinding
import com.example.memesji.ui.fragments.CategoryMemesFragment
import com.example.memesji.viewmodel.MemeViewModel
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val viewModel: MemeViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("Permission", "Storage permission granted")
                viewModel.executePendingDownloadAction()

            } else {
                Log.e("Permission", "Storage permission denied")
                Snackbar.make(binding.root, getString(R.string.permission_denied_message), Snackbar.LENGTH_LONG).show()
                 viewModel.clearPendingDownloadAction()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.categoriesFragment,
                R.id.moreFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val isTopLevel = appBarConfiguration.topLevelDestinations.contains(destination.id)
            val isCategoryMemes = destination.id == R.id.categoryMemesFragment

            binding.toolbar.isVisible = isTopLevel || isCategoryMemes

            if (isCategoryMemes) {
                val categoryName = arguments?.getString("categoryName")
                supportActionBar?.title = categoryName ?: getString(R.string.title_category_memes)
            } else if (!isTopLevel) {

            }


             invalidateMenu()
        }

        setupMenuProvider()
        observeViewModel()
    }

    private fun setupMenuProvider() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val downloadCategoryItem = menu.findItem(R.id.action_download_category)
                val searchView = searchItem?.actionView as? SearchView

                val currentDestinationId = navController.currentDestination?.id

                searchItem?.isVisible = currentDestinationId == R.id.homeFragment ||
                        currentDestinationId == R.id.categoryMemesFragment ||
                        currentDestinationId == R.id.categoriesFragment


                downloadCategoryItem?.isVisible = currentDestinationId == R.id.categoryMemesFragment


                searchView?.queryHint = when (currentDestinationId) {
                    R.id.categoriesFragment -> getString(R.string.search_categories_hint)
                    else -> getString(R.string.search_hint)
                }

                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        handleSearch(query)
                        searchView.clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        handleSearch(newText)
                        return true
                    }
                })

                searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        handleSearch(null)
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

                 if (menuItem.itemId == R.id.action_download_category) {
                     val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                     val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                     if (currentFragment is CategoryMemesFragment) {
                         currentFragment.showCategoryDownloadOptions()
                     } else {
                         Log.e("MainActivity", "Could not find CategoryMemesFragment to trigger download options.")
                     }
                     return true
                 }

                return false
            }
        }, this, Lifecycle.State.RESUMED)
    }


    fun requestStoragePermission(actionToRun: () -> Unit) {
         viewModel.setPendingDownloadAction(actionToRun)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("Permission", "Storage permission already granted (pre-Q)")
                     viewModel.executePendingDownloadAction()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.permission_rationale),
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(getString(R.string.ok)) {
                            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        .show()
                     viewModel.clearPendingDownloadAction()
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        } else {
            Log.i("Permission", "No legacy WRITE permission needed for Android 10+ for Downloads.")
             viewModel.executePendingDownloadAction()
        }
    }


    private fun handleSearch(query: String?) {
        val currentDestinationId = navController.currentDestination?.id
        when (currentDestinationId) {
            R.id.homeFragment, R.id.categoryMemesFragment -> viewModel.setSearchQuery(query)
            R.id.categoriesFragment -> viewModel.setCategorySearchQuery(query)
            else -> {
                 viewModel.setSearchQuery(null)
                 viewModel.setCategorySearchQuery(null)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun downloadMeme(meme: Meme?) {
        if (meme == null) {
            Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show()
            return
        }

        requestStoragePermission {
            viewModel.downloadMeme(meme)
        }
    }

    fun makeFilenameSafe(input: String): String {
        val pattern = Pattern.compile("[^a-zA-Z0-9-_\\.]")
        val safe = pattern.matcher(input).replaceAll("_")
        return safe.take(100)
    }

    private fun observeViewModel() {
        viewModel.singleMemeDownloadStatus.observe(this) { status ->
            status?.let {
                 Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                 viewModel.clearSingleMemeDownloadStatus() 
            }
        }
        
    }

    fun openUrlInBrowser(url: String?) {
        if (url == null) {
            Toast.makeText(this, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e("MainActivity", "No browser found to handle URL: $url", e)
            Toast.makeText(this, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Could not open URL: $url", e)
            Toast.makeText(this, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
        }
    }
}
