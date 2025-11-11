# react-native-adscope

[![npm version](https://badge.fury.io/js/react-native-adscope.svg)](https://badge.fury.io/js/react-native-adscope)

Android SDK Version: 5.2.2.4

iOS SDK Version: 4.90.7.0

## 开始

`$ npm install react-native-adscope -E`

## 插件安装与初始化

### iOS

1. 可能需要配置广告跟踪

```xml
<key>NSUserTrackingUsageDescription</key>
<string>请放心，开启权限不会获取您在其他站点的隐私信息，该权限仅用于标识设备、第三方广告、并保障服务安全与提示浏览体验</string>
```

### Android

## 插件接口文档

```javascript
import AdScope from 'react-native-adscope';

// 初始化
let init = await AdScope.init({
    appId: 'XXXXX',
    isCanUseLocation: true, //是否允许渠道SDK主动使用地理位置信息
    isCanUseWifiState: true, //是否允许渠道SDK主动使用ACCESS_WIFI_STATE权限
    isCanUsePhoneState: true, //是否允许渠道SDK主动使用手机硬件参数，如：imei，imsi
    isCanUseOaid: true, //是否允许SDK获取Oaid
    devOaid: 'XXXXX', //测试用Oaid，仅在isCanUseOaid为false时生效
    isCanUseGaid: true, //是否允许SDK获取Gaid
    isCanUseAppList: true, //是否允许SDK获取应用列表
});

// 加载闪屏广告
let ret = await AdScope.loadSplashAd({
    adUnitId: 'XXXX', //广告ID，GroMore
    width: width, //广告宽度，仅Android有效
    height: height, //广告高度，仅Android有效
    timeout: 1500, //获取广告超时
});

//销毁闪屏广告
AdScope.cancelSplashAd();

// 添加监听
// AdScope-onAdLoaded 广告加载成功
// AdScope-onAdShown 广告展示
// AdScope-onAdFailedToLoad 广告加载失败
// AdScope-onAdClosed 广告关闭
// AdScope-onAdTick 倒计时回调，返回广告还将被展示的剩余时间
// AdScope-onAdClicked 广告点击
const listener = AdScope.addListener('AdScope-onAdLoaded', e => {
    console.log('AdScope-onAdLoaded');
});

// 移除监听
listener?.remove?.();
```

## License

MIT
