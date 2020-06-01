/*****************************************************************************
 * NotificationHelper.java
 *
 * Copyright © 2017 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package org.videolan.vlc.gui.helpers


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import org.videolan.resources.ACTION_PAUSE_SCAN
import org.videolan.resources.ACTION_RESUME_SCAN
import org.videolan.vlc.R
import org.videolan.vlc.StartActivity
import org.videolan.vlc.util.*

object NotificationHelper {
    const val TAG = "VLC/NotificationHelper"

    private val sb = StringBuilder()
    const val VLC_DEBUG_CHANNEL = "vlc_debug"

    private val notificationIntent = Intent()

    fun createPlaybackNotification(ctx: Context, video: Boolean, title: String, artist: String,
                                   album: String, cover: Bitmap?, playing: Boolean, pausable: Boolean,
                                   sessionToken: MediaSessionCompat.Token,
                                   spi: PendingIntent): Notification {

        val piStop = MediaButtonReceiver.buildMediaButtonPendingIntent(ctx, PlaybackStateCompat.ACTION_STOP)
        val builder = NotificationCompat.Builder(ctx, "vlc_playback")
        sb.setLength(0)
        sb.append(title).append(" - ").append(artist)
        builder.setSmallIcon(if (video) R.drawable.ic_notif_video else R.drawable.ic_notif_audio)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(Util.getMediaDescription(artist, album))
                .setLargeIcon(cover)
                .setTicker(sb.toString())
                .setAutoCancel(!playing)
                .setOngoing(playing)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setDeleteIntent(piStop)
                .setContentIntent(spi)
                .addAction(NotificationCompat.Action(
                        R.drawable.ic_widget_previous_w, ctx.getString(R.string.previous),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(ctx,
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
        if (pausable) {
            if (playing)
                builder.addAction(NotificationCompat.Action(
                        R.drawable.ic_widget_pause_w, ctx.getString(R.string.pause),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(ctx,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))
            else
                builder.addAction(NotificationCompat.Action(
                        R.drawable.ic_widget_play_w, ctx.getString(R.string.play),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(ctx,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))
        }
        builder.addAction(NotificationCompat.Action(
                R.drawable.ic_widget_next_w, ctx.getString(R.string.next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(ctx,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
        if (!pausable)
            builder.addAction(NotificationCompat.Action(
                    R.drawable.ic_widget_close_w, ctx.getString(R.string.stop), piStop))

        if (AndroidDevices.showMediaStyle) {
            builder.setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(piStop)
            )
        }
        return builder.build()
    }

    fun createScanNotification(ctx: Context, progressText: String, paused: Boolean): Notification {
        val scanCompatBuilder = NotificationCompat.Builder(ctx, "vlc_medialibrary")
                .setContentIntent(PendingIntent.getActivity(ctx, 0, Intent(ctx, StartActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.ic_notif_scan)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(ctx.getString(R.string.ml_scanning))
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOngoing(true)
        scanCompatBuilder.setContentText(progressText)

        notificationIntent.action = if (paused) ACTION_RESUME_SCAN else ACTION_PAUSE_SCAN
        val pi = PendingIntent.getBroadcast(ctx.applicationContext.getContextWithLocale(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val playpause = if (paused)
            NotificationCompat.Action(R.drawable.ic_play, ctx.getString(R.string.resume), pi)
        else
            NotificationCompat.Action(R.drawable.ic_pause, ctx.getString(R.string.pause), pi)
        scanCompatBuilder.addAction(playpause)
        return scanCompatBuilder.build()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createNotificationChannels(appCtx: Context) {
        val notificationManager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Playback channel
        var name: CharSequence = appCtx.getString(R.string.playback)
        var description = appCtx.getString(R.string.playback_controls)
        var channel = NotificationChannel("vlc_playback", name, NotificationManager.IMPORTANCE_LOW)
        channel.description = description
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
        // Scan channel
        name = appCtx.getString(R.string.medialibrary_scan)
        description = appCtx.getString(R.string.Medialibrary_progress)
        channel = NotificationChannel("vlc_medialibrary", name, NotificationManager.IMPORTANCE_LOW)
        channel.description = description
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)

        // Misc channel
        name = appCtx.getString(R.string.misc)
        channel = NotificationChannel("misc", name, NotificationManager.IMPORTANCE_LOW)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)

        // Recommendations channel
        if (AndroidDevices.isAndroidTv) {
            name = appCtx.getString(R.string.recommendations)
            description = appCtx.getString(R.string.recommendations_desc)
            channel = NotificationChannel("vlc_recommendations", name, NotificationManager.IMPORTANCE_LOW)
            channel.description = description
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createDebugServcieChannel(appCtx: Context) {
        val notificationManager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
// Playback channel
        val name = appCtx.getString(R.string.debug_logs)
        val channel = NotificationChannel(VLC_DEBUG_CHANNEL, name, NotificationManager.IMPORTANCE_LOW)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }
}
