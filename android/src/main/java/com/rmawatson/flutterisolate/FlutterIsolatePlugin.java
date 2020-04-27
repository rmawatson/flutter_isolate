package com.rmawatson.flutterisolate;

import io.flutter.app.FlutterPluginRegistry;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.FlutterMain;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterRunArguments;

import java.lang.reflect.InvocationTargetException;
import java.util.Queue;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;

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
  static private Class registrant;
  private Registrar registrar;


  private static void registerWithRegistrant(FlutterPluginRegistry registry)
  {
    try {
      Class registrant = FlutterIsolatePlugin.registrant == null ?
              Class.forName("io.flutter.plugins.GeneratedPluginRegistrant") :
              FlutterIsolatePlugin.registrant;
      registrant.getMethod("registerWith", PluginRegistry.class).invoke(null, registry);
    } catch(ClassNotFoundException classNotFoundException) {
      String error = classNotFoundException.getClass().getSimpleName()
              + ": " + classNotFoundException.getMessage() + "\n" +
              "Unable to find the default GeneratedPluginRegistrant.";
      android.util.Log.e("FlutterIsolate", error);
      return;
    } catch(NoSuchMethodException noSuchMethodException) {
      String error = noSuchMethodException.getClass().getSimpleName()
              + ": " + noSuchMethodException.getMessage() + "\n" +
              "The plugin registrant must provide a static registerWith(FlutterPluginRegistry) method";
      android.util.Log.e("FlutterIsolate", error);
      return;
    } catch(InvocationTargetException invocationException) {
      Throwable target = invocationException.getTargetException();
      String error = target.getClass().getSimpleName() + ": " + target.getMessage() + "\n" +
              "It is possible the default GeneratedPluginRegistrant is attempting to register\n" +
              "a plugin that uses registrar.activity() or a similar method. Flutter Isolates have no\n" +
              "access to the activity() from the registrant. If the activity is being use to register\n" +
              "a method or event channel, have the plugin use registrar.context() instead. Alternatively\n" +
              "use a custom registrant for isolates, that only registers plugins that the isolate needs\n" +
              "to use.";
      android.util.Log.e("FlutterIsolate",error);
      return;
    }
    catch(Exception  except) {
      android.util.Log.e("FlutterIsolate",except.getClass().getSimpleName() + " " + ((InvocationTargetException)except).getTargetException().getMessage());
    }
  }

  /* This should be used to provides a custom plugin registrant for any FlutterIsolates that are spawned.
   * by copying the GeneratedPluginRegistrant provided by flutter call say "IsolatePluginRegistrant", modifying the
   * list of plugins that are registered (removing the ones you do not want to use from within a plugin) and passing
   * the class to setCustomIsolateRegistrant in your MainActivity.
   *
   * FlutterIsolatePlugin.setCustomIsolateRegistrant(IsolatePluginRegistrant.class);
   *
   * The list will have to be manually maintained if plugins are added or removed, as Flutter automatically
   * regenerates GeneratedPluginRegistrant.
   */
  public static void setCustomIsolateRegistrant(Class registrant)
  {
    FlutterIsolatePlugin.registrant = registrant;
  }

  public static void registerWith(Registrar registrar) {
    new FlutterIsolatePlugin(registrar);
  }

  FlutterIsolatePlugin(Registrar registrar)
  {
    this.registrar = registrar;
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

    registerWithRegistrant(isolate.view.getPluginRegistry());
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
