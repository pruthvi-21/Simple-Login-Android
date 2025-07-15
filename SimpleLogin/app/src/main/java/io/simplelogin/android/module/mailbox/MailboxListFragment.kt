package io.simplelogin.android.module.mailbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.simplelogin.android.R
import io.simplelogin.android.databinding.DialogViewEditTextBinding
import io.simplelogin.android.databinding.FragmentMailboxListBinding
import io.simplelogin.android.utils.baseclass.BaseFragment
import io.simplelogin.android.utils.extension.applyEdgeToEdgeInsets
import io.simplelogin.android.utils.extension.toastError
import io.simplelogin.android.utils.extension.toastLongly
import io.simplelogin.android.utils.extension.toastUpToDate
import io.simplelogin.android.utils.model.Mailbox

class MailboxListFragment :
    BaseFragment(),
    Toolbar.OnMenuItemClickListener {

    private lateinit var binding: FragmentMailboxListBinding
    private lateinit var viewModel: MailboxListViewModel
    private lateinit var adapter: MailboxListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setUpBinding()
        setUpViewModel()
        setUpRecyclerView()

        setLoading(true)
        viewModel.fetchMailboxes()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applyEdgeToEdgeInsets(binding.root, binding.appbar)
    }

    private fun setLoading(loading: Boolean) {
        binding.rootConstraintLayout.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun setUpBinding() {
        binding = FragmentMailboxListBinding.inflate(layoutInflater)
        binding.toolbar.setNavigationOnClickListener { showLeftMenu() }
        binding.toolbar.setOnMenuItemClickListener(this)
    }

    private fun setUpViewModel() {
        viewModel = MailboxListViewModel(requireContext())

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                setLoading(false)
                context?.toastError(error)
                viewModel.onHandleErrorComplete()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.eventUpdateMailboxes.observe(viewLifecycleOwner) { haveNewMailboxes ->
            activity?.runOnUiThread {
                setLoading(false)

                if (haveNewMailboxes) {
                    // toMutableList() is required. Refer to AliasListFragment viewModel
                    adapter.submitList(viewModel.mailboxes.toMutableList())
                    viewModel.onHandleUpdateMailboxesComplete()
                }

                if (binding.swipeRefreshLayout.isRefreshing) {
                    context?.toastUpToDate()
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }

        viewModel.createdMailbox.observe(viewLifecycleOwner) { createdMailbox ->
            activity?.runOnUiThread {
                setLoading(false)
                if (createdMailbox != null) {
                    context?.toastLongly("You are going to receive a confirmation email for $createdMailbox")
                    viewModel.onHandleCreatedMailboxComplete()
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        adapter = MailboxListAdapter(object : MailboxListAdapter.ClickListener {
            override fun onSetAsDefault(mailbox: Mailbox, onActionDone: () -> Unit) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Please confirm")
                    .setMessage("Make \"${mailbox.email}\" default mailbox?")
                    .setPositiveButton("Confirm") { _, _ ->
                        setLoading(true)
                        viewModel.makeDefault(mailbox)
                        onActionDone()
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        onActionDone()
                    }
                    .show()
            }

            override fun onDelete(mailbox: Mailbox, onActionDone: () -> Unit) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete \"${mailbox.email}\"")
                    .setMessage(R.string.warning_before_deleting_mailbox)
                    .setNegativeButton("Delete") { _, _ ->
                        setLoading(true)
                        viewModel.deleteMailbox(mailbox)
                        onActionDone()
                    }
                    .setPositiveButton("Cancel") { _, _ ->
                        onActionDone()
                    }
                    .show()
            }
        })
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.fetchMailboxes() }
    }

    // Toolbar.OnMenuItemClickListener
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.addMenuItem -> {
                val dialogTextViewBinding = DialogViewEditTextBinding.inflate(layoutInflater)
                dialogTextViewBinding.editText.hint = "my-another-email@example.com"
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("New mailbox")
                    .setMessage("A verification email will be sent to this email address")
                    .setView(dialogTextViewBinding.root)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Create") { _, _ ->
                        setLoading(true)
                        viewModel.create(dialogTextViewBinding.editText.text.toString())
                    }
                    .show()
            }

            R.id.howToMenuItem -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("How to use Mailboxes")
                    .setMessage(R.string.how_to_use_mailbox)
                    .setPositiveButton("Done", null)
                    .show()
            }
        }

        return true
    }
}
