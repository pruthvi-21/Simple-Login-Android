package io.simplelogin.android.module.alias.contact

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.text.color
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.simplelogin.android.R
import io.simplelogin.android.databinding.DialogViewEditTextBinding
import io.simplelogin.android.databinding.FragmentContactListBinding
import io.simplelogin.android.module.home.HomeActivity
import io.simplelogin.android.utils.LoadingFooterAdapter
import io.simplelogin.android.utils.SwipeHelper
import io.simplelogin.android.utils.baseclass.BaseFragment
import io.simplelogin.android.utils.extension.applyEdgeToEdgeInsets
import io.simplelogin.android.utils.extension.canReadContacts
import io.simplelogin.android.utils.extension.copyToClipboard
import io.simplelogin.android.utils.extension.isValidEmail
import io.simplelogin.android.utils.extension.resolveColor
import io.simplelogin.android.utils.extension.startSendEmailIntent
import io.simplelogin.android.utils.extension.toastError
import io.simplelogin.android.utils.extension.toastShortly
import io.simplelogin.android.utils.extension.toastUpToDate
import io.simplelogin.android.utils.model.Alias
import io.simplelogin.android.utils.model.Contact
import io.simplelogin.android.utils.model.PickedEmail


class ContactListFragment :
    BaseFragment(),
    HomeActivity.OnBackPressed,
    Toolbar.OnMenuItemClickListener {
    companion object {
        private const val RC_CONTACTS_ACCESS = 1000
    }

    private lateinit var binding: FragmentContactListBinding
    private lateinit var alias: Alias
    private lateinit var viewModel: ContactListViewModel
    private lateinit var contactListAdapter: ContactListAdapter
    private val footerAdapter = LoadingFooterAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Binding
        binding = FragmentContactListBinding.inflate(layoutInflater)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.setOnMenuItemClickListener(this)
        alias = ContactListFragmentArgs.fromBundle(requireArguments()).alias

        binding.emailTextField.text = alias.email
        binding.emailTextField.isSelected = true // to trigger marquee animation

        setUpViewModel()
        setUpRecyclerView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applyEdgeToEdgeInsets(binding.root, binding.appbar)
    }

    override fun onResume() {
        super.onResume()
        // On configuration change, force trigger refresh recyclerView
        if (contactListAdapter.itemCount == 0 && viewModel.contacts.isNotEmpty()) {
            contactListAdapter.submitList(viewModel.contacts)
            updateUiBaseOnNumOfContacts()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.rootConstraintLayout.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.icebergImageView.visibility =
            if (loading) View.GONE else if (viewModel.contacts.isEmpty()) View.VISIBLE else View.GONE
        binding.instructionTextView.visibility = binding.icebergImageView.visibility
    }

    private fun showLoadingFooter(showing: Boolean) {
        footerAdapter.isLoading = showing
        footerAdapter.notifyDataSetChanged()
    }

    private fun updateUiBaseOnNumOfContacts() {
        if (viewModel.contacts.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.icebergImageView.visibility = View.VISIBLE
            binding.instructionTextView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.icebergImageView.visibility = View.GONE
            binding.instructionTextView.visibility = View.GONE
        }
    }

    @Suppress("LongMethod")
    private fun setUpViewModel() {
        val tempViewModel: ContactListViewModel by viewModels {
            context?.let {
                ContactListViewModelFactory(it, alias)
            } ?: throw IllegalStateException("Context is null")
        }
        viewModel = tempViewModel
        viewModel.fetchContacts()
        setLoading(true)
        viewModel.eventHaveNewContacts.observe(viewLifecycleOwner) { haveNewContacts ->
            activity?.runOnUiThread {
                setLoading(false)
                showLoadingFooter(false)
                if (haveNewContacts) {
                    contactListAdapter.submitList(viewModel.contacts.toMutableList())
                }

                if (binding.swipeRefreshLayout.isRefreshing) {
                    context?.toastUpToDate()
                    binding.swipeRefreshLayout.isRefreshing = false
                }

                updateUiBaseOnNumOfContacts()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                setLoading(false)
                context?.toastError(error)
                viewModel.onHandleErrorComplete()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Create contact
        viewModel.eventFinishCallingCreateContact.observe(
            viewLifecycleOwner
        ) { finishedCallingCreateContact ->
            if (finishedCallingCreateContact) {
                setLoading(false)
                viewModel.onHandleFinishCallingCreateContactComplete()
            }
        }

        viewModel.eventCreatedContact.observe(viewLifecycleOwner) { createdContact ->
            if (createdContact != null) {
                context?.toastShortly("Created \"$createdContact\"")
                viewModel.refreshContacts()
                viewModel.onHandleCreatedContactComplete()
            }
        }

        // Delete contact
        viewModel.eventFinishCallingDeleteContact.observe(
            viewLifecycleOwner
        ) { finishedCallingDeleteContact ->
            if (finishedCallingDeleteContact) {
                setLoading(false)
                viewModel.onHandleFinishCallingDeleteContactComplete()
            }
        }

        viewModel.eventDeletedContact.observe(viewLifecycleOwner) { deletedContact ->
            if (deletedContact != null) {
                context?.toastShortly("Deleted \"$deletedContact\"")
                viewModel.refreshContacts()
                viewModel.onHandleDeletedContactComplete()
            }
        }

        // Toggle contact
        viewModel.eventFinishTogglingContact.observe(viewLifecycleOwner) { finishTogglingContact ->
            if (finishTogglingContact) {
                setLoading(false)
                contactListAdapter.notifyDataSetChanged()
                viewModel.onHandleToggledContactComplete()
            }
        }
    }

    private fun setUpRecyclerView() {
        contactListAdapter = ContactListAdapter(object : ContactListAdapter.ClickListener {
            override fun onClick(contact: Contact) {
                alertContactOptions(contact)
            }
        })
        binding.recyclerView.adapter = ConcatAdapter(contactListAdapter, footerAdapter)
        val linearLayoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = linearLayoutManager

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int,
            ) {
                val isPenultimateItem =
                    linearLayoutManager.findLastCompletelyVisibleItemPosition() == viewModel.contacts.size - 1
                if (isPenultimateItem && viewModel.moreToLoad) {
                    showLoadingFooter(true)
                    viewModel.fetchContacts()
                }
            }
        })

        // Add swipe recognizer to recyclerView
        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(binding.recyclerView) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                return listOf(
                    UnderlayButton(
                        requireContext(),
                        "Delete",
                        UnderlayButton.DEFAULT_TEXT_SIZE,
                        android.R.color.holo_red_light,
                        object : UnderlayButtonClickListener {
                            override fun onClick() {
                                val contact = viewModel.contacts[position]
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Delete \"${contact.email}\"?")
                                    .setMessage("\uD83D\uDED1 This operation is irreversible. Please confirm.")
                                    .setPositiveButton("Cancel", null)
                                    .setNegativeButton("Delete") { _, _ ->
                                        setLoading(true)
                                        viewModel.delete(contact)
                                    }
                                    .show()
                            }
                        }
                    )
                )
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshContacts()
        }
    }

    // HomeActivity.OnBackPressed
    override fun onBackPressed() {
    }

    // Toolbar.OnMenuItemClickListener
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.addMenuItem -> {
                if (context?.canReadContacts() == true) {
                    alertCreationOptions()
                } else {
                    showCreateContactDialog()
                }
            }

            R.id.howToMenuItem -> {
                val message = SpannableStringBuilder()
                    .append(getString(R.string.how_to_use_contacts))
                    .append("\n\n")
                    .color(requireContext().resolveColor(R.attr.colorError)) {
                        append(getString(R.string.how_to_use_contacts_warning))
                    }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("How to send email from an alias")
                    .setMessage(message)
                    .setPositiveButton("Done", null)
                    .show()
            }
        }
        return true
    }

    private fun alertCreationOptions() {
        MaterialAlertDialogBuilder(requireContext(), R.style.SlAlertDialogTheme)
            .setTitle("Create new contact")
            .setItems(
                arrayOf("Open phone contacts", "Manually enter email address")
            ) { _, itemIndex ->
                when (itemIndex) {
                    0 -> openPhoneContacts()
                    1 -> showCreateContactDialog()
                }
            }
            .show()
    }

    private fun openPhoneContacts() {
        val contactsIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(contactsIntent, RC_CONTACTS_ACCESS)
    }

    @SuppressLint("SetTextI18n")
    private fun showCreateContactDialog() {
        val editViewBinding = DialogViewEditTextBinding.inflate(LayoutInflater.from(context))
        editViewBinding.editText.hint = "Email address"
        editViewBinding.message.text =
            "Enter the email address you want to send messages to. A reverse-alias will be created so you can send emails from your alias."

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New Contact")
            .setView(editViewBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val email = editViewBinding.editText.text.toString()
                if (!email.isValidEmail()) return@setPositiveButton

                setLoading(true)
                viewModel.create(email)
            }
            .create()

        editViewBinding.editText.addTextChangedListener {
            val input = it.toString()
            val isValid = input.isValidEmail()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = isValid
            editViewBinding.editText.error =
                if (!isValid && input.isNotEmpty()) "Invalid email address" else null
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {

            }
        }

        dialog.show()
    }

    private fun showPickedEmailAddresses(contactName: String, emails: List<PickedEmail>) {
        MaterialAlertDialogBuilder(requireContext(), R.style.SlAlertDialogTheme)
            .setTitle(contactName)
            .setItems(emails.map { it.description }.toTypedArray()) { _, itemIndex ->
                val email = emails[itemIndex]
                if (email.address.isValidEmail()) {
                    viewModel.create(email.address)
                } else {
                    context?.toastShortly("Invalid email address: ${email.address}")
                }
            }
            .show()
    }

    private fun alertContactOptions(contact: Contact) {
        fun copyToClipboardAndToast(text: String) {
            copyToClipboard(text, text)
            context?.toastShortly("Copied $text")
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.SlAlertDialogTheme)
            .setTitle(contact.email)
            .setItems(
                arrayOf(
                    getString(R.string.copy_reverse_alias_with_display_name),
                    getString(R.string.copy_reverse_alias_without_display_name),
                    getString(R.string.begin_composing_with_default_email),
                    if (contact.blockForward) "Unblock" else "Block"
                )
            ) { _, itemIndex ->
                when (itemIndex) {
                    0 -> copyToClipboardAndToast(contact.reverseAlias)
                    1 -> copyToClipboardAndToast(contact.reverseAliasAddress)
                    2 -> activity?.startSendEmailIntent(contact.reverseAlias)
                    3 -> {
                        setLoading(true)
                        viewModel.toggle(contact)
                    }
                }
            }
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (resultCode == Activity.RESULT_OK && requestCode == RC_CONTACTS_ACCESS) {
            val contactData = data?.data ?: return
            val contentResolver = activity?.contentResolver ?: return
            val cursor = contentResolver.query(contactData, null, null, null, null) ?: return
            if (!cursor.moveToFirst()) return
            val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
            val emailsCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId,
                null, null
            ) ?: return
            val pickedEmails = mutableListOf<PickedEmail>()
            while (emailsCursor.moveToNext()) {
                val email = PickedEmail(emailsCursor)
                pickedEmails.add(email)
            }
            if (pickedEmails.isEmpty()) {
                context?.toastShortly("This contact has no email address")
            } else {
                showPickedEmailAddresses(name, pickedEmails)
            }
        }
    }
}
