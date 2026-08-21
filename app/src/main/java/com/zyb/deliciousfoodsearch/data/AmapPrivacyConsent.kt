package com.zyb.deliciousfoodsearch.data

import android.content.Context
import com.amap.api.services.core.ServiceSettings

/**
 * 必须在创建高德定位或搜索客户端之前同步用户的隐私授权状态。
 */
object AmapPrivacyConsent {
    fun update(
        context: Context,
        agreed: Boolean,
    ) {
        val appContext = context.applicationContext
        ServiceSettings.updatePrivacyShow(appContext, true, true)
        ServiceSettings.updatePrivacyAgree(appContext, agreed)
    }
}
