package com.example.taller2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.taller2.databinding.ActivityCameraBinding
import com.example.taller2.fragments.PhotoFragment
import com.example.taller2.fragments.VideoFragment

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, PhotoFragment())
            .commit()

        binding.toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.toggle.text = getString(R.string.video_opt)
                supportFragmentManager.beginTransaction()
                    .replace(binding.container.id, VideoFragment())
                    .commit()
            } else {
                binding.toggle.text = getString(R.string.photo_opt)
                supportFragmentManager.beginTransaction()
                    .replace(binding.container.id, PhotoFragment())
                    .commit()
            }
        }
    }
}