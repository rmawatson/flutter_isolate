import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_isolate/flutter_isolate.dart';
import 'package:path_provider/path_provider.dart';
import 'package:flutter_downloader/flutter_downloader.dart';

void isolate2(String arg) {
  getTemporaryDirectory().then((dir) async {
    print("isolate2 temporary directory: $dir");

    await FlutterDownloader.initialize(debug: true);
    FlutterDownloader.registerCallback(_MyAppState.downloaderCallback);

    final taskId = await FlutterDownloader.enqueue(
        url:
            "https://raw.githubusercontent.com/rmawatson/flutter_isolate/master/README.md",
        savedDir: dir.path);
  });
  Timer.periodic(
      Duration(seconds: 1), (timer) => print("Timer Running From Isolate 2"));
}

void isolate1(String arg) async {
  /*final isolate =*/ await FlutterIsolate.spawn(isolate2, "hello2");

  getTemporaryDirectory().then((dir) {
    print("isolate1 temporary directory: $dir");
  });
  Timer.periodic(
      Duration(seconds: 1), (timer) => print("Timer Running From Isolate 1"));
}

void main() async {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  static void downloaderCallback(
      String id, DownloadTaskStatus status, int progress) {
    print("progress: $progress");
  }

  Future<void> _run() async {
    print(
        "Temp directory in main isolate : ${(await getTemporaryDirectory()).path}");
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
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: ElevatedButton(
            child: Text('Run'),
            onPressed: _run,
          ),
        ),
      ),
    );
  }
}
