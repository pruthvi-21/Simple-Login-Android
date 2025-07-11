package io.simplelogin.android.module.alias.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.simplelogin.android.R
import io.simplelogin.android.databinding.FragmentAliasCreateBinding
import io.simplelogin.android.module.alias.AliasListViewModel
import io.simplelogin.android.utils.SLApiService
import io.simplelogin.android.utils.SLSharedPreferences
import io.simplelogin.android.utils.baseclass.BaseFragment
import io.simplelogin.android.utils.extension.applyEdgeToEdgeInsets
import io.simplelogin.android.utils.extension.dismissKeyboard
import io.simplelogin.android.utils.extension.isValidEmailPrefix
import io.simplelogin.android.utils.extension.showSelectMailboxesAlert
import io.simplelogin.android.utils.extension.toastError
import io.simplelogin.android.utils.extension.toastShortly
import io.simplelogin.android.utils.extension.toastThrowable
import io.simplelogin.android.utils.model.Alias
import io.simplelogin.android.utils.model.toSpannableString

class AliasCreateFragment : BaseFragment() {
    private lateinit var binding: FragmentAliasCreateBinding
    private val aliasListViewModel: AliasListViewModel by activityViewModels()
    private var selectedSuffix: String? = null
    private lateinit var viewModel: AliasCreateViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAliasCreateBinding.inflate(inflater)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Enable/disable createButton
        binding.prefixEditText.doOnTextChanged { text, _, _, _ ->
            val isValid = text?.toString()?.isValidEmailPrefix() ?: false
            binding.createButton.isEnabled = isValid
            if (isValid || text?.toString()?.isEmpty() == true) {
                binding.prefixTextInputLayout.isErrorEnabled = false
                binding.prefixTextInputLayout.error = null
            } else {
                binding.prefixTextInputLayout.isErrorEnabled = true
                binding.prefixTextInputLayout.error = getString(R.string.create_alias_warning)
            }
        }

        binding.createButton.setOnClickListener {
            if (selectedSuffix == null) {
                context?.toastShortly("No suffix is selected")
                return@setOnClickListener
            }
            createAlias()
        }

        binding.mailboxesTitleLinearLayout.setOnClickListener { showSelectMailboxesAlert() }
        binding.mailboxesTextView.setOnClickListener { showSelectMailboxesAlert() }

        binding.root.setOnClickListener { activity?.dismissKeyboard() }

        // viewModel
        viewModel = AliasCreateViewModel(requireContext())

        setLoading(true)
        viewModel.fetchUserOptionsAndMailboxes()

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                context?.toastError(error)
                findNavController().navigateUp()
            }
        }

        viewModel.userOptions.observe(viewLifecycleOwner) { userOptions ->
            if (userOptions != null) {
                setLoading(false)

                if (userOptions.canCreate) {
                    setUpSuffixesSpinner(userOptions.suffixes.map { it.suffix })
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Can not create more alias")
                        .setMessage("Go premium for unlimited aliases and more.")
                        .setPositiveButton("See pricing", null)
                        .setOnDismissListener {
                            aliasListViewModel.setNeedsSeePricing()
                            findNavController().navigateUp()
                        }
                        .show()
                }
            }
        }

        viewModel.selectedMailboxes.observe(viewLifecycleOwner) { selectedMailboxes ->
            if (selectedMailboxes != null) {
                setLoading(false)
                binding.mailboxesTextView.setText(
                    selectedMailboxes.toSpannableString(requireContext()),
                    TextView.BufferType.SPANNABLE
                )
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applyEdgeToEdgeInsets(binding.root, binding.appbar)
    }

    private fun updateAliasListViewModelAndNavigateUp(alias: Alias) {
        if (AliasCreateFragmentArgs.fromBundle(requireArguments()).isMailFromAlias) {
            aliasListViewModel.setMailFromAlias(alias)
        } else {
            context?.toastShortly("Created \"${alias.email}\"")
        }
        aliasListViewModel.addAlias(alias)
        findNavController().navigateUp()
    }

    private fun setUpSuffixesSpinner(suffixes: List<String>) {
        binding.suffixesDropdown.setAdapter(AliasCreateSpinnerAdapter(requireContext(), suffixes))
        binding.suffixesDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedSuffix = suffixes[position]
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            binding.rootLinearLayout.visibility = View.GONE
            binding.createButton.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.rootLinearLayout.visibility = View.VISIBLE
            binding.createButton.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun createAlias() {
        val apiKey = SLSharedPreferences.getApiKey(requireContext()) ?: throw IllegalStateException(
            "API key is null"
        )

        val signedSuffix = viewModel.userOptions.value!!.suffixes.first { it.suffix == selectedSuffix }.signedSuffix
        setLoading(true)

        SLApiService.createAlias(
            apiKey,
            binding.prefixEditText.text.toString(),
            signedSuffix,
            viewModel.selectedMailboxes.value!!.map { it.id },
            binding.nameEditText.text.toString(),
            binding.noteEditText.text.toString()
        ) { result ->
            activity?.runOnUiThread {
                setLoading(false)
                result.onSuccess(::updateAliasListViewModelAndNavigateUp)
                result.onFailure { context?.toastThrowable(it) }
            }
        }
    }

    private fun showSelectMailboxesAlert() {
        viewModel.selectedMailboxes.value?.let { selectedMailboxes ->
            activity?.showSelectMailboxesAlert(
                viewModel.mailboxes,
                selectedMailboxes
            ) { checkedMailboxes ->
                viewModel.setSelectedMailboxes(checkedMailboxes)
            }
        }
    }
}
