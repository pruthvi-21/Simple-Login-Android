package io.simplelogin.android.utils.extension

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun Fragment.copyToClipboard(label: String, text: String): Boolean =
    (activity as? AppCompatActivity)?.copyToClipboard(label, text) ?: false

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return // Fragment not attached to an Activity
    activity?.runOnUiThread(action)
}

fun applyEdgeToEdgeInsets(
    root: View,
    appBar: View? = null,
) {
    val rootInitial = root.recordInitialPadding()
    val appBarInitial = appBar?.recordInitialPadding()

    root.applySystemBarInsets(
        initialPadding = rootInitial,
        applyTop = appBar == null,
    )

    appBar?.applySystemBarInsets(
        initialPadding = appBarInitial!!,
        applyTop = true,
        applyBottom = false,
        applyStart = false,
        applyEnd = false
    )
}