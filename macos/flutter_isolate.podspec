#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_isolate'
  s.version          = '0.0.1'
  s.summary          = 'Flutter plugin for better isolates'
  s.description      = 'Flutter plugin for better isolates'
  s.homepage         = 'https://github.com/rmawatson/flutter_isolate'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'FlutterMacOS'
  s.platform = :osx, '10.11'
  s.pod_target_xcconfig = {'DEFINES_MODULE' => 'YES'}
end

