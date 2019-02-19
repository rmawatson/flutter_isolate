package com.rmawatson.flutterisolate;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.FlutterMain;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.plugin.common.PluginRegistry;

import android.content.Context;
import io.flutter.view.FlutterRunArguments;

import java.util.Queue;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;
//import java.lang.reflect.
/** FlutterIsolatePlugin */

class IsolateHolder
{
  FlutterNativeView view;
  String isolateId;

  EventChannel startupChannel;
  MethodChannel controlChannel;

  Long   entryPoint;
  Result result;
}


public class FlutterIsolatePlugin implements MethodCallHandler, StreamHandler{

  public static final String NAMESPACE = "com.rmawatson.flutterisolate";

  private final MethodChannel controlChannel;
  private final Queue<IsolateHolder> queuedIsolates;
  private final Map<String,IsolateHolder> activeIsolates;
  static private Registrar registrar;

  public static void registerWith(Registrar registrar) {
    if (FlutterIsolatePlugin.registrar == null) { // main isolates registrar
      FlutterIsolatePlugin.registrar = registrar;
      new FlutterIsolatePlugin(registrar);
    }
  }

  FlutterIsolatePlugin(Registrar registrar)
  {
    controlChannel  = new MethodChannel(registrar.messenger(), NAMESPACE + "/control");
    queuedIsolates = new LinkedList<>();
    activeIsolates  = new HashMap<>();

    controlChannel.setMethodCallHandler(this);
  }

  private void startNextIsolate() {

    IsolateHolder isolate = queuedIsolates.peek();

    FlutterMain.ensureInitializationComplete(registrar.context(),null);

    isolate.view = new FlutterNativeView(registrar.context(), true);

    FlutterCallbackInformation cbInfo = FlutterCallbackInformation.lookupCallbackInformation(isolate.entryPoint);
    FlutterRunArguments runArgs = new FlutterRunArguments();

    runArgs.bundlePath  = FlutterMain.findAppBundlePath(registrar.context());
    runArgs.libraryPath = cbInfo.callbackLibraryPath;
    runArgs.entrypoint  = cbInfo.callbackName;

    isolate.controlChannel = new MethodChannel(isolate.view,NAMESPACE + "/control");
    isolate.startupChannel = new EventChannel(isolate.view,NAMESPACE + "/event");


    isolate.startupChannel.setStreamHandler(this);
    isolate.controlChannel.setMethodCallHandler(this);

    try {
      Class.forName("io.flutter.plugins.GeneratedPluginRegistrant")
              .getMethod("registerWith", PluginRegistry.class)
              .invoke(null,isolate.view.getPluginRegistry());
    } catch(Exception  except) {
      android.util.Log.e("FlutterIsolate",except.getMessage());
    }

    isolate.view.runFromBundle(runArgs);
  }

  @Override
  public void onListen(Object o, EventChannel.EventSink sink) {

    IsolateHolder isolate = queuedIsolates.remove();
    sink.success(isolate.isolateId);
    sink.endOfStream();
    activeIsolates.put(isolate.isolateId,isolate);

    isolate.result.success(null);
    isolate.startupChannel = null;
    isolate.result = null;

    if (queuedIsolates.size() != 0)
      startNextIsolate();
  }

  @Override
  public void onCancel(Object o) {}

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("spawn_isolate")) {

      IsolateHolder isolate = new IsolateHolder();
      isolate.entryPoint = ((Number) call.argument("entry_point")).longValue();
      isolate.isolateId = call.argument("isolate_id");
      isolate.result = result;

      queuedIsolates.add(isolate);

      if (queuedIsolates.size() == 1) // no other pending isolate
        startNextIsolate();

    } else if (call.method.equals("kill_isolate")) {

      String isolateId = call.argument("isolate_id");
      activeIsolates.get(isolateId).view.destroy();
      activeIsolates.remove(isolateId);

    } else {
      result.notImplemented();
    }
  }


}
