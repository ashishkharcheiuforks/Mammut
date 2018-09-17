package io.github.jamiesanson.mammut.feature.instancebrowser.about

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.request.RequestOptions
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.component.GlideApp
import io.github.jamiesanson.mammut.data.remote.response.InstanceDetail
import kotlinx.android.synthetic.main.fragment_about_instance.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.textColorResource

class InstanceAboutFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_about_instance, container, false)

    @SuppressLint("SetTextI18n")
    fun populate(detail: InstanceDetail, logOutCallback: () -> Unit) {
        instanceTitleTextView.text = detail.name
        descriptionTextView.text = detail.info.shortDescription
        GlideApp.with(view!!)
                .asBitmap()
                .load(detail.thumbnail)
                .apply(RequestOptions.centerCropTransform())
                .into(backgroundImageView)
        fullDescriptionTextView.text = detail.info.fullDescription

        usersCountTextView.text = detail.users
        statusesCountTextView.text = detail.statuses
        connectionsCountTextView.text = detail.connections
        activeUsersTextView.text = "${detail.activeUsers} Active Users"

        presentUptime(name = detail.name, isUp = detail.up, uptime = detail.uptime)
        presentTechnicalDetails(detail)

        closeButton.onClick {
            requireActivity().onBackPressed()
        }

        logOutButton.onClick {
            logOutCallback()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun presentUptime(name: String, isUp: Boolean, uptime: Float) {
        if (isUp) {
            isUpImageView.setImageResource(R.drawable.ic_check_black_24dp)
            isUpImageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.successGreen), PorterDuff.Mode.SRC_IN)
            isUpTextView.text = "$name is up"
            isUpTextView.textColorResource = R.color.successGreen
        } else {
            isUpImageView.setImageResource(R.drawable.ic_close_black_24dp)
            isUpImageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.errorRed), PorterDuff.Mode.SRC_IN)
            isUpTextView.text = "$name is down"
            isUpTextView.textColorResource = R.color.errorRed
        }

        uptimePercentTextView.text = "Reporting an uptime of ${"%.1f".format(uptime * 100F)}%"
    }

    @SuppressLint("SetTextI18n")
    private fun presentTechnicalDetails(detail: InstanceDetail) {
        val ipv6Info = if (detail.ipv6) "IPV6 Enabled" else "No IPV6"

        technicalInfoTextView.text = "$ipv6Info • ${detail.version}"
    }
}