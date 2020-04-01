package com.cainwong.eskrimacounterstimer.ui

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter

object BindingAdapters {
    @JvmStatic
    @BindingAdapter(value = ["android:src"])
    fun setImageDrawable(imageView: ImageView, @DrawableRes resId: Int?) {
        if (resId != null) {
            imageView.setImageDrawable(imageView.resources.getDrawable(resId))
        }
    }
}