package com.example.flutterisolateexample;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.FlutterEngine;

/*
This is a copy of the original GeneratedPluginRegistrant, provided to explain how to use a custom
plugin registrant.
 */
@Keep
public final class CustomPluginRegistrant {
  public static void registerWith(@NonNull FlutterEngine flutterEngine) {
    flutterEngine.getPlugins().add(new com.rmawatson.flutterisolate.FlutterIsolatePlugin());
    flutterEngine.getPlugins().add(new io.flutter.plugins.pathprovider.PathProviderPlugin());
  }
}
