package io.simplelogin.android.module.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.simplelogin.android.R
import io.simplelogin.android.databinding.ActivityLoginBinding
import io.simplelogin.android.databinding.DialogViewEditTextBinding
import io.simplelogin.android.module.home.HomeActivity
import io.simplelogin.android.utils.LoginWithProtonUtils
import io.simplelogin.android.utils.SLApiService
import io.simplelogin.android.utils.SLSharedPreferences
import io.simplelogin.android.utils.baseclass.BaseAppCompatActivity
import io.simplelogin.android.utils.enums.Email
import io.simplelogin.android.utils.enums.MfaKey
import io.simplelogin.android.utils.enums.Password
import io.simplelogin.android.utils.enums.SLError
import io.simplelogin.android.utils.enums.VerificationMode
import io.simplelogin.android.utils.extension.applyEdgeToEdgeInsets
import io.simplelogin.android.utils.extension.customSetEnabled
import io.simplelogin.android.utils.extension.dismissKeyboard
import io.simplelogin.android.utils.extension.getVersionName
import io.simplelogin.android.utils.extension.isValidEmail
import io.simplelogin.android.utils.extension.resolveColor
import io.simplelogin.android.utils.extension.toastLongly
import io.simplelogin.android.utils.extension.toastShortly
import io.simplelogin.android.utils.extension.toastThrowable
import io.simplelogin.android.utils.model.UserInfo
import io.simplelogin.android.utils.model.UserLogin


class LoginActivity : BaseAppCompatActivity() {
    companion object {
        private const val RC_MFA_VERIFICATION = 0
        private const val RC_EMAIL_VERIFICATION = 1
        private const val RC_SIGN_UP = 2
    }

    private lateinit var binding: ActivityLoginBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SLSharedPreferences.reset(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeToEdgeInsets(binding.root)

        // Login
        binding.emailTextField.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                updateLoginButtonState()
            }
        })

        binding.passwordTextField.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                updateLoginButtonState()
            }
        })

        /*binding.passwordTextField.editText?.onDrawableEndTouch {
            if (binding.passwordTextField.editText?.text.isNullOrEmpty()) return@onDrawableEndTouch
            isShowingPassword = !isShowingPassword
            binding.passwordTextField.editText?.setShowPassword(isShowingPassword)
        }*/

        binding.passwordTextField.editText?.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                login()
                return@setOnKeyListener true
            }
            false
        }

        binding.loginButton.isEnabled = false // disable login button by default
        binding.loginButton.setOnClickListener { login() }

        binding.loginWithProtonButton.setOnClickListener {
            loginWithProton()
        }

        // Sign up
        binding.signUpButton.setOnClickListener {
            val signUpIntent = Intent(this, SignUpActivity::class.java)
            startActivityForResult(signUpIntent, RC_SIGN_UP)
            overridePendingTransition(R.anim.screen_enter_anim, R.anim.screen_exit_anim)
        }

        binding.forgotPasswordButton.setOnClickListener { showForgotPasswordDialog() }
        binding.apiKeyButton.setOnClickListener { showApiKeyDialog() }
        binding.changeApiUrlButton.setOnClickListener { showChangeApiUrlDialog() }

        // App version & About us
        binding.appVersionTextView.text = "SimpleLogin v${getVersionName()}"
        binding.aboutUsTextView.setOnClickListener {
            val aboutActivityIntent = Intent(this, AboutActivity::class.java)
            startActivity(aboutActivityIntent)
            overridePendingTransition(R.anim.screen_enter_anim, R.anim.screen_exit_anim)
        }

        binding.root.setOnClickListener { dismissKeyboard() }
    }

    /**
     * Callback for when the Login with Proton process is done.
     * The Login with Proton will redirect the user to
     * auth.simplelogin://callback?apikey=YOUR_API_KEY
     *
     * (The intent-filter is registered on the AndroidManifest.xml)
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val apiKey = intent?.data?.getQueryParameter("apikey")
        apiKey?.let { onApiKey(it) }
    }

    private fun showForgotPasswordDialog() {
        val dialogTextViewBinding = DialogViewEditTextBinding.inflate(layoutInflater)
        dialogTextViewBinding.editText.hint = "Email address"

        dialogTextViewBinding.message.setTextColor(resolveColor(R.attr.colorError))
        dialogTextViewBinding.message.setText(R.string.forgot_password_message)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Forgot password")
            .setView(dialogTextViewBinding.root)
            .setPositiveButton("Reset password") { _, _ ->
                val email = dialogTextViewBinding.editText.text.toString()
                if (!email.isValidEmail()) {
                    dialogTextViewBinding.editText.error = "Invalid email address"
                    return@setPositiveButton
                }

                dismissKeyboard()
                setLoading(true)
                SLApiService.forgotPassword(email) {
                    runOnUiThread {
                        setLoading(false)
                        toastLongly("We've sent reset password email to \"$email\"")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val resetButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            resetButton?.isEnabled = false

            dialogTextViewBinding.editText.addTextChangedListener {
                resetButton?.isEnabled = it.toString().isNotEmpty()
                dialogTextViewBinding.editText.error = null
            }
        }

        dialog.show()
    }

    private fun showApiKeyDialog() {
        val editTextBinding = DialogViewEditTextBinding.inflate(layoutInflater)
        editTextBinding.editText.hint = "API key"

        editTextBinding.message.setText(R.string.api_key_explanation)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Enter API key")
            .setView(editTextBinding.root)
            .setPositiveButton("Set API key") { _, _ ->
                val enteredApiKey = editTextBinding.editText.text.toString()
                setLoading(true)
                SLApiService.fetchUserInfo(enteredApiKey) { result ->
                    runOnUiThread {
                        setLoading(false)
                        SLSharedPreferences.setApiKey(this, enteredApiKey)
                        result.onSuccess { finalizeLogin(it) }
                        result.onFailure(::toastThrowable)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val setButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            setButton?.isEnabled = false

            editTextBinding.editText.addTextChangedListener {
                setButton?.isEnabled = it.toString().isNotEmpty()
            }
        }

        dialog.show()
    }

    private fun showChangeApiUrlDialog() {
        val editTextBinding = DialogViewEditTextBinding.inflate(layoutInflater)
        editTextBinding.editText.hint = "Current API URL"

        editTextBinding.message.setTextColor(resolveColor(R.attr.colorError))
        editTextBinding.message.setText(R.string.do_not_change_api_url)

        val apiUrl = SLSharedPreferences.getApiUrl(this@LoginActivity)
        editTextBinding.editText.setText(apiUrl)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Change API URL")
            .setView(editTextBinding.root)
            .setPositiveButton("Apply") { _, _ ->
                val enteredApiUrl = editTextBinding.editText.text.toString()
                setLoading(true)

                SLSharedPreferences.setApiUrl(this, enteredApiUrl)
                toastShortly("Changed API URL to: $enteredApiUrl")
                SLApiService.setUpBaseUrl(this)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Reset") { _, _ ->
                SLSharedPreferences.resetApiUrl(this)
                toastShortly("Reset API URL to: ${SLSharedPreferences.getApiUrl(this)}")
                SLApiService.setUpBaseUrl(this)
            }
            .create()

        dialog.setOnShowListener {
            val applyButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            applyButton?.isEnabled = false

            editTextBinding.editText.addTextChangedListener {
                applyButton?.isEnabled = it.toString().isNotEmpty()
            }
        }

        dialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_MFA_VERIFICATION ->
                if (resultCode == Activity.RESULT_OK) {
                    val apiKey = data?.getStringExtra(VerificationActivity.API_KEY)
                    apiKey?.let { onApiKey(it) }
                }

            RC_EMAIL_VERIFICATION ->
                if (resultCode == Activity.RESULT_OK) {
                    val verificationMode =
                        data?.getParcelableExtra<VerificationMode.AccountActivation>(
                            VerificationActivity.ACCOUNT
                        )
                    binding.emailTextField.editText?.setText(verificationMode?.email?.value)
                    binding.passwordTextField.editText?.setText(verificationMode?.password?.value)
                    login()
                }

            RC_SIGN_UP ->
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val email = data?.getStringExtra(SignUpActivity.EMAIL) ?: ""
                        val password = data?.getStringExtra(SignUpActivity.PASSWORD) ?: ""
                        signUp(email, password)
                    }

                    else -> Unit
                }

            else -> Unit
        }
    }

    private fun updateLoginButtonState() {
        val email = binding.emailTextField.editText?.text.toString()
        val password = binding.passwordTextField.editText?.text.toString()

        binding.loginButton.isEnabled = email != "" && password != ""
    }

    private fun login() {
        dismissKeyboard()

        val email = binding.emailTextField.editText?.text.toString().trim()
        val password = binding.passwordTextField.editText?.text.toString()
        val deviceName = Build.DEVICE

        if (email.isNotEmpty() && password.isNotEmpty()) {
            setLoading(true)
            SLApiService.login(email, password, deviceName) { result ->
                runOnUiThread {
                    setLoading(false)
                    result.onSuccess(::processUserLogin)
                    result.onFailure {
                        if (it is SLError && it.description == SLError.ResponseError(403).description) {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("WebAuthn currently not supported")
                                .setMessage("Please log in using API key while we are working on supporting WebAuthn on mobile.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Enter API key") { _, _ ->
                                    showApiKeyDialog()
                                }
                                .show()
                        } else {
                            toastThrowable(it)
                        }
                    }
                }
            }
        } else {
            toastShortly("Please enter both email and password")
        }
    }

    private fun loginWithProton() {
        dismissKeyboard()
        LoginWithProtonUtils.launchLoginWithProton(this)
    }

    private fun setLoading(loading: Boolean) {
        binding.rootLinearLayout.customSetEnabled(!loading)
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun processUserLogin(userLogin: UserLogin) {
        when (userLogin.mfaEnabled) {
            true -> userLogin.mfaKey?.let {
                startVerificationActivity(VerificationMode.Mfa(MfaKey(it)))
            }

            false -> userLogin.apiKey?.let { onApiKey(userLogin.apiKey) }
        }
    }

    private fun onApiKey(apiKey: String) {
        SLSharedPreferences.setApiKey(this, apiKey)
        SLApiService.fetchUserInfo(apiKey) { result ->
            result.onSuccess(::finalizeLogin)
            result.onFailure(::toastThrowable)
        }
    }

    private fun finalizeLogin(userInfo: UserInfo) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra(HomeActivity.USER_INFO, userInfo)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.screen_enter_anim, R.anim.screen_exit_anim)
        finish()
    }

    private fun signUp(email: String, password: String) {
        setLoading(true)

        SLApiService.signUp(email, password) { result ->
            runOnUiThread {
                setLoading(false)

                result.onSuccess {
                    toastLongly("Check your inbox for verification code")
                    val mode = VerificationMode.AccountActivation(Email(email), Password(password))
                    startVerificationActivity(mode)
                }

                result.onFailure(::toastThrowable)
            }
        }
    }

    private fun startVerificationActivity(verificationMode: VerificationMode) {
        val verificationIntent = Intent(this, VerificationActivity::class.java)

        when (verificationMode) {
            is VerificationMode.Mfa -> {
                verificationIntent.putExtra(
                    VerificationActivity.MFA_MODE,
                    verificationMode
                )

                startActivityForResult(verificationIntent, RC_MFA_VERIFICATION)
            }

            is VerificationMode.AccountActivation -> {
                verificationIntent.putExtra(
                    VerificationActivity.ACCOUNT_ACTIVATION_MODE,
                    verificationMode
                )

                startActivityForResult(verificationIntent, RC_EMAIL_VERIFICATION)
            }
        }

        overridePendingTransition(R.anim.screen_enter_anim, R.anim.screen_exit_anim)
    }
}
