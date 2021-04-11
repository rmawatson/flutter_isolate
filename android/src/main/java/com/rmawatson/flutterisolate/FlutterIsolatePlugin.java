package com.rmawatson.flutterisolate;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.view.FlutterRunArguments;

/**
 * FlutterIsolatePlugin
 */

class IsolateHolder {
    FlutterEngine engine;
    String isolateId;

    EventChannel startupChannel;
    MethodChannel controlChannel;

    Long entryPoint;
    Result result;
}

public class FlutterIsolatePlugin implements FlutterPlugin, MethodCallHandler, StreamHandler {

    public static final String NAMESPACE = "com.rmawatson.flutterisolate";

    private Queue<IsolateHolder> queuedIsolates;
    private Map<String, IsolateHolder> activeIsolates;
    private Context context;

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        setupChannel(binding.getBinaryMessenger(), binding.getApplicationContext());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    }

    private void setupChannel(BinaryMessenger messenger, Context context) {
        this.context = context;
        MethodChannel controlChannel = new MethodChannel(messenger, NAMESPACE + "/control");
        queuedIsolates = new LinkedList<>();
        activeIsolates = new HashMap<>();

        controlChannel.setMethodCallHandler(this);
    }

    private void startNextIsolate() {
        IsolateHolder isolate = queuedIsolates.peek();

        FlutterInjector.instance().flutterLoader().ensureInitializationComplete(context, null);

        isolate.engine = new FlutterEngine(context);

        FlutterCallbackInformation cbInfo = FlutterCallbackInformation.lookupCallbackInformation(isolate.entryPoint);
        FlutterRunArguments runArgs = new FlutterRunArguments();

        runArgs.bundlePath = FlutterInjector.instance().flutterLoader().findAppBundlePath();
        runArgs.libraryPath = cbInfo.callbackLibraryPath;
        runArgs.entrypoint = cbInfo.callbackName;

        isolate.controlChannel = new MethodChannel(isolate.engine.getDartExecutor().getBinaryMessenger(), NAMESPACE + "/control");
        isolate.startupChannel = new EventChannel(isolate.engine.getDartExecutor().getBinaryMessenger(), NAMESPACE + "/event");

        isolate.startupChannel.setStreamHandler(this);
        isolate.controlChannel.setMethodCallHandler(this);

        DartExecutor.DartCallback dartCallback = new DartExecutor.DartCallback(context.getAssets(), runArgs.bundlePath, cbInfo);
        isolate.engine.getDartExecutor().executeDartCallback(dartCallback);
    }

    @Override
    public void onListen(Object o, EventChannel.EventSink sink) {
        IsolateHolder isolate = queuedIsolates.remove();
        sink.success(isolate.isolateId);
        sink.endOfStream();
        activeIsolates.put(isolate.isolateId, isolate);

        isolate.result.success(null);
        isolate.startupChannel = null;
        isolate.result = null;

        if (queuedIsolates.size() != 0) {
            startNextIsolate();
        }
    }

    @Override
    public void onCancel(Object o) {
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        if (call.method.equals("spawn_isolate")) {
            IsolateHolder isolate = new IsolateHolder();
            isolate.entryPoint = call.argument("entry_point");
            isolate.isolateId = call.argument("isolate_id");
            isolate.result = result;

            queuedIsolates.add(isolate);

            if (queuedIsolates.size() == 1) { // no other pending isolate
                startNextIsolate();
            }
        } else if (call.method.equals("kill_isolate")) {
            String isolateId = call.argument("isolate_id");

            activeIsolates.get(isolateId).engine.destroy();
            activeIsolates.remove(isolateId);
        } else {
            result.notImplemented();
        }
    }
}
