package com.example.taller2.fragments

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller2.CameraActivity
import com.example.taller2.R
import com.example.taller2.databinding.FragmentPhotoBinding
import com.example.taller2.databinding.FragmentVideoBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import java.util.logging.Logger

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [VideoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VideoFragment : Fragment() {
    private lateinit var binding: FragmentVideoBinding

    companion object {
        val TAG: String = CameraActivity::class.java.name
    }
    private val logger = Logger.getLogger(TAG)

    // Permission handler
    private val getSimplePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        updateUI(it)
    }

    private var videoViewContainer: VideoView? = null

    // Create ActivityResultLauncher instances
    private val cameraActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle camera result
            videoViewContainer!!.setVideoURI(result.data!!.data)
            videoViewContainer!!.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
            videoViewContainer!!.setMediaController(MediaController(requireContext()))
            videoViewContainer!!.start()
            videoViewContainer!!.setZOrderOnTop(true)
        }
        else {
            logger.warning("Video capture failed.")
        }
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Handle gallery result
            val videoUri: Uri? = result.data!!.data
            videoViewContainer!!.setVideoURI(videoUri)
            videoViewContainer!!.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
            videoViewContainer!!.setMediaController(MediaController(requireContext()))
            videoViewContainer!!.start()
            videoViewContainer!!.setZOrderOnTop(true)
            logger.info("Video loaded successfully")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoViewContainer = binding.videoViewContainer
        binding.takeVideoBtn.setOnClickListener {
            verifyPermissions(requireContext(), android.Manifest.permission.CAMERA, "El permiso es requerido para...")
        }
        binding.pickVideoBtn.setOnClickListener {
            val pickGalleryVideo = Intent(Intent.ACTION_PICK)
            pickGalleryVideo.type = "video/*"
            galleryActivityResultLauncher.launch(pickGalleryVideo)
        }
    }


    // Verify permission to access contacts info
    private fun verifyPermissions(context: Context, permission: String, rationale: String) {
        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                Snackbar.make(binding.root, "Ya tengo los permisos 😜", Snackbar.LENGTH_LONG).show()
                updateUI(true)
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // We display a snackbar with the justification for the permission, and once it disappears, we request it again.
                val snackbar = Snackbar.make(binding.root, rationale, Snackbar.LENGTH_LONG)
                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar, event: Int) {
                        if (event == DISMISS_EVENT_TIMEOUT) {
                            getSimplePermission.launch(permission)
                        }
                    }
                })
                snackbar.show()
            }
            else -> {
                getSimplePermission.launch(permission)
            }
        }
    }

    // Update activity behavior and actions according to result of permission request
    private fun updateUI(permission : Boolean) {
        if (permission) {
            //granted
            logger.info("Permission granted")
            dispatchTakeVideoIntent()
        } else {
            logger.warning("Permission denied")
        }
    }

    private fun dispatchTakeVideoIntent() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        // Set maximum video duration in seconds
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
        // Set video quality (0: low, 1: high)
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        cameraActivityResultLauncher.launch(takeVideoIntent)
    }
}