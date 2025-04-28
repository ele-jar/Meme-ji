package com.example.memesji.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.memesji.R
import com.example.memesji.data.Meme
import com.example.memesji.databinding.ItemMemeBinding

class MemeAdapter(
    private val onItemClicked: (Meme) -> Unit
) : ListAdapter<Meme, MemeAdapter.MemeViewHolder>(MemeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemeViewHolder {
        val binding = ItemMemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemeViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: MemeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemeViewHolder(
        private val binding: ItemMemeBinding,
        private val onItemClicked: (Meme) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meme: Meme) {
            Glide.with(binding.imageViewMeme.context)
                .load(meme.url)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(DrawableTransitionOptions.withCrossFade())
                .fitCenter()
                .into(binding.imageViewMeme)

            binding.root.setOnClickListener {
                onItemClicked(meme)
            }
            binding.root.isClickable = true
        }
    }

    class MemeDiffCallback : DiffUtil.ItemCallback<Meme>() {
        override fun areItemsTheSame(oldItem: Meme, newItem: Meme): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: Meme, newItem: Meme): Boolean {
            return oldItem == newItem
        }
    }
}
