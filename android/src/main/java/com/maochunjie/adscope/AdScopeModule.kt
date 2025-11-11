package com.maochunjie.adscope

import com.beizi.fusion.BeiZiCustomController;
import com.beizi.fusion.BeiZis;
import com.beizi.fusion.AdListener;
import com.beizi.fusion.SplashAd;

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
  private var splashAdInstance: SplashAd? = null

  override fun getName(): String {
    return "AdScope"
  }

  @ReactMethod
  fun init(params: ReadableMap, promise: Promise) {
    try {
      val appId = params.getString("appId") ?: run {
        promise.reject("INVALID_PARAM", "appId is required")
        return
      }

      if (appId.isEmpty()) {
        promise.reject("INVALID_PARAM", "appId cannot be empty")
        return
      }

      // 从 JS 参数中读取各项配置，提供默认值（true）
      val isCanUseLocation = params.getBoolean("isCanUseLocation", true)
      val isCanUseWifiState = params.getBoolean("isCanUseWifiState", true)
      val isCanUsePhoneState = params.getBoolean("isCanUsePhoneState", true)
      val isCanUseOaid = params.getBoolean("isCanUseOaid", true)
      val devOaid = params.getString("devOaid") // 可为 null
      val isCanUseGaid = params.getBoolean("isCanUseGaid", true)
      val isCanUseAppList = params.getBoolean("isCanUseAppList", true)

      // 创建自定义控制器
      val customController = object : BeiZiCustomController() {
        override fun isCanUseLocation(): Boolean = isCanUseLocation

        override fun getLocation(): BeiZiLocation? = null // 如需传入位置，可在此构造 BeiZiLocation 对象

        override fun isCanUseWifiState(): Boolean = isCanUseWifiState

        override fun isCanUsePhoneState(): Boolean = isCanUsePhoneState

        override fun isCanUseOaid(): Boolean = isCanUseOaid

        override fun getDevOaid(): String? = if (!isCanUseOaid) devOaid else null

        override fun isCanUseGaid(): Boolean = isCanUseGaid

        override fun isCanUseAppList(): Boolean = isCanUseAppList
      }

      // 执行初始化
      BeiZis.init(reactApplicationContext, appId, customController)

      // 初始化是同步还是异步？
      // 根据 BeiZis 文档：init 通常是同步完成基础配置，但可能无回调。
      // 若 SDK 有异步回调，请改用监听器 + setTimeout 兜底。
      // 此处假设 init 成功即返回成功。

      val result = Arguments.createMap()
      result.putString("code", "0")
      result.putString("message", "BeiZis SDK initialized successfully")
      promise.resolve(result)

    } catch (e: Exception) {
      promise.reject("INIT_ERROR", "Failed to initialize BeiZis SDK: ${e.message}", e)
    }
  }

  @ReactMethod
  fun loadSplashAd(params: ReadableMap, promise: Promise) {
    val currentActivity = currentActivity ?: run {
      promise.reject("NO_ACTIVITY", "No current activity available")
      return
    }

    val adUnitId = params.getString("adUnitId") ?: run {
      promise.reject("INVALID_PARAM", "adUnitId is required")
      return
    }

    if (adUnitId.isEmpty()) {
      promise.reject("INVALID_PARAM", "adUnitId cannot be empty")
      return
    }
    val widthDp = params.getInt("width")
    val heightDp = params.getInt("height")
    val timeout = params.getInt("timeout").toLong() // BeiZis 使用 long 类型

    try {
      // 创建开屏广告对象
      // 第二个参数 skipView：传 null 表示使用 SDK 默认跳过按钮（支持“开屏点睛”）
      val splashAd = SplashAd(
        currentActivity,
        null, // 自定义跳过按钮 View，null 表示用默认
        adUnitId,
        object : AdListener() {
          override fun onAdLoaded() {
            emitEvent("AdScope-onAdLoaded", mapOf("ecpm" to splashAd.ecpmLevel.toString()))
            // 展示广告（必须在 onAdLoaded 后调用）
            splashAd.show(container)
            // Promise 成功：广告已加载并展示
            promise.resolve(createResult("0", "Ad loaded and shown"))
          }

          override fun onAdShown() {
            emitEvent("AdScope-onAdShown", emptyMap())
          }

          override fun onAdFailedToLoad(errorCode: Int) {
            splashAdInstance = null // 加载失败，清空引用
            emitEvent("AdScope-onAdFailedToLoad", mapOf("code" to errorCode.toString()))
            promise.reject("LOAD_FAIL", "Ad failed to load, code: $errorCode")
          }

          override fun onAdClosed() {
            emitEvent("AdScope-onAdClosed", emptyMap())
            // 注意：此时不应 finish Activity！由 JS 决定何时跳转
          }

          override fun onAdClicked() {
            emitEvent("AdScope-onAdClicked", emptyMap())
          }

          override fun onAdTick(millisUntilFinished: Long) {
            // 可选：发送倒计时事件
            // emitEvent("onAdTick", mapOf("timeLeft" to millisUntilFinished.toString()))
          }
        },
        timeout
      )
      //保存实例到成员变量
      splashAdInstance = splashAd
      // 发起广告加载请求
      splashAd.loadAd(widthDp, heightDp)

    } catch (e: Exception) {
      splashAdInstance = null
      promise.reject("EXCEPTION", "Failed to create/load splash ad: ${e.message}", e)
    }
  }

  @ReactMethod
  fun cancelSplashAd() {
    try {
      val ad = splashAdInstance
      if (ad != null) {
        ad.cancel(reactApplicationContext)
        splashAdInstance = null // 防止重复销毁
        promise.resolve(createResult("0", "SplashAd destroyed successfully"))
      } else {
        // 广告未创建或已销毁
        promise.resolve(createResult("1", "No active SplashAd to destroy"))
      }
    } catch (e: Exception) {
      promise.reject("DESTROY_ERROR", "Failed to destroy SplashAd: ${e.message}", e)
    }
  }

  // 工具方法：发送事件到 JS
  private fun emitEvent(eventName: String, data: Map<String, String>) {
    val map = Arguments.createMap()
    data.forEach { (key, value) -> map.putString(key, value) }
    reactApplicationContext
      .getJSModule(RCTDeviceEventEmitter::class.java)
      .emit("BeiZiSplash-$eventName", map)
  }

  // 工具方法：创建标准返回结果
  private fun createResult(code: String, message: String): WritableMap {
    return Arguments.createMap().apply {
      putString("code", code)
      putString("message", message)
    }
  }


}
