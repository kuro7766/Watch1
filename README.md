# Android一键锁屏与抬手亮屏的实现

基于原作者修改 -> https://gitee.com/qu-wenbin/lock-and-light-screen-demo

适用于**只有重力传感器**的安卓手表，如瑞恒8606的抬手唤醒

添加了

- 开机自启动

- cpu wakelock，防杀

- 简化了唤醒判断逻辑：上抬约45°就会唤醒；保持抬手姿势不会持续唤醒（必须放下一次）；五秒钟之内只会唤醒一次。

耗电情况： 

|待机耗电量占比|海派贵族耗电情况|
| --- | --- |
|![IR~8U9$ET3TR$H A ~(B{SV](https://user-images.githubusercontent.com/49401947/221221181-de5c2fcf-98bc-4a57-b163-bf9ef627170c.jpg)|![{V05SL}YW$D`MGR R2 PCAL](https://user-images.githubusercontent.com/49401947/221221267-68c8e304-fe8a-4035-849d-f0edea30a02e.jpg)|


---

>>**注意：**
>>
>>如果你想卸载这个锁屏程序，可能会出现卸载失败的情况，此时你只要在系统设置中找到对应的设备管理器程序(设置 -> 安全和隐私 -> 设备管理-> 设备管理器找到），然后取消激活这个程序，就可以执行正常的卸载了

>> 或者先冻结，再卸载

---

### 以下出自原作者

.

### 介绍
> 最近由于项目需要开发语音控制相关的功能，需要用语音来实现锁屏和唤醒屏幕的功能，所以顺便就想开发一个“一键锁屏”的App。主要是我用的手机那个双击亮屏使用起来太麻烦了，而且又不想按电源键。（主要是因为太穷了，买不起手机O(∩_∩)O哈哈~）

.

.

### 锁屏实现一
> 主要是基于 `DeviceAdminReceiver`，安卓的设备管理器来实现一键锁屏的功能 

.

**1. 创建`AdminManageReceiver`类并继承`DeviceAdminReceiver`**

	public class AdminManageReceiver extends DeviceAdminReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        super.onReceive(context, intent);
	    }
	}

.

**2.在`res`目录下新建子目录`xml`,创建一个`xml`资源文件**

	<?xml version="1.0" encoding="utf-8"?>
	<device-admin xmlns:android="http://schemas.android.com/apk/res/android">
	    <uses-policies>
	        <force-lock />
	    </uses-policies>
	</device-admin>

.

**3. 在 `AndroidManifest.xml` 中进行注册**

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@drawable/ic_launcher"
        android:supportsRtl="true"
        android:theme="@style/Theme.LockAndLightScreenDemo"><!--隐藏状态栏与标题栏-->
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".Receiver.AdminManageReceiver"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_DEVICE_ADMIN">
            <meta-data
                android:name="android.app.device_admin"
                android:resource="@xml/device_admin" />

            <intent-filter>
                <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
            </intent-filter>
        </receiver>

    </application>

.

**4. 校验是否激活设备管理器**
> **注：** 不激活设备管理器权限是无法实现锁屏功能的。`DialogUtils`是我自己写的一个弹窗工具类，是基于`AlertDialog`实现的。

	private var mAdminName: ComponentName? = null
    private var mDPM: DevicePolicyManager? = null

    private fun ifOpenLockScreen() {
        //判断是否有锁屏权限
        if (checkLockPermission()) {
            lockScreen()//锁屏
        } else {
            DialogUtils.createDialog(this@MainActivity,
                R.drawable.dialog_icon_warning,
                "提醒",
                "点击「前往设置」前往激活设备管理器，如果未开启，锁屏功能无法使用!",
                "前往设置", { _, _ ->
                    showAdminManagement(mAdminName!!)
                },
                "算了", { dialog, which ->
                    Toast.makeText(this, "未开启，锁屏功能无法使用", Toast.LENGTH_SHORT).show()
                }
            ).show()
        }
    }

.

**5. 如果没有激活设备管理器，跳转到激活页面**

	private var mAdminName: ComponentName? = null
    private var mDPM: DevicePolicyManager? = null

    //跳转到设备管理器激活页面
    private fun showAdminManagement(mAdminName: ComponentName) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "activity device")
        startActivityForResult(intent, 1)
    }

.

**6. 实现一键锁屏功能**
> **注：** 我这里为了使用方便在调用完这个方法后 `finish()` ，会退出本应用。如果想实现更完美的效果建议把锁屏功能在 **Service** 实现，我这里就偷懒一下了。O(∩_∩)O哈哈~


    //一键锁屏
    private fun lockScreen() {
        if (mDPM!!.isAdminActive(mAdminName!!)) {
            mDPM!!.lockNow()
            finish()
        }
    }

.

.

### 锁屏实现二
> 主要是基于 `AccessibilityService`，安卓的无障碍服务来实现一键锁屏的功能 
.

**1. 创建 `AccessibilitySetting` 类并继承 `AccessibilityService` 服务**
> 这里只继承 `AccessibilityService` 服务，但是没有做任何的逻辑代码处理，如果想实现更多的功能可以自己去查找安卓无障碍服务修改的资料来实现，这里就不做过多的介绍。


	public class AccessibilitySetting extends AccessibilityService {
	    private static AccessibilityService service;
	
	    public AccessibilitySetting() {
	        service = this;
	    }
	
	    @Override
	    public void onAccessibilityEvent(AccessibilityEvent event) {
	
	    }
	
	    @Override
	    public void onInterrupt() {
	
	    }
	
	    public static AccessibilityService getService() {
	        if (service == null) {
	            return null;
	        }
	        return service;
	    }
	}

.

**2.在`res`目录下新建子目录`xml`,创建一个`xml`资源文件**

	<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
	    android:accessibilityEventTypes="typeAllMask"
	    android:accessibilityFeedbackType="feedbackGeneric"
	    android:accessibilityFlags="flagRequestFilterKeyEvents"
	    android:canRetrieveWindowContent="true"
	    android:canRequestFilterKeyEvents="true"
	    android:description="@string/accessibility" />


.

**3. 在 `AndroidManifest.xml` 中进行服务的注册**

	<service
	    android:name=".Services.AccessibilitySetting"
	    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
	    <intent-filter>
	        <action android:name="android.accessibilityservice.AccessibilityService" />
	    </intent-filter>
	
	    <meta-data
	        android:name="android.accessibilityservice"
	        android:resource="@xml/accessibilityservice" />
	</service>

.

**4. 校验是否有无障碍的权限**

    /**
     * 判断是否有无障碍权限
     */
    private boolean checkAccessPermission() {
        if (!isAccessibilityServiceRunning("AccessibilitySetting")) {
            DialogUtils.createDialog(MainActivity.this,
                    R.drawable.dialog_icon_warning,
                    "提醒",
                    "点击「前往设置」前往设置辅助功能。",
                    "前往设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            jump2AccessPermissionSetting(MainActivity.this, false);
                        }
                    }, "算了", null).show();

            Toast.makeText(this, "未开启，操作功能无法使用", Toast.LENGTH_SHORT).show();
            return false;

        } else {
            Toast.makeText(this, "已开启", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    /**
     * 判断是否存在置顶的无障碍服务
     *
     * @param name
     * @return
     */
    public boolean isAccessibilityServiceRunning(String name) {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enableServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo enableService : enableServices) {
            if (enableService.getId().endsWith(name)) {
                return true;
            }
        }
        return false;
    }

.

**5. 如果没有有无障碍的权限，则跳转到授权页面**


    /**
     * 跳转到辅助功能设置界面
     *
     * @param activity
     */
    private static final String INTENT_ACTION_ACCESSIBILITY_SETTINGS = "android.settings.ACCESSIBILITY_SETTINGS";
    //辅助功能Key
    public static final int REQUEST_PERMISS_REQUEST_ACCESSABLE = 300;

    public static void jump2AccessPermissionSetting(Activity activity, boolean forResult) {
        if (forResult) {
            activity.startActivityForResult(new Intent(INTENT_ACTION_ACCESSIBILITY_SETTINGS), REQUEST_PERMISS_REQUEST_ACCESSABLE);
        } else {
            activity.startActivity(new Intent(INTENT_ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

.


**6. 实现锁屏功能**


	private void lockScreen() {
	    if (checkAccessPermission()){
	        if (Build.VERSION.SDK_INT < 16) {
	            Toast.makeText(AccessibilitySetting.getService(), "Android 4.1及以上系统才支持此功能，请升级后重试", Toast.LENGTH_SHORT).show();
	        } else {
	            if (AccessibilitySetting.getService() != null) {
	                AccessibilitySetting.getService().performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
	            }
	        }
	    }
	}

> **注：** 同样也要对 `AccessibilitySetting` 服务进行保活操作，可以参考下面的抬手亮屏的实现。


.

.

### 抬手亮屏的实现
> 抬手亮屏的功能的实现主要是基于重力感应传感器实现的，如果想实现更完美的效果可以配合光线距离传感器来一起实现，我这里就偷懒一下了。O(∩_∩)O哈哈~，就没有使用光线距离传感器了。

.

**1. 初始化重力感应**

	private var shakeTime: Long = 0
    private var showTime: Long = 0
    private var sensorManager: SensorManager? = null
    private var wakeLock: PowerManager.WakeLock? = null//亮屏
    private lateinit var notification:Notification


    //初始化重力感应
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
    }

.

**2. 创建传感器的监听器**

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
                showTime = System.currentTimeMillis()
                if (showTime - shakeTime in 1..800) {
                    shakeTime = 0
                    wakeLock!!.acquire()
                    wakeLock!!.release()//亮屏
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

**到这里就可以实现抬手亮屏的功能了，下面贴一下完整代码：**

.

**在 `AndroidManifest.xml` 中进行注册**

    <service
        android:name=".Services.MyService"
        android:priority = "1000"
        android:foregroundServiceType="location"
        android:enabled="true"
        android:exported="true" />
    <!--守护服务-->
    <service android:name=".Services.GuardingServices"
        android:priority = "1000"
        android:foregroundServiceType="location"
        android:enabled="true"
        android:exported="true"/>

**核心的实现代码：**

	
	class MyService:Service() {
	
	    private var shakeTime: Long = 0
	    private var showTime: Long = 0
	    private var sensorManager: SensorManager? = null
	    private var wakeLock: PowerManager.WakeLock? = null//亮屏
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
	                showTime = System.currentTimeMillis()
	                if (showTime - shakeTime in 1..800) {
	                    shakeTime = 0
	                    wakeLock!!.acquire()
	                    wakeLock!!.release()
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
	            .setContentText("点击锁屏")
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

.

**守护服务**

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
	            .setContentText("点击锁屏")
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

.

**最后在Activity中启动服务**

	//是否开启抬手亮屏
	private fun ifOpenLightScreen() {
	    if (SpUtils.getBoolean(this, SpUtils.KEY_IDEA_FUNC_GRAVITY_SENSOR, false)) {
	        //启动抬手亮屏服务
	        startService(Intent(this@MainActivity, MyService::class.java))
	    } else {
	        DialogUtils.createDialog(
	            this@MainActivity,
	            R.drawable.dialog_icon_warning,
	            "提醒",
	            "是否开启抬手亮屏功能，如果不开启只能清除所以数据后才能再开启！",
	            "开启", { _, _ ->
	                SpUtils.saveBoolean(this, SpUtils.KEY_IDEA_FUNC_GRAVITY_SENSOR, true)
	                //启动抬手亮屏服务
	                startService(Intent(this@MainActivity, MyService::class.java))
	                finish()
	            },
	            "算了", null
	        ).show()
	        stopService(Intent(this@MainActivity, MyService::class.java))
	    }
	}



> **重要说明：**
> 
> 为了使用 **`MyService`** 服务能在系统中存活更长的时间，这里在启动服务的时候会默认开启一个通知，并且设置该服务为前台服务，还创建一个 `GuardingServices` 守护服务用来在服务被系统杀死的时候把服务重新拉起来。
> 
>>**注意：**
>>
>>如果你想卸载这个锁屏程序，可能会出现卸载失败的情况，此时你只要在系统设置中找到对应的设备管理器程序(设置 -> 安全和隐私 -> 设备管理-> 设备管理器找到），然后取消激活这个程序，就可以执行正常的卸载了


.

[**体验安装包**](https://gitee.com/qu-wenbin/lock-and-light-screen-demo/blob/master/app/release/app-release.apk)

.

.

### 项目地址

- [**码云**](https://gitee.com/qu-wenbin/lock-and-light-screen-demo)
