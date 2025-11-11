#import <BeiZiSDK/BeiZiSDK.h>
#import "AdScope.h"

@implementation AdScope {
    BeiZiSplash *splash;
}

RCT_EXPORT_MODULE()

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[
        @"AdScope-onAdLoaded",
        @"AdScope-onAdShown",
        @"AdScope-onAdFailedToLoad",
        @"AdScope-onAdClosed",
        @"AdScope-onAdTick",
        @"AdScope-onAdClicked"
    ];
}

RCT_EXPORT_METHOD(init:(NSDictionary *)params resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        // 1. 检查 appId 是否存在且非空
        NSString *appId = params[@"appId"];

        if (!appId || [appId isKindOfClass:[NSNull class]] || appId.length == 0) {
            NSDictionary *result = @{
                    @"code": @"0",
                    @"message": @"appId is required and cannot be empty"
            };
            resolve(result);
            return;
        }

        [BeiZiSDKManager configureWithApplicationID:appId];
        NSDictionary *result = @{
                @"code": @"1",
                @"message": @""
        };
        resolve(result);
    } @catch (NSException *exception) {
        NSDictionary *result = @{
                @"code": @"0",
                @"message": [NSString stringWithFormat:@"Failed to initialize BeiZis SDK: %@", exception.reason ? : @"unknown error"]
        };
        resolve(result);
    }
}



RCT_EXPORT_METHOD(loadSplashAd:(NSDictionary *)params resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSString *adUnitId = @"";
    float timeout = 5000;

    if ((NSString *)params[@"adUnitId"] != nil) {
        adUnitId = (NSString *)params[@"adUnitId"];
    }

    if (params[@"timeout"] != nil) {
        timeout = [[params objectForKey:@"timeout"]floatValue];
    }

    splash = [[BeiZiSplash alloc]initWithSpaceID:adUnitId spaceParam:@"" lifeTime:timeout];
    splash.delegate = self;
    splash.showLaunchImage = NO;
    [splash BeiZi_loadSplashAd];// 使用此方法加载开屏
    NSDictionary *result = @{
            @"code": @"1",
            @"message": @""
    };
    resolve(result);
}

RCT_EXPORT_METHOD(cancelSplashAd:(NSDictionary *)params resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
}

#pragma 监听
- (void)BeiZi_splashDidLoadSuccess:(BeiZiSplash *)beiziSplash {
    //主线程
    [splash BeiZi_showSplashAdWithWindow:[UIApplication sharedApplication].keyWindow];
    [self sendEventWithName:@"AdScope-onAdLoaded" body:@{ @"message": @"", @"code": @"" }];
}

/**
   开屏展现
 */
- (void)BeiZi_splashDidPresentScreen:(BeiZiSplash *)beiziSplash {
    [self sendEventWithName:@"AdScope-onAdShown" body:@{ @"message": @"", @"code": @"" }];
}

/**
   开屏点睛点击
 */
- (void)BeiZi_splashZoomOutDidClick:(BeiZiSplash *)beiziSplash {
}

/**
   开屏点睛关闭
 */
- (void)BeiZi_splashZoomOutDidClose:(BeiZiSplash *)beiziSplash {
    splash = nil;
    splash.delegate = nil;
}

/**
   开屏点击
 */
- (void)BeiZi_splashDidClick:(BeiZiSplash *)beiziSplash {
    [self sendEventWithName:@"AdScope-onAdClicked" body:@{ @"message": @"", @"code": @"" }];
}

/**
   开屏即将消失
 */
- (void)BeiZi_splashWillDismissScreen:(BeiZiSplash *)beiziSplash {
//    NSLog(@"Cookie:开屏即将消失");
}

/**
   开屏消失
 */
- (void)BeiZi_splashDidDismissScreen:(BeiZiSplash *)beiziSplash {
    [self sendEventWithName:@"AdScope-onAdClosed" body:@{ @"message": @"", @"code": @"" }];

    if (!splash.isZoomOutAd) {
        splash = nil;
        splash.delegate = nil;
    }
}

- (void)BeiZi_splashAdLifeTime:(int)lifeTime {
//    NSLog(@"Cookie:开屏倒计时%d", lifeTime);
}

/**
   开屏请求失败
 */
- (void)BeiZi_splash:(BeiZiSplash *)beiziSplash didFailToLoadAdWithError:(BeiZiRequestError *)error {
    [self sendEventWithName:@"AdScope-onAdFailedToLoad" body:@{ @"code": [NSString stringWithFormat:@"%ld", (long)error.code], @"message": [NSString stringWithFormat:@"%@", error.domain] }];
}

#pragma 监听
@end
