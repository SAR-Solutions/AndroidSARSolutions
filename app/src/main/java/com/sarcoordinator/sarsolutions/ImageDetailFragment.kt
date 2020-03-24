package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sarcoordinator.sarsolutions.util.ISharedElementFragment
import kotlinx.android.synthetic.main.fragment_image_detail.*
import java.io.File

class ImageDetailFragment : Fragment(R.layout.fragment_image_detail), ISharedElementFragment {
    companion object ArgsTags {
        const val IMAGE_PATH = "IMAGE_PATH"
    }

    private lateinit var viewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)

        (requireActivity() as MainActivity).enableTransparentStatusBar(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        (requireActivity() as MainActivity).enableTransparentStatusBar(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailed_image_view.transitionName = arguments!!.getString(IMAGE_PATH)

        val imageFile = File(arguments!!.getString(IMAGE_PATH))
        val image = ExifInterface(arguments!!.getString(IMAGE_PATH)!!)
        image_title.text = imageFile.name
        image_timestamp.text = image.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)

        Glide.with(this)
            .load(imageFile)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(detailed_image_view)
    }

    override fun getSharedElement(): View? {
        return detailed_image_view
    }
}