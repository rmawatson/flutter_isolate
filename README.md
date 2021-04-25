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



