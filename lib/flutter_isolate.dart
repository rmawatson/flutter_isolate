import 'dart:async';
import 'dart:isolate';
import 'dart:ui';

import 'package:uuid/uuid.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class FlutterIsolate {
  /// Control port used to send control messages to the isolate.
  SendPort controlPort;

  /// Capability granting the ability to pause the isolate (not implemented)
  Capability pauseCapability;

  /// Capability granting the ability to terminate the isolate (not implemented)
  Capability terminateCapability;

  /// Creates and spawns a flutter isolate that shares the same code
  /// as the current isolate. The spawned isolate will be able to use flutter
  /// plugins. T can be any type that can be normally be passed through to
  /// regular isolate's entry point.
  static Future spawn<T>(void entryPoint(T message), T message) async {
    final userEntryPointId =
        PluginUtilities.getCallbackHandle(entryPoint).toRawHandle();
    final isolateId = Uuid().v4();
    final isolateResult = Completer<FlutterIsolate>();
    final setupReceivePort = ReceivePort();

    IsolateNameServer.registerPortWithName(
        setupReceivePort.sendPort, isolateId);
    StreamSubscription setupSubscription;
    setupSubscription = setupReceivePort.listen((data) {
      final portSetup = (data as List<Object>);
      SendPort setupPort = portSetup[0];
      final remoteIsolate =
          FlutterIsolate._(isolateId, portSetup[1], portSetup[2], portSetup[3]);

      setupPort.send([userEntryPointId, message]);

      setupSubscription.cancel();
      setupReceivePort.close();
      isolateResult.complete(remoteIsolate);
    });
    _control.invokeMethod("spawn_isolate", {
      "entry_point":
          PluginUtilities.getCallbackHandle(_flutterIsolateEntryPoint)
              .toRawHandle(),
      "isolate_id": isolateId
    });
    return isolateResult.future;
  }

  bool get _isCurrentIsolate =>
      _isolateId == null ||
      _current != null && _current._isolateId == _isolateId;

  /// Requests the isolate to pause. This uses the underlying isolates pause
  /// implementation to pause the isolate from with the pausing isolate
  /// otherwises uses a SendPort to pass through a pause requres to the target
  void pause() => _isCurrentIsolate
      ? Isolate.current.pause()
      : Isolate(controlPort,
              pauseCapability: pauseCapability,
              terminateCapability: terminateCapability)
          .pause(pauseCapability);

  /// Requests the isolate to resume. This uses the underlying isolates resume
  /// implementation to as it takes advangtage of functionality that is not
  /// exposed, ie sending 'out of band' messages to an isolate. Regular 'user'
  /// ports will not be serviced when an isolate is paused.
  void resume() => _isCurrentIsolate
      ? Isolate.current.resume(Capability())
      : Isolate(controlPort,
              pauseCapability: pauseCapability,
              terminateCapability: terminateCapability)
          .resume(pauseCapability);

  /// Requestes to terminate the flutter isolate. As the isolate that is
  /// created is backed by a FlutterBackgroundView/FlutterEngine for the
  /// platform implementations, the event loop will continue to execute
  /// even after user code has completed. Thus they must be explicitly
  /// terminate using kill if you wish to dispose of them after you have
  /// finished. This should cleanup the native components backing the isolates.
  void kill() => _isolateId != null
      ? _control.invokeMethod("kill_isolate", {"isolate_id": _isolateId})
      : Isolate.current.kill();

  String _isolateId;
  static FlutterIsolate _current;
  static final _control = MethodChannel("com.rmawatson.flutterisolate/control");
  static final _event = EventChannel("com.rmawatson.flutterisolate/event");

  FlutterIsolate._(
      [this._isolateId,
      this.controlPort,
      this.pauseCapability,
      this.terminateCapability]);

  static get current => _current != null ? _current : FlutterIsolate._();
  static void _isolateInitialize() {
    WidgetsFlutterBinding.ensureInitialized();
    window.onPlatformMessage = BinaryMessages.handlePlatformMessage;

    StreamSubscription eventSubscription;
    eventSubscription = _event.receiveBroadcastStream().listen((isolateId) {
      _current = FlutterIsolate._(isolateId, null, null);
      final sendPort = IsolateNameServer.lookupPortByName(_current._isolateId);
      final setupReceivePort = ReceivePort();
      IsolateNameServer.removePortNameMapping(_current._isolateId);
      sendPort.send([
        setupReceivePort.sendPort,
        Isolate.current.controlPort,
        Isolate.current.pauseCapability,
        Isolate.current.terminateCapability
      ]);
      eventSubscription.cancel();

      StreamSubscription setupSubscription;
      setupSubscription = setupReceivePort.listen((data) {
        final args = data as List<Object>;
        final int userEntryPointHandle = args[0];
        final userMessage = args[1];
        Function userEntryPoint = PluginUtilities.getCallbackFromHandle(
            CallbackHandle.fromRawHandle(userEntryPointHandle));
        setupSubscription.cancel();
        setupReceivePort.close();
        userEntryPoint(userMessage);
      });
    });
  }
}

void _flutterIsolateEntryPoint() => FlutterIsolate._isolateInitialize();
