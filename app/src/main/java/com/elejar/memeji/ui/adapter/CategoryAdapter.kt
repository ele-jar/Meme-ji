package com.elejar.memeji.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.elejar.memeji.R
import com.elejar.memeji.data.CategoryItem
import com.elejar.memeji.databinding.ItemCategoryBinding // Assuming you update the layout name

class CategoryAdapter(
    private val onCategoryClick: (String) -> Unit
) : ListAdapter<CategoryItem, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        // Inflate the updated item layout
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding, onCategoryClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder remains largely the same, but uses the updated binding
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
        private val onCategoryClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryItem: CategoryItem) {
            binding.textViewCategoryName.text = categoryItem.name

            // Improved Glide loading with placeholder and error handling
            Glide.with(binding.imageViewCategoryImage.context)
                .load(categoryItem.imageUrl)
                .placeholder(R.drawable.ic_placeholder_image) // Use a consistent placeholder
                .error(R.drawable.ic_placeholder_image) // Use a consistent error drawable
                .centerCrop() // Or fitCenter() depending on desired look
                .transition(DrawableTransitionOptions.withCrossFade()) // Fade animation
                .into(binding.imageViewCategoryImage)

            // Set click listener on the root view (CardView or ConstraintLayout)
            binding.root.setOnClickListener {
                onCategoryClick(categoryItem.name)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem.name == newItem.name // Name is the unique identifier
        }

        override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem == newItem // Compare all fields for content changes
        }
    }
}
