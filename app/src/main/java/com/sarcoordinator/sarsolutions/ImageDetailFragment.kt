package com.sarcoordinator.sarsolutions

import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.ISharedElementFragment
import kotlinx.android.synthetic.main.fragment_image_detail.*
import timber.log.Timber
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

        upload_test_button.setOnClickListener {
            upload_test_button.isEnabled = false
            val uriFile = Uri.fromFile(imageFile)
            val storage = FirebaseStorage.getInstance().reference
            val ref = storage.child(viewModel.currentCase.value!!.id)
                .child("images/${uriFile.lastPathSegment}")
            val uploadTask = ref.putBytes(
                GlobalUtil.fixImageOrientation(
                    arguments!!.getString(IMAGE_PATH)!!,
                    image.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                )
            )

            uploadTask.addOnSuccessListener {
                Timber.d("Image was uploaded successfully")
                Toast.makeText(requireContext(), "Successfully uploaded image", Toast.LENGTH_LONG)
                    .show()
                Timber.d("Image delete result: ${imageFile.delete()}")
                requireActivity().onBackPressed()
            }.addOnFailureListener {
                upload_test_button.isEnabled = true
                Timber.e("Image was not uploaded")
                Toast.makeText(requireContext(), "Error uploading image", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun getSharedElement(): View? {
        return detailed_image_view
    }
}