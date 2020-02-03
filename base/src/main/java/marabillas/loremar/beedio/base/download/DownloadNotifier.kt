/*
 * Beedio is an Android app for downloading videos
 * Copyright (C) 2019 Loremar Marabillas
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
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package marabillas.loremar.beedio.base.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import marabillas.loremar.beedio.base.R
import marabillas.loremar.beedio.base.database.DownloadItem
import java.io.File
import kotlin.math.roundToInt


class DownloadNotifier(
        private val context: Context,
        private val downloadItem: DownloadItem,
        private val directory: File
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(PROGRESS_CHANNEL,
                    "Download Notification", NotificationManager.IMPORTANCE_LOW))
            notificationManager.createNotificationChannel(NotificationChannel(FINISH_CHANNEL,
                    "Download Notification", NotificationManager.IMPORTANCE_HIGH))

            notificationManager
                    .getNotificationChannel(PROGRESS_CHANNEL)
                    ?.setSound(null, null)
        }
    }

    fun notifyProgress() {
        val name = downloadItem.name
        val ext = downloadItem.ext
        val filename = "$name.$ext"

        var notificationBuilder = NotificationCompat.Builder(context, PROGRESS_CHANNEL).run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setStyle(NotificationCompat.BigTextStyle())
            } else {
                setSound(null).setPriority(NotificationCompat.PRIORITY_LOW)
            }
        }

        notificationBuilder = notificationBuilder.setContentTitle("Downloading $filename")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setOngoing(true)

        val file = File(directory, filename)
        if (downloadItem.isChunked) {
            val downloaded = if (file.exists())
                Formatter.formatFileSize(context, file.length())
            else
                "OKB"

            notificationBuilder.setProgress(100, 0, true)
                    .setContentText(downloaded)
            notificationManager.notify(PROGRESS_NOTIFY_ID, notificationBuilder.build())
        } else {
            val size = downloadItem.size
            var progress = ((file.length().toDouble() / size.toDouble()) * 100).roundToInt()
            progress = if (progress >= 100) 100 else progress

            val downloaded = Formatter.formatFileSize(context, file.length())
            val total = Formatter.formatFileSize(context, size)

            notificationBuilder.setProgress(100, progress, false)
                    .setContentText("$downloaded/$total $progress%")
            notificationManager.notify(PROGRESS_NOTIFY_ID, notificationBuilder.build())
        }
    }

    fun notifyFinish() {
        stop()

        val name = downloadItem.name
        val ext = downloadItem.ext
        val filename = "$name.$ext"

        var notificationBuilder = NotificationCompat.Builder(context, FINISH_CHANNEL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = notificationBuilder.setTimeoutAfter(1500)
                    .setContentTitle("Download Finished")
                    .setContentText(filename)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))

            notificationManager.notify(FINISH_NOTIFY_ID, notificationBuilder.build())
        } else {
            notificationBuilder = notificationBuilder.setTicker("Download Finished")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))

            notificationManager.notify(FINISH_NOTIFY_ID, notificationBuilder.build())
            notificationManager.cancel(FINISH_NOTIFY_ID)
        }
    }

    fun stop() {
        notificationManager.cancel(PROGRESS_NOTIFY_ID)
    }

    companion object {
        const val PROGRESS_CHANNEL = "progress_channel"
        const val FINISH_CHANNEL = "finish_channel"
        const val PROGRESS_NOTIFY_ID = 1
        const val FINISH_NOTIFY_ID = 2
    }
}