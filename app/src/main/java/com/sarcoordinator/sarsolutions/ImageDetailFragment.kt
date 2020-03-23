package com.sarcoordinator.sarsolutions

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.sarcoordinator.sarsolutions.util.ISharedElementFragment
import kotlinx.android.synthetic.main.fragment_image_detail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class ImageDetailFragment : Fragment(R.layout.fragment_image_detail), ISharedElementFragment {
    companion object ArgsTags {
        const val IMAGE_PATH = "IMAGE_PATH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)

        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailed_image_view.transitionName = arguments!!.getString(IMAGE_PATH)

        Glide.with(this)
            .asBitmap()
            .apply {
                RequestOptions().dontTransform()
            }
            .load(Uri.fromFile(File(arguments!!.getString(IMAGE_PATH)!!)))
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Timber.e("Error loading image")
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    CoroutineScope(Main).launch {
                        detailed_image_view.setImageBitmap(resource)
                        startPostponedEnterTransition()
                    }
                    return true
                }

            }).submit()
    }

    override fun getSharedElements(): Array<View> {
        return arrayOf(detailed_image_view)
    }
}