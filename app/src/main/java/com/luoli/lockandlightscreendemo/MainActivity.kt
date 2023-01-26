package com.luoli.lockandlightscreendemo

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.luoli.lockandlightscreendemo.Receiver.AdminManageReceiver
import com.luoli.lockandlightscreendemo.Services.MyService
import com.luoli.lockandlightscreendemo.utils.DialogUtils
import com.luoli.lockandlightscreendemo.utils.LockScreenUtils
import com.luoli.lockandlightscreendemo.utils.SpUtils

class MainActivity : AppCompatActivity() {

    private var mAdminName: ComponentName? = null
    private var mDPM: DevicePolicyManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        //是否开启抬手亮屏
        ifOpenLightScreen()
        //是否开启锁屏
        LockScreenUtils(this).ifOpenLockScreen()
    }

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
//                    finish()
                },
                "算了", null
            ).show()
            stopService(Intent(this@MainActivity, MyService::class.java))
        }
    }


    //检查设备管理器是否已经激活
    private fun checkLockPermission(): Boolean {
        //如果设备管理器尚未激活，这里会启动一个激活设备管理器的Intent,具体的表现就是第一次打开程序时，手机会弹出激活设备管理器的提示，激活即可。
        mAdminName = ComponentName(this, AdminManageReceiver::class.java)
        mDPM = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return mDPM!!.isAdminActive(mAdminName!!)
    }


    //跳转到设备管理器激活页面
    private fun showAdminManagement(mAdminName: ComponentName) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "activity device")
        startActivityForResult(intent, 1)
    }

    /**
     * 按下锁屏键
     */
    private fun lockScreen() {
        if (mDPM!!.isAdminActive(mAdminName!!)) {
            mDPM!!.lockNow()
            finish()
        }
    }


}