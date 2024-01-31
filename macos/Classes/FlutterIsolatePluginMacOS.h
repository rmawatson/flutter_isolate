#import <FlutterMacOS/FlutterMacOS.h>

#define FLUTTER_ISOLATE_NAMESPACE @"com.rmawatson.flutterisolate"

@interface FlutterIsolatePluginMacOS : NSObject<FlutterPlugin,FlutterStreamHandler>

@property (class) NSString* isolatePluginRegistrantClassName;

@end
