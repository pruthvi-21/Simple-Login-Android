package io.simplelogin.android.utils.extension

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.simplelogin.android.R
import io.simplelogin.android.utils.InitialPadding

@Suppress("MagicNumber")
fun View.customSetEnabled(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1.0f else 0.5f
}

fun View.makeSubviewsClippedToBound() {
    outlineProvider = ViewOutlineProvider.BACKGROUND
    clipToOutline = true
}

fun View.shake() = startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))

@Suppress("MagicNumber")
fun View.fadeOut() {
    val valueAnimator = ValueAnimator.ofFloat(1f, 0f)
    valueAnimator.addUpdateListener { alpha = it.animatedValue as Float }
    valueAnimator.duration = 200
    valueAnimator.interpolator = LinearInterpolator()
    valueAnimator.start()
}

fun View.recordInitialPadding(): InitialPadding {
    return InitialPadding(paddingStart, paddingTop, paddingEnd, paddingBottom)
}

fun View.applySystemBarInsets(
    initialPadding: InitialPadding,
    applyTop: Boolean = true,
    applyBottom: Boolean = true,
    applyStart: Boolean = true,
    applyEnd: Boolean = true,
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        setPaddingRelative(
            if (applyStart) initialPadding.start + systemBars.left else initialPadding.start,
            if (applyTop) initialPadding.top + systemBars.top else initialPadding.top,
            if (applyEnd) initialPadding.end + systemBars.right else initialPadding.end,
            if (applyBottom) initialPadding.bottom + systemBars.bottom else initialPadding.bottom
        )

        insets
    }
}