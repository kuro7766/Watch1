package com.luoli.lockandlightscreendemo.Services

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.luoli.lockandlightscreendemo.MainActivity
import com.luoli.lockandlightscreendemo.R

/**
 * 守护服务
 *
 * 作用：拉起MyService抬手亮屏主服务
 * */

class GuardingServices:Service() {

    private lateinit var notification:Notification

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notification = createNotification()
        startForeground(1,notification)
        startService(Intent(this, MyService::class.java))
        Log.i("萝莉","GuardingServices服务onCreate")
    }


    private fun createNotification(): Notification {
        val channelId = "LockService"
        val channelName = "LockService"
        val importance = NotificationManager.IMPORTANCE_HIGH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility= Notification.VISIBILITY_PRIVATE
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_launcher) //通知显示的图标
            .setContentIntent(pendingIntent) //点击通知进入Activity
            .build()
    }

    override fun onDestroy() {
        //在服务被销毁时，关闭前台服务
        stopForeground(true)
        startService(Intent(this, MyService::class.java))
        Log.i("萝莉","GuardingServices服务onDestroy")
        super.onDestroy()
    }

}