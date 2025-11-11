#import "AdScope.h"

@implementation AdScope {
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

RCT_EXPORT_METHOD(init:(NSDictionary *)param resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {

}

RCT_EXPORT_METHOD(loadSplashAd:(NSDictionary *)param resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
}



@end
