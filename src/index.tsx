import {EmitterSubscription, NativeEventEmitter, NativeModules} from 'react-native';
const {AdScope} = NativeModules;

type EventListener = (event: any) => void;

export type EventName =
  | 'AdScope-onAdLoaded'
  | 'AdScope-onAdShown'
  | 'AdScope-onAdFailedToLoad'
  | 'AdScope-onAdClosed'
  | 'AdScope-onAdTick'
  | 'AdScope-onAdClicked';

const eventEmitter = new NativeEventEmitter(AdScope);

class AdScopeAd {
  // 初始化
  static init = async(params:{
    appId: string
  }): Promise<{code: string, message: string}> => {
    return await AdScope.init(params);
  }

  // 加载启动屏广告
  static loadSplashAd = async(params:{
    codeId: string
  }): Promise<{code: string, message: string}> => {
    return await AdScope.loadSplashAd(params);
  }

    // 销毁启动屏广告
  static cancelSplashAd = async() => {
    return await AdScope.cancelSplashAd();
  }

  static addListener = (eventType: EventName, listener: EventListener): EmitterSubscription => {
    return eventEmitter.addListener(eventType, listener);
  }
}




export default AdScopeAd
