package io.simplelogin.android.module.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.simplelogin.android.R
import io.simplelogin.android.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            overridePendingTransition(R.anim.screen_pop_enter_anim, R.anim.screen_pop_exit_anim)
        }
    }
}
