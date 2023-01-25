package com.luoli.lockandlightscreendemo.utils

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.luoli.lockandlightscreendemo.R
import com.luoli.lockandlightscreendemo.Receiver.AdminManageReceiver

/**
 * 一键锁屏工具类
 * */
class LockScreenUtils(private var mActivity: Activity) {

    private var mAdminName: ComponentName? = null
    private var mDPM: DevicePolicyManager? = null


    fun ifOpenLockScreen() {
        //判断是否有锁屏权限
        if (checkLockPermission()) {
            lockScreen()//锁屏
        } else {
            DialogUtils.createDialog(mActivity,
                R.drawable.dialog_icon_warning,
                mActivity.resources.getString(R.string.dialog_title),
                mActivity.resources.getString(R.string.dialog_message),
                mActivity.resources.getString(R.string.dialog_confirm_button), { _, _ ->
                    showAdminManagement(mAdminName!!)
                },
                mActivity.resources.getString(R.string.dialog_cancel_button), { _, _ ->
                    Toast.makeText(mActivity, mActivity.resources.getString(R.string.dialog_cancel_message), Toast.LENGTH_SHORT).show()
                }
            ).show()

        }
    }


    //检查设备管理器是否已经激活
    private fun checkLockPermission(): Boolean {
        //如果设备管理器尚未激活，这里会启动一个激活设备管理器的Intent,具体的表现就是第一次打开程序时，手机会弹出激活设备管理器的提示，激活即可。
        mAdminName = ComponentName(mActivity, AdminManageReceiver::class.java)
        mDPM = mActivity.getSystemService(AppCompatActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return mDPM!!.isAdminActive(mAdminName!!)
    }


    //跳转到设备管理器激活页面
    private fun showAdminManagement(mAdminName: ComponentName) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "activity device")
        mActivity.startActivityForResult(intent, 1)
    }


    /**
     * 按下锁屏键
     */
    private fun lockScreen() {
        if (mDPM!!.isAdminActive(mAdminName!!)) {
            mDPM!!.lockNow()
            mActivity.finish()
        }
    }

}