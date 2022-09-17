import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_isolate/flutter_isolate.dart';
import 'package:path_provider/path_provider.dart';
import 'package:flutter_downloader/flutter_downloader.dart';

@pragma('vm:entry-point')
void isolate2(String arg) {
  getTemporaryDirectory().then((dir) async {
    print("isolate2 temporary directory: $dir");

    await FlutterDownloader.initialize(debug: true);
    FlutterDownloader.registerCallback(AppWidget.downloaderCallback);

    final taskId = await FlutterDownloader.enqueue(
        url:
            "https://raw.githubusercontent.com/rmawatson/flutter_isolate/master/README.md",
        savedDir: dir.path);
  });
  Timer.periodic(
      Duration(seconds: 1), (timer) => print("Timer Running From Isolate 2"));
}

@pragma('vm:entry-point')
void isolate1(String arg) async {
  await FlutterIsolate.spawn(isolate2, "hello2");

  getTemporaryDirectory().then((dir) {
    print("isolate1 temporary directory: $dir");
  });
  Timer.periodic(
      Duration(seconds: 1), (timer) => print("Timer Running From Isolate 1"));
}

void computeFunction(String arg) async {
  getTemporaryDirectory().then((dir) {
    print("Temporary directory in compute function : $dir with arg $arg");
  });
}

void main() async {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        home: Scaffold(
            appBar: AppBar(
              title: const Text('Plugin example app'),
            ),
            body: AppWidget()));
  }
}

class AppWidget extends StatelessWidget {
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
    return Column(crossAxisAlignment: CrossAxisAlignment.center, children: [
      ElevatedButton(
        child: Text('Spawn isolates'),
        onPressed: _run,
      ),
      ElevatedButton(
        child: Text('Check running isolates'),
        onPressed: () async {
          final isolates = await FlutterIsolate.runningIsolates;
          await showDialog(
              builder: (ctx) {
                return Center(child:Container(color:Colors.white, padding:EdgeInsets.all(5), child:Column(
                    children:
                        isolates.map((i) => Text(i)).cast<Widget>().toList() +
                            [
                              ElevatedButton(
                                  child: Text("Close"),
                                  onPressed: () {
                                    Navigator.of(ctx).pop();
                                  })
                            ])));
              },
              context: context);
        },
      ),
      ElevatedButton(
        child: Text('Kill all running isolates'),
        onPressed: () async {
          await FlutterIsolate.killAll();
        },),
      ElevatedButton(
        child: Text('Run in compute function'),
        onPressed: () async {
          await flutterCompute(computeFunction, "foo");
        },),
    ]);
  }
}
