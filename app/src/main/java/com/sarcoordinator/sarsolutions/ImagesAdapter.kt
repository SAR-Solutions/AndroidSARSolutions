package com.sarcoordinator.sarsolutions

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.image_list_view_item.view.*

class ImagesAdapter : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {
    private val imageList = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_list_view_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = imageList.count()

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bindView(imageList[position])
    }

    fun addImagePath(imagePath: String) {
        imageList.add(imagePath)
        notifyItemInserted(imageList.size - 1)
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(imagePath: String) {
            itemView.image_view.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        }
    }
}