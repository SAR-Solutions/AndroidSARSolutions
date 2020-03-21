package com.sarcoordinator.sarsolutions

import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.image_list_view_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

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

            Glide.with(itemView)
                .asBitmap()
                .load(Uri.fromFile(File(imagePath)))
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Toast.makeText(
                            itemView.context,
                            "Something went wrong while loading image.",
                            Toast.LENGTH_LONG
                        ).show()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        CoroutineScope(Dispatchers.Main).launch {
                            itemView.image_progress_bar.visibility = View.GONE
                            itemView.image_view.setImageBitmap(resource)
                        }
                        return true
                    }

                }).submit()
//            itemView.image_view.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        }
    }
}