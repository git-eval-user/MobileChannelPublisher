/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui.viewmodel

import android.view.SurfaceHolder
import androidx.lifecycle.ViewModel
import com.phenixrts.pcast.android.AndroidVideoRenderSurface
import com.phenixrts.suite.channelpublisher.common.PublishConfiguration
import com.phenixrts.suite.channelpublisher.common.launchMain
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import timber.log.Timber

class ChannelViewModel(private val channelExpressRepository: ChannelExpressRepository) : ViewModel() {

    private val androidVideoSurface = AndroidVideoRenderSurface()

    var channelAlias: String = ""
    val onChannelExpressError = channelExpressRepository.onChannelExpressError
    val onChannelState = channelExpressRepository.onChannelState

    fun showPublisherPreview(surfaceHolder: SurfaceHolder) {
        androidVideoSurface.setSurfaceHolder(surfaceHolder)
        channelExpressRepository.showPublisherPreview(androidVideoSurface)
    }

    fun publishToChannel(configuration: PublishConfiguration, surfaceHolder: SurfaceHolder) = launchMain {
        Timber.d("Publishing to channel: $configuration")
        androidVideoSurface.setSurfaceHolder(surfaceHolder)
        channelExpressRepository.publishToChannel(configuration)
    }

    fun stopPublishing() = channelExpressRepository.stopPublishing()
}
