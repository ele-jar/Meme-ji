package com.example.memesji.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memesji.R
import com.example.memesji.databinding.FragmentCategoriesBinding
import com.example.memesji.ui.adapter.CategoryAdapter
import com.example.memesji.viewmodel.MemeViewModel

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemeViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { categoryName ->
            val action = CategoriesFragmentDirections.actionCategoriesFragmentToCategoryMemesFragment(categoryName)
            findNavController().navigate(action)
        }

        // Adjust span count based on orientation
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        layoutManager = GridLayoutManager(context, spanCount)

        binding.recyclerViewCategories.apply {
            adapter = categoryAdapter
            layoutManager = this@CategoriesFragment.layoutManager
            setHasFixedSize(true) // Improve performance if item size doesn't change
            // Add item decoration for spacing if needed (e.g., using GridSpacingItemDecoration)
        }
    }

    private fun observeViewModel() {
        viewModel.filteredCategories.observe(viewLifecycleOwner) { categories ->
            val isLoading = viewModel.isLoading.value ?: false
            val error = viewModel.error.value
            val hasData = !categories.isNullOrEmpty()

            binding.progressBarCategories.isVisible = isLoading && !hasData
            binding.textViewNoCategories.isVisible = !isLoading && !hasData && error == null
            binding.textViewErrorCategories.isVisible = !isLoading && error != null
            binding.recyclerViewCategories.isVisible = hasData && error == null

            categoryAdapter.submitList(categories)

            if (binding.textViewNoCategories.isVisible) {
                 updateNoCategoriesText()
            }
            if (binding.textViewErrorCategories.isVisible) {
                binding.textViewErrorCategories.text = error ?: getString(R.string.unknown_error)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
             val hasData = !(viewModel.filteredCategories.value.isNullOrEmpty())
             val error = viewModel.error.value
             binding.progressBarCategories.isVisible = isLoading && !hasData
             // Only show error/empty text when *not* loading, unless there's already data
             if (!isLoading) {
                 binding.textViewNoCategories.isVisible = !hasData && error == null
                 binding.textViewErrorCategories.isVisible = error != null
                 binding.recyclerViewCategories.isVisible = hasData && error == null
                 if(binding.textViewNoCategories.isVisible) updateNoCategoriesText()
                 if(binding.textViewErrorCategories.isVisible) binding.textViewErrorCategories.text = error ?: getString(R.string.unknown_error)
             } else {
                 // While loading, hide error/empty text if there's no data yet
                 if (!hasData) {
                     binding.textViewNoCategories.isVisible = false
                     binding.textViewErrorCategories.isVisible = false
                 }
             }
        }

         // Observe error specifically to potentially show error view even if list has stale data
         viewModel.error.observe(viewLifecycleOwner) { error ->
             val isLoading = viewModel.isLoading.value ?: false
             val hasData = !(viewModel.filteredCategories.value.isNullOrEmpty())
             val showErrorView = error != null && !isLoading

             binding.textViewErrorCategories.isVisible = showErrorView
             binding.recyclerViewCategories.isVisible = !showErrorView || hasData // Show list if error but has old data
             binding.progressBarCategories.isVisible = isLoading && !hasData // Hide progress if error occurred

             if (showErrorView) {
                 binding.textViewErrorCategories.text = error ?: getString(R.string.unknown_error)
                 binding.textViewNoCategories.isVisible = false // Don't show both error and empty
             } else if (!isLoading && !hasData) {
                 binding.textViewNoCategories.isVisible = true
                 updateNoCategoriesText()
             }
         }
    }

     private fun updateNoCategoriesText() {
         val query = viewModel.categorySearchQuery.value
         binding.textViewNoCategories.text = if (query.isNullOrBlank()) {
             getString(R.string.no_categories_found)
         } else {
             getString(R.string.no_categories_match_search, query)
         }
     }

    // Handle configuration changes like orientation
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val spanCount = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        layoutManager.spanCount = spanCount
        // Optional: notify adapter if layout needs drastic changes, though GridLayoutManager handles span changes well.
        // categoryAdapter.notifyItemRangeChanged(0, categoryAdapter.itemCount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Avoid memory leaks by nulling adapter
        binding.recyclerViewCategories.adapter = null
        _binding = null
    }
}
