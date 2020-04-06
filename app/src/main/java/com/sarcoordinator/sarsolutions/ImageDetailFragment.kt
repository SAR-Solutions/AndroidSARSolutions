package com.sarcoordinator.sarsolutions

import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storageMetadata
import com.sarcoordinator.sarsolutions.util.CustomFragment
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.fragment_image_detail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ImageDetailFragment : Fragment(R.layout.fragment_image_detail), CustomFragment {
    companion object ArgsTags {
        const val IMAGE_PATH = "IMAGE_PATH"
    }

    private lateinit var viewModel: SharedViewModel
    private lateinit var imageFile: File
    private lateinit var image: ExifInterface
    private val nav: Navigation by lazy { Navigation.getInstance() }

    override fun getSharedElement(): View = detailed_image_view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)

        (requireActivity() as MainActivity).enableTransparentStatusBar(true)
        nav.hideBottomNavBar?.let { it(true) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imagePath =
            arguments?.getString(IMAGE_PATH) ?: savedInstanceState?.getString(IMAGE_PATH)!!

        detailed_image_view.transitionName = imagePath
        imageFile = File(imagePath)
        image = ExifInterface(imagePath)
        image_title.text = imageFile.name
        image_timestamp.text = image.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)

        setupMetaDataEditText()
        setupUploadButton()
        setupDeleteButton()

        Glide.with(this)
            .load(imageFile)
            .dontTransform()
            .centerCrop()
            .into(detailed_image_view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(IMAGE_PATH, imageFile.absolutePath)
    }

    private fun setupMetaDataEditText() {
        // Load image metadata
        metadata_content.setText(
            image.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION),
            TextView.BufferType.EDITABLE
        )

        // Save metadata on send click
        metadata_content.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    GlobalUtil.hideKeyboard(requireActivity())
                    image.setAttribute(
                        ExifInterface.TAG_IMAGE_DESCRIPTION,
                        metadata_content.text.toString()
                    )
                    image.saveAttributes()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupUploadButton() {
        upload_button.setOnClickListener {
            upload_button.isEnabled = false

            CoroutineScope(Default).launch {
                val uriFile = Uri.fromFile(imageFile)
                val storage = FirebaseStorage.getInstance().reference
                val ref = storage.child(viewModel.currentCase.value!!.id)
                    .child("images/${uriFile.lastPathSegment}")

                val testMetadata = storageMetadata {
                    setCustomMetadata(
                        "Description",
                        image.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
                    )
                }

                withContext(IO) {
                    viewModel.isUploadTaskActive = true
                    val uploadTask = ref.putBytes(
                        GlobalUtil.fixImageOrientation(
                            arguments!!.getString(IMAGE_PATH)!!,
                            image.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                        ),
                        testMetadata
                    )

                    uploadTask.addOnSuccessListener {
                        Timber.d("Image was uploaded successfully")
                        CoroutineScope(Main).launch {
                            Toast.makeText(
                                requireContext(),
                                "Successfully uploaded image",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            viewModel.isUploadTaskActive = false
                            deleteImageFile()
                        }
                    }.addOnFailureListener {
                        CoroutineScope(Main).launch {
                            upload_button?.isEnabled = true
                            Timber.e("Image was not uploaded")
                            Toast.makeText(
                                requireContext(),
                                "Error uploading image",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }.addOnCompleteListener {
                        viewModel.isUploadTaskActive = false
                    }
                }

            }
        }
    }

    private fun setupDeleteButton() {
        delete_button.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_image))
                .setMessage(getString(R.string.delete_confirmation))
                .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                    dialog.dismiss()
                    deleteImageFile()
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun deleteImageFile() {
        Timber.d("Image delete result: ${imageFile.delete()}")
        requireActivity().onBackPressed()
    }
}