/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixcore.repositories.core.common

import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.ImageView
import com.phenixrts.common.RequestStatus
import com.phenixrts.express.*
import com.phenixrts.pcast.*
import com.phenixrts.pcast.android.AndroidVideoRenderSurface
import com.phenixrts.room.*
import com.phenixrts.suite.phenixcore.common.launchMain
import com.phenixrts.suite.phenixcore.repositories.models.*
import kotlinx.coroutines.delay
import timber.log.Timber

private val userMediaOptions = UserMediaOptions().apply {
    videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] = listOf(DeviceConstraint(FacingMode.USER))
    videoOptions.capabilityConstraints[DeviceCapability.HEIGHT] = listOf(DeviceConstraint(360.0))
    videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] = listOf(DeviceConstraint(15.0))
    audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] =
        listOf(DeviceConstraint(AudioEchoCancelationMode.ON))
}

internal val rendererOptions get() = RendererOptions().apply {
    audioEchoCancelationMode = AudioEchoCancelationMode.ON
}

internal fun getRoomOptions(configuration: PhenixRoomConfiguration): RoomOptions {
    var roomOptionsBuilder = RoomServiceFactory.createRoomOptionsBuilder()
        .withType(configuration.roomType)
    if (configuration.roomAlias.isNotBlank()) {
        roomOptionsBuilder = roomOptionsBuilder.withAlias(configuration.roomAlias).withName(configuration.roomAlias)
    } else if (configuration.roomId.isNotBlank()) {
        roomOptionsBuilder = roomOptionsBuilder.withAlias(configuration.roomId).withName(configuration.roomId)
    }
    return roomOptionsBuilder.buildRoomOptions()
}

internal fun getPublishToRoomOptions(
    roomOptions: RoomOptions,
    publishOptions: PublishOptions,
    configuration: PhenixRoomConfiguration
): PublishToRoomOptions {
    val options = RoomExpressFactory.createPublishToRoomOptionsBuilder()
        .withMemberRole(configuration.memberRole)
        .withRoomOptions(roomOptions)
        .withPublishOptions(publishOptions)
    if (configuration.memberName.isNotBlank()) {
        options.withScreenName(configuration.memberName)
    }
    return options.buildPublishToRoomOptions()
}

internal fun getPublishToChannelOptions(
    configuration: PhenixConfiguration,
    channelConfiguration: PhenixChannelConfiguration,
    userMediaStream: UserMediaStream
): PublishToChannelOptions {
    val channelOptions = RoomServiceFactory.createChannelOptionsBuilder()
        .withName(channelConfiguration.channelAlias)
        .withAlias(channelConfiguration.channelAlias)
        .buildChannelOptions()
    var publishOptionsBuilder = PCastExpressFactory.createPublishOptionsBuilder()
        .withUserMedia(userMediaStream)
    publishOptionsBuilder = if (!configuration.publishToken.isNullOrBlank()) {
        Timber.d("Publishing with publish token: ${configuration.publishToken}")
        publishOptionsBuilder.withStreamToken(configuration.publishToken).withSkipRetryOnUnauthorized()
    } else {
        Timber.d("Publishing with capabilities")
        publishOptionsBuilder.withCapabilities(channelConfiguration.channelCapabilities.toTypedArray())
    }
    return ChannelExpressFactory.createPublishToChannelOptionsBuilder()
        .withChannelOptions(channelOptions)
        .withPublishOptions(publishOptionsBuilder.buildPublishOptions())
        .buildPublishToChannelOptions()
}

internal fun getPublishOptions(userMediaStream: UserMediaStream, publishToken: String?,
                               channelCapabilities: List<String>): PublishOptions {
    var options = PCastExpressFactory.createPublishOptionsBuilder()
        .withUserMedia(userMediaStream)
    options = if (!publishToken.isNullOrBlank()) {
        Timber.d("Publishing with publish token: $publishToken")
        options.withStreamToken(publishToken).withSkipRetryOnUnauthorized()
    } else if (channelCapabilities.isNotEmpty()) {
        Timber.d("Publishing with capabilities: $channelCapabilities")
        options.withCapabilities(channelCapabilities.toTypedArray())
    } else {
        Timber.d("Publishing with capabilities")
        options.withCapabilities(arrayOf("ld", "multi-bitrate", "prefer-h264"))
    }
    return options.buildPublishOptions()
}

internal fun getJoinRoomOptions(configuration: PhenixRoomConfiguration): JoinRoomOptions {
    var joinRoomOptionsBuilder = RoomExpressFactory.createJoinRoomOptionsBuilder()
        .withScreenName(configuration.memberName)
    if (configuration.roomAlias.isNotBlank()) {
        joinRoomOptionsBuilder = joinRoomOptionsBuilder.withRoomAlias(configuration.roomAlias)
    } else if (configuration.roomId.isNotBlank()) {
        joinRoomOptionsBuilder = joinRoomOptionsBuilder.withRoomId(configuration.roomId)
    }
    return joinRoomOptionsBuilder.buildJoinRoomOptions()
}

internal fun getCreateRoomOptions(configuration: PhenixRoomConfiguration): RoomOptions {
    var roomOptionsBuilder = RoomServiceFactory.createRoomOptionsBuilder()
        .withType(configuration.roomType)
    if (configuration.roomAlias.isNotBlank()) {
        roomOptionsBuilder = roomOptionsBuilder.withAlias(configuration.roomAlias).withName(configuration.roomAlias)
    } else if (configuration.roomId.isNotBlank()) {
        roomOptionsBuilder = roomOptionsBuilder.withName(configuration.roomId)
    }
    return roomOptionsBuilder.buildRoomOptions()
}

internal fun RoomExpress.joinRoom(configuration: PhenixRoomConfiguration, onJoined: (service: RoomService?) -> Unit) {
    joinRoom(getJoinRoomOptions(configuration)) { requestStatus: RequestStatus?, roomService: RoomService? ->
        Timber.d("Room join completed with status: $requestStatus")
        onJoined(roomService)
    }
}

internal fun RoomExpress.createRoom(configuration: PhenixRoomConfiguration, onCreated: (RequestStatus) -> Unit) {
    createRoom(getCreateRoomOptions(configuration)) { status, _ ->
        onCreated(status)
    }
}

internal fun RoomExpress.publishInRoom(options: PublishToRoomOptions, onPublished: (ExpressPublisher?, RoomService?) -> Unit) {
    publishToRoom(options) { requestStatus: RequestStatus, service: RoomService?, publisher: ExpressPublisher? ->
        Timber.d("Media is published to room: $requestStatus, ${publisher != null}")
        onPublished(publisher, service)
    }
}

internal fun getSubscribeVideoOptions(
    rendererSurface: AndroidVideoRenderSurface,
    aspectRatioMode: AspectRatioMode,
    configuration: PhenixRoomConfiguration?
): SubscribeToMemberStreamOptions {
    val rendererOptions = rendererOptions
    rendererOptions.aspectRatioMode = aspectRatioMode
    var memberStreamOptionsBuilder = RoomExpressFactory.createSubscribeToMemberStreamOptionsBuilder()
        .withRenderer(rendererSurface)
        .withRendererOptions(rendererOptions)
        .withCapabilities(arrayOf("video-only"))
    if (configuration?.roomVideoToken != null) {
        memberStreamOptionsBuilder = memberStreamOptionsBuilder.withStreamToken(configuration.roomVideoToken)
    }
    return memberStreamOptionsBuilder.buildSubscribeToMemberStreamOptions()
}

internal fun getSubscribeAudioOptions(configuration: PhenixRoomConfiguration?): SubscribeToMemberStreamOptions {
    var memberStreamOptionsBuilder = RoomExpressFactory.createSubscribeToMemberStreamOptionsBuilder()
        .withCapabilities(arrayOf("audio-only"))
        .withAudioOnlyRenderer()
        .withRendererOptions(rendererOptions)
    if (configuration?.roomAudioToken != null) {
        memberStreamOptionsBuilder = memberStreamOptionsBuilder.withStreamToken(configuration.roomAudioToken)
    }
    return memberStreamOptionsBuilder.buildSubscribeToMemberStreamOptions()
}

internal fun PCastExpress.getUserMedia(onMediaCollected: (userMedia: UserMediaStream) -> Unit) {
    getUserMedia(userMediaOptions) { status, stream ->
        Timber.d("Collecting media stream from pCast: $status")
        onMediaCollected(stream)
    }
}

internal fun getUserMediaOptions(configuration: PhenixPublishConfiguration): UserMediaOptions = UserMediaOptions().apply {
    // TODO: Changing facing mode some time crashes the app with:
    //  JNI DETECTED ERROR IN APPLICATION: JNI GetObjectRefType called with pending exception java.lang.RuntimeException: Fail to connect to camera service
    if (configuration.cameraFacingMode != FacingMode.UNDEFINED) {
        videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] = listOf(DeviceConstraint(configuration.cameraFacingMode))
    }
    // TODO: If Height is not set to the same value as the default one (Set on app start) - then BAD_REQUEST is returned when applying options
    videoOptions.capabilityConstraints[DeviceCapability.HEIGHT] = listOf(DeviceConstraint(360.0))
    // TODO: Changing FPS - causes BAD_REQUEST which then causes the stream to be re-created and unusable for publishing;
    videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] = listOf(DeviceConstraint(configuration.cameraFps))
    audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] = listOf(DeviceConstraint(configuration.echoCancellationMode))
    audioOptions.enabled = configuration.microphoneEnabled
}

internal fun ImageView.drawFrameBitmap(bitmap: Bitmap, delay: Boolean, onDrawn: () -> Unit) {
    try {
        launchMain {
            if (bitmap.isRecycled) return@launchMain
            if (delay) delay(THUMBNAIL_DRAW_DELAY)
            setImageBitmap(bitmap.copy(bitmap.config, bitmap.isMutable))
            onDrawn()
        }
    } catch (e: Exception) {
        Timber.d(e, "Failed to draw bitmap")
    }
}

internal fun Bitmap.prepareBitmap(configuration: PhenixFrameReadyConfiguration?): Bitmap {
    if (configuration == null) return this
    return try {
        val newWidth = configuration.width.takeIf { it < width } ?: width
        val newHeight = configuration.height.takeIf { it < height } ?: height
        val matrix = Matrix()
        if (newWidth > 0 && newHeight > 0) {
            val scaleWidth = newWidth / width.toFloat()
            val scaleHeight = newHeight / height.toFloat()
            matrix.postScale(scaleWidth, scaleHeight)
        }
        matrix.postRotate(configuration.rotation)
        Bitmap.createBitmap(this, 0, 0, newWidth, newHeight, matrix, true)
    } catch (e: Exception) {
        Timber.d(e, "Failed to prepare bitmap for: $configuration, $width, $height")
        this
    }
}