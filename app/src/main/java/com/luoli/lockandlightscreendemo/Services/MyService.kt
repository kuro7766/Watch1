package com.luoli.lockandlightscreendemo.Services

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.luoli.lockandlightscreendemo.MainActivity
import com.luoli.lockandlightscreendemo.R
import sun.rmi.runtime.Log






class MyService:Service() {

    private var shakeTime: Long = 0
    private var showTime: Long = 0
    private var lastWakeUp: Long = 0
    private var mode: Long = 0 // 0 , 1 : 非抬手、进入抬手状态

    private var sensorManager: SensorManager? = null
    private var wakeLock: PowerManager.WakeLock? = null//亮屏


    private var cpuLock: PowerManager.WakeLock? = null//从doze apk里弄出来的

    private lateinit var notification:Notification


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notification = createNotification()
        startForeground(1,notification)
        initSensorEvent()
    }

    /**
     * 初始化重力感应
     */
    @SuppressLint("InvalidWakeLockTag")
    private fun initSensorEvent() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_DIM_WAKE_LOCK, "WakeLock"
        )
        val accelerometer: Sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager!!.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

//        添加cpu lock
        if(true){
            try {
                //acquire a CPU wakelock
                if (cpuLock != null && cpuLock.isHeld()) { //release preexisting wakelock if present
                    cpuLock.release()
                    cpuLock = null
                }
                if (true) { //acquire CPU wakelock if requested
                    cpuLock =
                        powerManager.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "MYDozeOff::CPULock"
                        )
                    cpuLock!!.acquire()
                }
            } catch (t: Throwable) {
                sun.rmi.runtime.Log.d("DozeOff", "CPU wakelock error")
            }
        }

    }


    /**
     * 处理重力感应监听
     */
    private val sensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val values = event.values
            //X轴方向的重力加速度，向右为正
            val x = values[0]
            //Y轴方向的重力加速度，向前为正
            val y = values[1]
            //Z轴方向的重力加速度，向上为正
            val z = values[2]
            val medumValue = 12
            //判断是否抬手
            if (Math.abs(x) > medumValue || Math.abs(y) > medumValue || Math.abs(z) > medumValue) {
                shakeTime = System.currentTimeMillis()
            }
            if (z < 9 && z > 2 && -2 < x && x < 2 && 4 < y && y < 10) {
//                showTime = System.currentTimeMillis()
//                if (showTime - shakeTime in 1..800) {
//
//                }
//                防止抖动
                if(System.currentTimeMillis() - lastWakeUp > 5000){

                    shakeTime = 0
                    wakeLock!!.acquire()
                    wakeLock!!.release()

                    lastWakeUp = System.currentTimeMillis()

                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }


    private fun createNotification(): Notification {
        val channelId = "LockService"
        val channelName = "LockService"
        val importance = NotificationManager.IMPORTANCE_HIGH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility=Notification.VISIBILITY_PRIVATE
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
        startService(Intent(this, GuardingServices::class.java))

        Log.i("萝莉","MyService服务onDestroy")
        super.onDestroy()
    }
    
}