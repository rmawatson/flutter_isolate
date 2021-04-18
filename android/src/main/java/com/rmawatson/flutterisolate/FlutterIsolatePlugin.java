package com.rmawatson.flutterisolate;

import android.content.Context;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
<<<<<<< HEAD

=======
>>>>>>> 38fb715e0b753f684b9ae577ffe319d9f038e45e
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterJNI;
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
    static private Class registrant;
    private Context context;

    private static void registerWithCustomRegistrant(io.flutter.embedding.engine.FlutterEngine flutterEngine) {
        if (registrant == null) return;
        try {
            FlutterIsolatePlugin.registrant.getMethod("registerWith", FlutterEngine.class).invoke(null, flutterEngine);
            android.util.Log.i("FlutterIsolate", "Using custom Flutter plugin registrant " + registrant.getCanonicalName());
        } catch (NoSuchMethodException noSuchMethodException) {
            String error = noSuchMethodException.getClass().getSimpleName()
                    + ": " + noSuchMethodException.getMessage() + "\n" +
                    "The plugin registrant must provide a static registerWith(FlutterEngine) method";
            android.util.Log.e("FlutterIsolate", error);
            return;
        } catch (InvocationTargetException invocationException) {
            Throwable target = invocationException.getTargetException();
            String error = target.getClass().getSimpleName() + ": " + target.getMessage() + "\n" +
                    "It is possible the default GeneratedPluginRegistrant is attempting to register\n" +
                    "a plugin that uses registrar.activity() or a similar method. Flutter Isolates have no\n" +
                    "access to the activity() from the registrant. If the activity is being use to register\n" +
                    "a method or event channel, have the plugin use registrar.context() instead. Alternatively\n" +
                    "use a custom registrant for isolates, that only registers plugins that the isolate needs\n" +
                    "to use.";
            android.util.Log.e("FlutterIsolate", error);
            return;
        } catch (Exception except) {
            android.util.Log.e("FlutterIsolate", except.getClass().getSimpleName() + " " + ((InvocationTargetException) except).getTargetException().getMessage());
        }
    }

    /* This should be used to provide a custom plugin registrant for any FlutterIsolates that are spawned,
     * by copying the GeneratedPluginRegistrant provided by flutter call, say "IsolatePluginRegistrant", modifying the
     * list of plugins that are registered (removing the ones you do not want to use from within a plugin) and passing
     * the class to setCustomIsolateRegistrant in your MainActivity.
     *
     * FlutterIsolatePlugin.setCustomIsolateRegistrant(IsolatePluginRegistrant.class);
     *
     * The list will have to be manually maintained if plugins are added or removed, as Flutter automatically
     * regenerates GeneratedPluginRegistrant.
     */
    public static void setCustomIsolateRegistrant(Class registrant) {
        FlutterIsolatePlugin.registrant = registrant;
    }

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

        if (registrant != null) {
            isolate.engine = new FlutterEngine(context,
                    null,
                    new FlutterJNI(),
                    null,
                    false);
            registerWithRegistrantV2(isolate.engine);
        } else {
            isolate.engine = new FlutterEngine(context);
        }

        FlutterCallbackInformation cbInfo = FlutterCallbackInformation.lookupCallbackInformation(isolate.entryPoint);
        FlutterRunArguments runArgs = new FlutterRunArguments();

        runArgs.bundlePath = FlutterInjector.instance().flutterLoader().findAppBundlePath();
        runArgs.libraryPath = cbInfo.callbackLibraryPath;
        runArgs.entrypoint = cbInfo.callbackName;

        isolate.controlChannel = new MethodChannel(isolate.engine.getDartExecutor().getBinaryMessenger(), NAMESPACE + "/control");
        isolate.startupChannel = new EventChannel(isolate.engine.getDartExecutor().getBinaryMessenger(), NAMESPACE + "/event");

        isolate.startupChannel.setStreamHandler(this);
        isolate.controlChannel.setMethodCallHandler(this);

        if(registrant != null) {
            registerWithCustomRegistrant(isolate.engine);
        }
        
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
