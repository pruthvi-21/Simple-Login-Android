package io.simplelogin.android.module.about

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.simplelogin.android.R
import io.simplelogin.android.databinding.FragmentAboutBinding
import io.simplelogin.android.module.home.HomeActivity
import io.simplelogin.android.utils.baseclass.BaseFragment
import io.simplelogin.android.utils.extension.getVersionName
import io.simplelogin.android.utils.extension.openUrlInBrowser
import io.simplelogin.android.utils.extension.startSendEmailIntent

class AboutFragment : BaseFragment(), HomeActivity.OnBackPressed {
    companion object {
        const val OPEN_FROM_LOGIN_ACTIVITY = "openFromLoginActivity"
    }

    private lateinit var binding: FragmentAboutBinding
    private var openFromLoginActivity = true

    private val navController by lazy { findNavController() }

    @SuppressLint("SetTextI18n", "LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Determine if this is opened from LoginActivity or HomeActivity
        openFromLoginActivity = arguments?.getBoolean(OPEN_FROM_LOGIN_ACTIVITY) ?: true

        binding = FragmentAboutBinding.inflate(inflater)

        binding.toolbar.setNavigationOnClickListener { finishOrNavigateUp() }

        binding.appVersionTextView.text = "SimpleLogin v${context?.getVersionName()}"
        val baseUrl = "https://simplelogin.io"

        binding.root.findViewById<View>(R.id.howTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToHowItWorksFragment()
            )
        }

        binding.root.findViewById<View>(R.id.securityTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToWebViewFragment("$baseUrl/security")
            )
        }

        binding.root.findViewById<View>(R.id.contactTextView).setOnClickListener {
            activity?.startSendEmailIntent("hi@simplelogin.io")
        }

        binding.root.findViewById<View>(R.id.whatTextView).setOnClickListener {
            navController.navigate(AboutFragmentDirections.actionAboutFragmentToWhatYouCanDoFragment())
        }

        binding.root.findViewById<View>(R.id.faqTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToFaqFragment()
            )
        }

        binding.root.findViewById<View>(R.id.teamTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToWebViewFragment("$baseUrl/about")
            )
        }

        binding.root.findViewById<View>(R.id.pricingTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToWebViewFragment("$baseUrl/pricing")
            )
        }

        binding.root.findViewById<View>(R.id.blogTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToWebViewFragment("$baseUrl/blog")
            )
        }

        binding.root.findViewById<View>(R.id.helpTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToWebViewFragment("$baseUrl/help")
            )
        }

        binding.root.findViewById<View>(R.id.roadmapTextView).setOnClickListener {
            activity?.openUrlInBrowser("https://github.com/simple-login/app/projects/1")
        }

        binding.root.findViewById<View>(R.id.termsTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToWebViewFragment("$baseUrl/terms")
            )
        }

        binding.root.findViewById<View>(R.id.privacyTextView).setOnClickListener {
            navController.navigate(
                AboutFragmentDirections.actionAboutFragmentToWebViewFragment("$baseUrl/privacy")
            )
        }

        return binding.root
    }

    private fun finishOrNavigateUp() {
        if (openFromLoginActivity) {
            activity?.finish()
        } else {
            navController.navigateUp()
        }
    }

    // HomeActivity.OnBackPressed
    override fun onBackPressed() {}
}
