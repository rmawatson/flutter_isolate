import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter_startup/flutter_startup.dart';
import 'package:flutter_isolate/flutter_isolate.dart';

void isolate2(String arg) {
  FlutterStartup.startupReason.then((reason) {
    print("Isolate2 $reason");
  });
  Timer.periodic(
      Duration(seconds: 1), (timer) => print("Timer Running From Isolate 2"));
}

void isolate1(String arg) async {
  final isolate = await FlutterIsolate.spawn(isolate2, "hello2");

  FlutterStartup.startupReason.then((reason) {
    print("Isolate1 $reason");
  });
  Timer.periodic(
      Duration(seconds: 1), (timer) => print("Timer Running From Isolate 1"));
}

void main() async {
  final isolate = await FlutterIsolate.spawn(isolate1, "hello");
  Timer(Duration(seconds: 5), () {
    print("Pausing Isolate 1");
    isolate.pause();
  });
  Timer(Duration(seconds: 10), () {
    print("Resuming Isolate 1");
    isolate.resume();
  });
  Timer(Duration(seconds: 20), () {
    print("Killing Isolate 1");
    isolate.kill();
  });

  runApp(MyApp());
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await FlutterIsolate.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n'),
        ),
      ),
    );
  }
}
