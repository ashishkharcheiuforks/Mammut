package io.github.jamiesanson.mammut.extension

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import io.github.jamiesanson.mammut.MammutApplication
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.dagger.application.ApplicationComponent
import kotlinx.android.synthetic.main.design_layout_snackbar_include.view.*
import org.jetbrains.anko.contentView
import androidx.annotation.ColorInt
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.Controller
import org.jetbrains.anko.textColor


val AppCompatActivity.mammutApplication: MammutApplication
    get() = application as MammutApplication

val AppCompatActivity.applicationComponent: ApplicationComponent
    get() = mammutApplication.component

inline fun <reified T: ViewModel> AppCompatActivity.provideViewModel(viewModelFactory: MammutViewModelFactory): T =
        ViewModelProviders.of(this, viewModelFactory)[T::class.java]

fun Activity.snackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
    val typedValue = TypedValue()
    theme.resolveAttribute(R.attr.colorPrimaryLight, typedValue, true)
    @ColorInt val primaryDarkColor = typedValue.data

    theme.resolveAttribute(R.attr.colorAccentLight, typedValue, true)
    @ColorInt val lightAccentColor = typedValue.data

    Snackbar.make(contentView!!, message, length).apply {
        view.background.setTint(primaryDarkColor)
        view.snackbar_text.textColor = lightAccentColor
    }.show()
}

fun Fragment.snackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
    requireActivity().snackbar(message, length)
}

fun Controller.snackbar(message: String, length: Int = Snackbar.LENGTH_LONG) {
    activity!!.snackbar(message, length)
}

/**
 * Helper function for allowing simple ViewHolder view inflation
 */
internal fun ViewGroup.inflate(@LayoutRes resource: Int): View =
        LayoutInflater.from(context).inflate(resource, this, false)

val RecyclerView.ViewHolder.lifecycleOwner: LifecycleOwner
    get() = itemView.context as LifecycleOwner

/**
 * Conductor extensions
 */
inline fun <reified T> Controller.startActivity(finishCurrent: Boolean = false) =
        startActivity(Intent(activity, T::class.java)).also {
            if (finishCurrent) this@startActivity.activity?.finish()
        }
