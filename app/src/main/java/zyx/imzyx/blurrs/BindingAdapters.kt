package zyx.imzyx.blurrs

import android.view.View
import androidx.databinding.BindingAdapter

/**
 * Created by holmes on 19-4-16.
 */

@BindingAdapter("isGone")
fun bindIsGone(view: View, isGone: Boolean) {
    view.visibility = if (isGone) {
        View.GONE
    } else {
        View.VISIBLE
    }
}