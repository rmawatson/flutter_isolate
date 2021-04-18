#include "AppDelegate.h"
#include "GeneratedPluginRegistrant.h"
#include "FlutterIsolatePlugin.h"
#import <path_provider/FLTPathProviderPlugin.h>
#import <flutter_downloader/FlutterDownloaderPlugin.h>

@interface IsolatePluginRegistrant : NSObject
+ (void)registerWithRegistry:(NSObject<FlutterPluginRegistry>*)registry;
@end

@implementation IsolatePluginRegistrant


+ (void)registerWithRegistry:(NSObject<FlutterPluginRegistry>*)registry {
    if(![registry hasPlugin:@"FlutterDownloaderPlugin"]) {
        NSLog(@"FlutterDownloaderPlugin not found.");
        [FlutterDownloaderPlugin registerWithRegistrar:[registry registrarForPlugin:@"FlutterDownloaderPlugin"]];
    }
    [FLTPathProviderPlugin registerWithRegistrar:[registry registrarForPlugin:@"FLTPathProviderPlugin"]];
    [FlutterIsolatePlugin registerWithRegistrar:[registry registrarForPlugin:@"FlutterIsolatePlugin"]];
}

@end


@implementation AppDelegate

void registerPlugins(NSObject<FlutterPluginRegistry>* registry) {
    if(![registry hasPlugin:@"FlutterDownloaderPlugin"]) {
        [FlutterDownloaderPlugin registerWithRegistrar:[registry registrarForPlugin:@"FlutterDownloaderPlugin"]];
    }
}


- (BOOL)application:(UIApplication *)application
    didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {

    FlutterIsolatePlugin.isolatePluginRegistrantClassName = @"IsolatePluginRegistrant";
  [IsolatePluginRegistrant registerWithRegistry:self];
  [FlutterDownloaderPlugin setPluginRegistrantCallback:registerPlugins];
  return [super application:application didFinishLaunchingWithOptions:launchOptions];
}

@end
