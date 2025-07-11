package io.simplelogin.android.module.home

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import io.simplelogin.android.R
import io.simplelogin.android.databinding.ActivityHomeBinding
import io.simplelogin.android.module.about.AboutFragment
import io.simplelogin.android.module.settings.SettingsFragment
import io.simplelogin.android.module.settings.view.AvatarView
import io.simplelogin.android.module.startup.StartupActivity
import io.simplelogin.android.utils.SLSharedPreferences
import io.simplelogin.android.utils.baseclass.BaseAppCompatActivity
import io.simplelogin.android.utils.extension.getVersionName
import io.simplelogin.android.utils.model.UserInfo

class HomeActivity : BaseAppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    enum class NavigationGraph {
        ALIAS, MAILBOX, SETTINGS, ABOUT
    }

    companion object {
        const val EMAIL = "email"
        const val USER_INFO = "userInfo"
    }

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setUpViewModel()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        binding.navigationView.setNavigationItemSelectedListener(this)
        setUpDrawer()
        setContentView(binding.root)

        if (savedInstanceState == null) {
            setNavigationGraph(viewModel.navigationGraph)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.mainDrawer.isDrawerOpen(binding.navigationView)) {
                    binding.mainDrawer.closeDrawer(binding.navigationView)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun setUpViewModel() {
        viewModel.eventUserInfoUpdated.observe(this) { updated ->
            if (updated) {
                updateHeaderView()
                viewModel.onHandleUserInfoUpdateComplete()
            }
        }
        // Retrieve UserInfo from intent
        val userInfo = intent.getParcelableExtra(USER_INFO) as? UserInfo
            ?: throw IllegalStateException("UserInfo can not be null")
        viewModel.setUserInfo(userInfo)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // In case we received a new intent is because the Connect with Proton worked
        // Refresh the user info
        if (supportFragmentManager.fragments.size == 0) return
        val navHostFragment = supportFragmentManager.fragments[0] as? NavHostFragment ?: return
        val settingsFragment = navHostFragment.childFragmentManager.fragments.find { it is SettingsFragment }
        if (settingsFragment != null) {
            val casted = settingsFragment as SettingsFragment
            casted.onNewIntent(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            overridePendingTransition(R.anim.screen_pop_enter_anim, R.anim.screen_pop_exit_anim)
        }
    }

    private fun setNavigationGraph(navigationGraph: NavigationGraph) {
        val navController = findNavController(R.id.homeNavHostFragment)

        val navOptions = navOptions {
            anim {
                enter = R.anim.screen_enter_anim
                exit = R.anim.screen_exit_anim
                popEnter = R.anim.screen_pop_enter_anim
                popExit = R.anim.screen_pop_exit_anim
            }
        }

        when (navigationGraph) {
            NavigationGraph.ALIAS -> navController.navigate(R.id.aliasListFragment)
            NavigationGraph.MAILBOX -> navController.navigate(R.id.mailboxListFragment)

            NavigationGraph.SETTINGS -> {
                navController.navigate(
                    R.id.settingsFragment,
                    bundleOf(USER_INFO to viewModel.userInfo),
                    navOptions
                )
            }

            NavigationGraph.ABOUT -> {
                navController.navigate(
                    R.id.aboutFragment,
                    bundleOf(AboutFragment.OPEN_FROM_LOGIN_ACTIVITY to false),
                    navOptions
                )
            }
        }
        viewModel.navigationGraph = navigationGraph
        binding.mainDrawer.closeDrawer(GravityCompat.START)
    }

    @SuppressLint("RtlHardcoded")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.aliasMenuItem -> setNavigationGraph(NavigationGraph.ALIAS)
            R.id.mailboxMenuItem -> setNavigationGraph(NavigationGraph.MAILBOX)
            R.id.settingsMenuItem -> setNavigationGraph(NavigationGraph.SETTINGS)
            R.id.aboutMenuItem -> setNavigationGraph(NavigationGraph.ABOUT)

            R.id.rateUsMenuItem -> {
                val uri = "market://details?id=$packageName".toUri()
                val goToMarketIntent = Intent(Intent.ACTION_VIEW, uri)

                @Suppress("MaxLineLength")
                goToMarketIntent.flags =
                    Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK

                try {
                    SLSharedPreferences.setRated(this, true)
                    startActivity(goToMarketIntent)
                } catch (_: ActivityNotFoundException) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "http://play.google.com/store/apps/details?id=$packageName".toUri()
                    )
                    startActivity(intent)
                }
            }

            R.id.signOutMenuItem -> {
                // Sign Out
                MaterialAlertDialogBuilder(this)
                    .setTitle("You will be signed out")
                    .setMessage("Please confirm")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Yes, sign me out") { _, _ -> resetSettingsAndRestartApp() }
                    .show()
            }
        }
        return true
    }

    private fun resetSettingsAndRestartApp() {
        SLSharedPreferences.reset(this)
        val intent = Intent(this, StartupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    @SuppressLint("SetTextI18n")
    private fun setUpDrawer() {
        // App version name
        val appVersionMenuItem = binding.navigationView.menu.findItem(R.id.appVersionMenuItem)
        appVersionMenuItem?.title = "SimpleLogin v${getVersionName()}"
        appVersionMenuItem.isEnabled = false

        // Header info
        updateHeaderView()

        // Define a nested function in order to reuse it later
        fun hideRateUsMenuItemIfApplicable() {
            binding.navigationView.menu.findItem(R.id.rateUsMenuItem).isVisible =
                !SLSharedPreferences.getRated(this)
        }

        hideRateUsMenuItemIfApplicable()

        binding.mainDrawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
            override fun onDrawerOpened(drawerView: View) = Unit
            override fun onDrawerStateChanged(newState: Int) = Unit

            override fun onDrawerClosed(drawerView: View) {
                hideRateUsMenuItemIfApplicable()
            }
        })
    }

    fun openDrawer() {
        binding.mainDrawer.openDrawer(GravityCompat.START)
    }

    @SuppressLint("SetTextI18n")
    private fun updateHeaderView() {
        val headerView = binding.navigationView.getHeaderView(0)

        ViewCompat.setOnApplyWindowInsetsListener(headerView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val container = headerView.findViewById<View>(R.id.container)
            container.updatePadding(top = systemBars.top)
            insets
        }
    }
}
