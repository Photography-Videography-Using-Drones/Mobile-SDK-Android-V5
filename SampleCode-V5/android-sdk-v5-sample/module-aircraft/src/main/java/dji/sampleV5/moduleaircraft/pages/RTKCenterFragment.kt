package dji.sampleV5.moduledrone.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.models.RTKCenterVM
import dji.sampleV5.modulecommon.util.ToastHelper.showToast
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkmobilestation.RTKLocation
import dji.sdk.keyvalue.value.rtkmobilestation.RTKSatelliteInfo
import dji.v5.manager.aircraft.rtk.RTKLocationInfo
import dji.v5.manager.aircraft.rtk.RTKSystemState
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.show
import dji.v5.ux.core.util.GpsUtils

import kotlinx.android.synthetic.main.frag_rtk_center_page.*


/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/3/19
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKCenterFragment : DJIFragment(), CompoundButton.OnCheckedChangeListener, RadioGroup.OnCheckedChangeListener {
    private val TAG = LogUtils.getTag("RTKCenterFragment")

    companion object {
        const val KEY_IS_QX_RTK = "key_is_qx_rtk"
    }

    private val rtkCenterVM: RTKCenterVM by activityViewModels()
    private var mIsUpdatingKeepStatus = false
    private var mIsUpdatingPrecisionStatus = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_rtk_center_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //初始化
        initListener()
        rtkCenterVM.addRTKLocationInfoListener()
        rtkCenterVM.addRTKSystemStateListener()

        rtkCenterVM.getAircraftRTKModuleEnabled()
        rtkCenterVM.getRTKMaintainAccuracyEnabled()

    }

    private fun initListener() {
        tb_rtk_keep_status_switch.setOnCheckedChangeListener(this)
        tb_precision_preservation_switch.setOnCheckedChangeListener(this)
        rtk_source_radio_group.setOnCheckedChangeListener(this)

        //处理打开相关RTK逻辑
        bt_open_network_rtk.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_network_trk_pag, networkRTKParam)
        }
        bt_open_rtk_station.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_rtk_station_page)
        }

        //RTK开启状态
        rtkCenterVM.getAircraftRTKModuleEnabledLD.observe(viewLifecycleOwner) {
            updateRTKOpenSwitchStatus(it.data)
        }
        //RTK 位置信息
        rtkCenterVM.rtkLocationInfoLD.observe(viewLifecycleOwner) {
            showRTKInfo(it.data)
        }
        //RTK SystemState信息
        rtkCenterVM.rtkSystemStateLD.observe(viewLifecycleOwner) {
            showRTKSystemStateInfo(it.data)
        }
        //RTK精度维持
        rtkCenterVM.getRTKAccuracyMaintainLD.observe(viewLifecycleOwner) {
            it.data?.showToast(
                StringUtils.getResStr(R.string.tv_rtk_precision_preservation_turn_on),
                StringUtils.getResStr(R.string.tv_rtk_precision_preservation_turn_off),
                tv_rtk_precision_preservation_hint_info
            )
            updateRTKMaintainAccuracy(it.data)
        }
    }

    private fun showRTKSystemStateInfo(rtkSystemState: RTKSystemState?) {
        rtkSystemState?.run {
            rtkConnected?.let {
                tv_tv_rtk_connect_info.text = if (rtkConnected) {
                    "Connected"
                } else {
                    "Disconnected"
                }
            }
            rtkHealthy?.let {
                tv_rtk_healthy_info.text = if (rtkHealthy) {
                    "healthy"
                } else {
                    "unhealthy"
                }
            }
            tv_rtk_error_info.text = error?.toString()
            //展示卫星数
            showSatelliteInfo(satelliteInfo)
            //更新RTK服务类型
            updateRTKUI(rtkReferenceStationSource)
            //更新飞控和机身的RTK是否正常连接
            updateRTKOpenSwitchStatus(isRTKEnabled)


        }
    }

    private fun showRTKInfo(rtkLocationInfo: RTKLocationInfo?) {
        rtkLocationInfo?.run {
            tv_trk_location_strategy.text = rtkLocation?.positioningSolution?.name
            tv_rtk_station_position_info.text = rtkLocation?.baseStationLocation?.toString()
            tv_rtk_mobile_position_info.text = rtkLocation?.mobileStationLocation?.toString()
            tv_rtk_position_std_distance_info.text = getRTKLocationDistance(rtkLocation)?.toString()
            tv_rtk_std_position_info.text = "stdLongitude:${rtkLocation?.stdLongitude}" +
                    ",stdLatitude:${rtkLocation?.stdLatitude}" +
                    ",stdAltitude=${rtkLocation?.stdAltitude}"


            tv_rtk_head_info.text = rtkHeading?.toString()
            tv_rtk_real_head_info.text = realHeading?.toString()
            tv_rtk_real_location_info.text = real3DLocation?.toString()

        }

    }

    private fun showSatelliteInfo(rtkSatelliteInfo: RTKSatelliteInfo?) {
        rtkSatelliteInfo?.run {
            var baseStationReceiverInfo = ""
            var mobileStationReceiver2Info = ""
            var mobileStationReceiver1Info = ""
            for (receiver1 in rtkSatelliteInfo.mobileStationReceiver1Info) {
                mobileStationReceiver1Info += "${receiver1.type.name}:${receiver1.count};"
            }
            for (receiver2 in rtkSatelliteInfo.mobileStationReceiver2Info) {
                mobileStationReceiver2Info += "${receiver2.type.name}:${receiver2.count};"
            }
            for (receiver3 in rtkSatelliteInfo.baseStationReceiverInfo) {
                baseStationReceiverInfo += "${receiver3.type.name}:${receiver3.count};"
            }

            tv_rtk_antenna_1_info.text = mobileStationReceiver1Info
            tv_rtk_antenna_2_info.text = mobileStationReceiver2Info
            tv_rtk_station_info.text = baseStationReceiverInfo

        }
    }

    private fun getRTKLocationDistance(rtklocation: RTKLocation?): Double? {
        rtklocation?.run {
            baseStationLocation?.let { baseStationLocation ->
                mobileStationLocation?.let { mobileStationLocation ->
                    return GpsUtils.distance3D(baseStationLocation, mobileStationLocation)
                }
            }
        }
        return null
    }


    //基站开启状态和精度维持Listener
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView) {
            tb_rtk_keep_status_switch -> {
                if (mIsUpdatingKeepStatus) {
                    return
                }
                mIsUpdatingKeepStatus = true
                rtkCenterVM.setAircraftRTKModuleEnabled(isChecked)
                rtkCenterVM.getAircraftRTKModuleEnabled()

            }
            tb_precision_preservation_switch -> {
                if (mIsUpdatingPrecisionStatus) {
                    return
                }
                mIsUpdatingPrecisionStatus = true
                rtkCenterVM.setRTKMaintainAccuracyEnabled(isChecked)
                rtkCenterVM.getRTKMaintainAccuracyEnabled()

            }
        }
    }

    //RTK源切换
    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.btn_rtk_source_base_rtk -> {
                LogUtils.d(TAG, "Turn on switch to base station RTK ")
                rtkCenterVM.setRTKReferenceStationSource(RTKReferenceStationSource.BASE_STATION)
            }
            R.id.btn_rtk_source_network -> {
                LogUtils.d(TAG, "Turn on switch to custom network RTK ")
                rtkCenterVM.setRTKReferenceStationSource(RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE)
            }
            R.id.btn_rtk_source_qx -> {
                LogUtils.d(TAG, "Turn on switch to custom QX RTK ")
                rtkCenterVM.setRTKReferenceStationSource(RTKReferenceStationSource.QX_NETWORK_SERVICE)
            }
        }
    }


    private fun updateRTKOpenSwitchStatus(isChecked: Boolean?) {
        tb_rtk_keep_status_switch.setOnCheckedChangeListener(null)
        tb_rtk_keep_status_switch.isChecked = isChecked ?: false
        tb_rtk_keep_status_switch.setOnCheckedChangeListener(this)
        rl_rtk_all.isVisible = isChecked ?: false
        mIsUpdatingKeepStatus = false

        if (isChecked == null || !isChecked) {
            tv_rtk_enable.text="RTK is off"
        }else{
            tv_rtk_enable.text="RTK is on"
        }

    }

    private fun updateRTKMaintainAccuracy(isChecked: Boolean?) {
        tb_precision_preservation_switch.setOnCheckedChangeListener(null)
        tb_precision_preservation_switch.isChecked = isChecked ?: false
        tb_precision_preservation_switch.setOnCheckedChangeListener(this)
        mIsUpdatingPrecisionStatus = false
    }

    //用于区分是哪个网络rtk
    private var networkRTKParam = Bundle()
    private fun updateRTKUI(rtkReferenceStationSource: RTKReferenceStationSource?) {
        rtk_source_radio_group.setOnCheckedChangeListener(null)
        when (rtkReferenceStationSource) {
            RTKReferenceStationSource.BASE_STATION -> {
                bt_open_rtk_station.show()
                bt_open_network_rtk.hide()
                rl_rtk_info_show.show()
                btn_rtk_source_base_rtk.isChecked = true
                networkRTKParam.putBoolean(KEY_IS_QX_RTK, false)
            }
            RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE -> {
                bt_open_rtk_station.hide()
                bt_open_network_rtk.show()
                rl_rtk_info_show.show()
                btn_rtk_source_network.isChecked = true
                networkRTKParam.putBoolean(KEY_IS_QX_RTK, false)
            }
            RTKReferenceStationSource.QX_NETWORK_SERVICE -> {
                bt_open_rtk_station.hide()
                bt_open_network_rtk.show()
                rl_rtk_info_show.show()
                btn_rtk_source_qx.isChecked = true
                networkRTKParam.putBoolean(KEY_IS_QX_RTK, true)
            }
            else -> {
                ToastUtils.showToast("Current rtk reference station source is:$rtkReferenceStationSource")
            }
        }
        rtk_source_radio_group.setOnCheckedChangeListener(this)
    }
}