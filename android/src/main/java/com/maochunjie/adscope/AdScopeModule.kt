package com.maochunjie.adscope

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.beizi.fusion.AdListener
import com.beizi.fusion.BeiZiCustomController
import com.beizi.fusion.BeiZis
import com.beizi.fusion.SplashAd

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter


class AdScopeModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  // 保存开屏广告实例的引用
  private var splashAd: SplashAd? = null
  private var adsParent: FrameLayout? = null

  override fun getName(): String {
    return "AdScope"
  }

  @ReactMethod
  fun init(params: ReadableMap, promise: Promise) {
    try {
      val appId = params.getString("appId") ?: run {
        val result = Arguments.createMap()
        result.putString("code", "0")
        result.putString("message", "appId is required")
        promise.resolve(result)
        return
      }

      if (appId.isEmpty()) {
        val result = Arguments.createMap()
        result.putString("code", "0")
        result.putString("message", "appId cannot be empty")
        promise.resolve(result)
        return
      }

      // 从 JS 参数中读取各项配置，提供默认值（true）
      val isCanUseLocation = params.getBooleanOrDefault("isCanUseLocation", true)
      val isCanUseWifiState = params.getBooleanOrDefault("isCanUseWifiState", true)
      val isCanUsePhoneState = params.getBooleanOrDefault("isCanUsePhoneState", true)
      val isCanUseOaid = params.getBooleanOrDefault("isCanUseOaid", true)
      val devOaid = params.getStringOrNull("devOaid") // 可为 null
      val isCanUseGaid = params.getBooleanOrDefault("isCanUseGaid", true)
      val isCanUseAppList = params.getBooleanOrDefault("isCanUseAppList", true)

      // 创建自定义控制器
      val customController = object : BeiZiCustomController() {
        override fun isCanUseLocation(): Boolean = isCanUseLocation

        override fun getLocation() = null // 如需传入位置，可在此构造 BeiZiLocation 对象

        override fun isCanUseWifiState(): Boolean = isCanUseWifiState

        override fun isCanUsePhoneState(): Boolean = isCanUsePhoneState

        override fun isCanUseOaid(): Boolean = isCanUseOaid

        override fun getDevOaid(): String? = if (!isCanUseOaid) devOaid else null

        override fun isCanUseGaid(): Boolean = isCanUseGaid

        override fun isCanUseAppList(): Boolean = isCanUseAppList
      }

      // 执行初始化
      BeiZis.init(reactApplicationContext, appId, customController)

      //init 成功即返回成功。
      val result = Arguments.createMap()
      result.putString("code", "1")
      result.putString("message", "BeiZis SDK initialized successfully")
      promise.resolve(result)

    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putString("code", "0")
      result.putString("message", "Failed to initialize BeiZis SDK: ${e.message}")
      promise.resolve(result)
    }
  }

  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @ReactMethod
  fun loadSplashAd(params: ReadableMap, promise: Promise) {
    val currentActivity = currentActivity ?: run {
      val result = Arguments.createMap()
      result.putString("code", "0")
      result.putString("message", "No current activity available")
      promise.resolve(result)
      return
    }

    val adUnitId = params.getString("adUnitId") ?: run {
      val result = Arguments.createMap()
      result.putString("code", "0")
      result.putString("message", "adUnitId is required")
      promise.resolve(result)
      return
    }

    if (adUnitId.isEmpty()) {
      val result = Arguments.createMap()
      result.putString("code", "0")
      result.putString("message", "adUnitId cannot be empty")
      promise.resolve(result)
      return
    }
    val widthDp = params.getInt("width")
    val heightDp = params.getInt("height")
    val timeout = params.getInt("timeout").toLong() // BeiZis 使用 long 类型

    try {
      // 创建开屏广告对象
      // 第二个参数 skipView：传 null 表示使用 SDK 默认跳过按钮（支持“开屏点睛”）
      currentActivity.runOnUiThread(Runnable {
        adsParent = FrameLayout(reactApplicationContext)
        adsParent!!.id = View.generateViewId() // 或使用固定 ID（需注册）

        val decorView = currentActivity.window.decorView as ViewGroup
        decorView.addView(
          adsParent, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
        )
        splashAd = SplashAd(reactApplicationContext, null, adUnitId, object : AdListener {
          override fun onAdLoaded() {
            splashAd?.show(adsParent)
            fireSplashAdEvent("onAdLoaded", "", "")
          }

          override fun onAdShown() {
            fireSplashAdEvent("onAdShown", "", "")
          }

          override fun onAdFailedToLoad(errorCode: Int) {
            fireSplashAdEvent(
              "onAdFailedToLoad",
              "Ad failed to load, code: $errorCode",
              errorCode.toString()
            )
          }

          override fun onAdClosed() {
            fireSplashAdEvent("onAdClosed", "", "")
            cleanupAdView()
          }

          override fun onAdTick(millisUnitFinished: Long) {
//                Log.i("BeiZisDemo", "onAdTick millisUnitFinished == " + millisUnitFinished);
          }

          override fun onAdClicked() {
            fireSplashAdEvent("onAdClicked", "", "")
          }
        }, timeout)
        splashAd?.loadAd(
          widthDp,
          heightDp
        )
        val result = Arguments.createMap()
        result.putString("code", "1")
        result.putString("message", "")
        promise.resolve(result)
      })
    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putString("code", "0")
      result.putString("message", "Failed to create/load splash ad: ${e.message}")
      promise.resolve(result)
    }
  }

  private fun cleanupAdView() {
    currentActivity?.runOnUiThread {
      try {
        adsParent?.let { view ->
          (view.parent as? ViewGroup)?.removeView(view)
        }
        splashAd = null
        adsParent = null
      } catch (e: Exception) {
        // ignore or log
      }
    }
  }

  @ReactMethod
  fun cancelSplashAd(params: ReadableMap, promise: Promise) {
    try {
      val ad = splashAd
      if (ad != null) {
        ad.cancel(reactApplicationContext)
        cleanupAdView()
        val result = Arguments.createMap()
        result.putString("code", "1")
        result.putString("message", "")
        promise.resolve(result)
      } else {
        // 广告未创建或已销毁
        val result = Arguments.createMap()
        result.putString("code", "0")
        result.putString("message", "")
        promise.resolve(result)
      }
    } catch (e: Exception) {
      val result = Arguments.createMap()
      result.putString("code", "0")
      result.putString("message", "")
      promise.resolve(result)
    }
  }

  fun fireSplashAdEvent(eventName: String, message: String, code: String) {
    val map = Arguments.createMap()
    map.putString("message", message)
    if (code != "") {
      map.putString("code", code)
    }
    fireEvent("AdScope-$eventName", map)
  }

  private fun fireEvent(eventName: String, params: WritableMap) {
    reactApplicationContext.getJSModule(RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  private fun ReadableMap.getBooleanOrDefault(key: String, defaultValue: Boolean): Boolean {
    return if (this.hasKey(key)) this.getBoolean(key) else defaultValue
  }

  private fun ReadableMap.getStringOrNull(key: String): String? {
    return if (this.hasKey(key)) this.getString(key) else null
  }


}
