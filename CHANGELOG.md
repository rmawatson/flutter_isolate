## 2.0.3
* Fix release mode crashes on Flutter >3.3.0 - @pragma('vm:entry-point') must be added as a decorator to all entrypoint functions
* Fix occasional crash on Android due to incorrect casting
* Add flutterCompute method

## 2.0.2
* Fix bug on iOS when _isolatePluginRegistrantClassName was empty

## 2.0.1

* Fixes for hot reloading
* Adds support for custom plugin registrant

## 2.0.0

* Support null safety.
* Replace flutter_startup by path_provider in example.

## 1.0.0+15

* Fix Git example and README example.
* Update gradle/sdk dependencies. 
* Remove deprecated BinaryMessages API.

## 1.0.0+14

* Overflow bug fix on iOS.

## 1.0.0+13

* Null Context bug fix on Android.

## 1.0.0+12

* v1 embedding support on Android

## 1.0.0

* Initial implementation of an isolate that works with Flutter plugins for android/iOS
