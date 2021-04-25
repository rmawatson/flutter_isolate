# FlutterIsolate

FlutterIsolate allows creation of an Isolate in flutter that is able to use flutter plugins. It creates the necessary platform specific bits (FlutterBackgroundView on android & FlutterEngine on iOS) to enable the platform channels to work inside an isolate.

### FlutterIsolate API

|                  |      Android       |         iOS          |             Description            |
| :--------------- | :----------------: | :------------------: |  :-------------------------------- |
| FlutterIsolate.spawn(entryPoint,message)             | :white_check_mark: |  :white_check_mark:  | spawns a new FlutterIsolate        |
| flutterIsolate.pause()            | :white_check_mark: |  :white_check_mark:  | pauses a running isolate |
| flutterIsolate.resume()           | :white_check_mark: |  :white_check_mark:  | resumed a paused isoalte |
| flutterIsolate.kill()             | :white_check_mark: |  :white_check_mark:  | kills a an isolate |

### Example

This is available in the example included with the package.

```
import 'package:flutter_startup/flutter_startup.dart';
import 'package:flutter_isolate/flutter_isolate.dart';

void isolate2(String arg) {
  FlutterStartup.startupReason.then((reason){
    print("Isolate2 $reason");
  });
  Timer.periodic(Duration(seconds:1),(timer)=>print("Timer Running From Isolate 2"));
}

void isolate1(String arg) async  {

  final isolate = await FlutterIsolate.spawn(isolate2, "hello2");

  FlutterStartup.startupReason.then((reason){
    print("Isolate1 $reason");
  });
  Timer.periodic(Duration(seconds:1),(timer)=>print("Timer Running From Isolate 1"));
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final isolate = await FlutterIsolate.spawn(isolate1, "hello");
  Timer(Duration(seconds:5), (){print("Pausing Isolate 1");isolate.pause();});
  Timer(Duration(seconds:10),(){print("Resuming Isolate 1");isolate.resume();});
  Timer(Duration(seconds:20),(){print("Killing Isolate 1");isolate.kill();});

  runApp(MyApp());
}
...
```

### Notes

Due to a FlutterIsolate being backed by a platform specific 'view', the event loop will not terminate when there is no more 'user' work left to do and FlutterIsolates will require explict termination with kill().

Additionally this plugin has not been tested with a large range of plugins, only a small subset I have been using such as flutter_notification, flutter_blue and flutter_startup.

### Communicating between isolates

To pass data between isolates, a ReceivePort should be created on your (parent) isolate with the corresponding SendPort sent via the isolate constructor:

```

void spawnIsolate(SendPort port) {
  port.send("Hello!");
}

void main() {
  var port = ReceivePort();
  port.listen((msg) {
    print("Received message from isolate $msg");
  });
  var isolate = await FlutterIsolate.spawn(spawnIsolate, port.sendPort);

}

```

[Only primitives can be sent via a SendPort.](https://api.flutter.dev/flutter/dart-isolate/SendPort/send.html) for further details.


### Custom plugin registrant

See the example project for a sample implementation using a custom plugin registrant.

#### iOS

By default, `flutter_isolate` will register all plugins provided by Flutter's automatically generated `GeneratedPluginRegistrant.m` file.

If you want to register, e.g. some custom `FlutterMethodChannel`s, you can define a custom registrant:

```swift
// Defines a custom plugin registrant, to be used specifically together with FlutterIsolatePlugin
@objc(IsolatePluginRegistrant) class IsolatePluginRegistrant: NSObject {
    @objc static func register(withRegistry registry: FlutterPluginRegistry) {
        // Register channels for Flutter Isolate
        registerMethodChannelABC(bm: registry.registrar(forPlugin: "net.myapp.myChannelABC").messenger())

        // Register default plugins
        GeneratedPluginRegistrant.register(with: registry)
    }
}

// In AppDelegate.swift
@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
    override func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        let controller = window.rootViewController as! FlutterViewController

        // Register custom channels for Flutter
        registerMethodChannelABC(bm: controller.binaryMessenger) // <-- the custom method channel

        // Point FlutterIsolatePlugin to use our previously defined custom registrant.
        // The string content must be equal to the plugin registrant class annotation 
        // value: @objc(IsolatePluginRegistrant)
        FlutterIsolatePlugin.isolatePluginRegistrantClassName = "IsolatePluginRegistrant" // <--

        GeneratedPluginRegistrant.register(with: self)
        return super.application(application, didFinishLaunchingWithOptions: launchOptions)
    }
}

#### Android

Define a custom plugin registrant, to be used specifically together with FlutterIsolatePlugin:

```Java
public final class CustomPluginRegistrant {
  public static void registerWith(@NonNull FlutterEngine flutterEngine) {
    flutterEngine.getPlugins().add(... [your plugin goes here]);
  }
}
```

Create a MainApplication class that sets this custom isolate registrant:
```Java
  public class MainApplication extends FlutterApplication {
    public MainApplication() {
        FlutterIsolatePlugin.setCustomIsolateRegistrant(CustomPluginRegistrant.class);
    }
}
```