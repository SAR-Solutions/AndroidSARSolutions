package com.sarcoordinator.sarsolutions.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sarcoordinator.sarsolutions.ImageDetailFragment
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.image_list_view_item.view.*

class ImagesAdapter(private val nav: Navigation, private var imageList: ArrayList<String>) :
    RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_list_view_item, parent, false)
        return ImageViewHolder(
            view,
            nav
        )
    }

    override fun getItemCount(): Int = imageList.count()

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bindView(imageList[position])
    }

    fun setData(imagePathList: ArrayList<String>) {
        imageList = imagePathList
        notifyDataSetChanged()
    }

    class ImageViewHolder(itemView: View, private val nav: Navigation) :
        RecyclerView.ViewHolder(itemView) {
        fun bindView(imagePath: String) {

            // Shared element transition name
            ViewCompat.setTransitionName(itemView.image_view, imagePath)

            itemView.setOnClickListener {
                val detailedFragment =
                    ImageDetailFragment()
                detailedFragment.arguments = Bundle().apply {
                    putString(ImageDetailFragment.IMAGE_PATH, imagePath)
                }

                nav.pushFragment(detailedFragment, null, itemView.image_view)
            }

            val circularProgressDrawable = CircularProgressDrawable(itemView.context)
            circularProgressDrawable.strokeWidth = 5f
            circularProgressDrawable.centerRadius = 30f
            circularProgressDrawable.start()

            Glide.with(itemView)
                .load(imagePath)
                .dontTransform()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(circularProgressDrawable)
                .into(itemView.image_view)
        }
    }
}