import 'dart:async';
import 'dart:isolate';
import 'dart:ui';

import 'package:uuid/uuid.dart';
import 'package:flutter/services.dart';

class FlutterIsolate
{
  static FlutterIsolate _current;
  static final _control = MethodChannel("com.rmawatson.flutterisolate/control");
  static final _event   = EventChannel("com.rmawatson.flutterisolate/event");

  SendPort   controlPort;
  String     _isolateId;
  Capability pauseCapability;
  Capability terminateCapability;


  FlutterIsolate._([
    this._isolateId,
    this.controlPort,
    this.pauseCapability,
    this.terminateCapability]);

  static get current => _current != null ? _current : FlutterIsolate._();

  static Future spawn<T>(void entryPoint(T message),T message) async
  {
    final userEntryPointId =
    PluginUtilities.getCallbackHandle(entryPoint).toRawHandle();
    final isolateId = Uuid().v4();
    final isolateResult = Completer<FlutterIsolate>();
    final setupReceivePort = ReceivePort();

    IsolateNameServer.registerPortWithName(setupReceivePort.sendPort, isolateId);
    StreamSubscription setupSubscription;
    setupSubscription = setupReceivePort.listen((data) {

      final portSetup = (data as List<Object>);
      SendPort setupPort          = portSetup[0];
      final remoteIsolate =FlutterIsolate._(
          isolateId,
          portSetup[1],
          portSetup[2],
          portSetup[3]);

      setupPort.send([userEntryPointId, message]);

      setupSubscription.cancel();
      setupReceivePort.close();
      isolateResult.complete(remoteIsolate);

    });
    _control.invokeMethod("spawn_isolate",{
      "entry_point":PluginUtilities.getCallbackHandle(_flutterIsolateEntryPoint).toRawHandle(),
      "isolate_id":isolateId
    });
    return isolateResult.future;
  }

  bool get _isCurrentIsolate =>
      _isolateId == null || _current != null
          && _current._isolateId == _isolateId;

  void pause() =>
      _isCurrentIsolate ?
      Isolate.current.pause() :
      Isolate(controlPort,
          pauseCapability: pauseCapability,
          terminateCapability: terminateCapability).pause(pauseCapability);

  void resume() =>
      _isCurrentIsolate ?
      Isolate.current.resume(Capability()) :
      Isolate(controlPort,
          pauseCapability: pauseCapability,
          terminateCapability: terminateCapability).resume(pauseCapability);

  void kill() =>
      _isolateId != null ?
      _control.invokeMethod("kill_isolate",{"isolate_id":_isolateId}) :
      Isolate.current.kill();

  static void _isolateInitialize() {
    window.onPlatformMessage = BinaryMessages.handlePlatformMessage;

    StreamSubscription eventSubscription;
    eventSubscription = _event.receiveBroadcastStream().listen((isolateId) {

      _current = FlutterIsolate._(isolateId,null,null);
      final sendPort = IsolateNameServer.lookupPortByName(_current._isolateId);
      final setupReceivePort = ReceivePort();
      IsolateNameServer.removePortNameMapping(_current._isolateId);
      sendPort.send([
        setupReceivePort.sendPort,
        Isolate.current.controlPort,
        Isolate.current.pauseCapability,
        Isolate.current.terminateCapability]);

      StreamSubscription setupSubscription;
      setupSubscription = setupReceivePort.listen((data) {
        final args = data as List<Object>;
        final int userEntryPointHandle = args[0];
        final String userMessage = args[1];
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

