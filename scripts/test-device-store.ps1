param([string]$Serial)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME){$env:JAVA_HOME=(Get-ChildItem "$root/.tools/jdk" -Directory | Select-Object -First 1).FullName}
if (-not $env:ANDROID_HOME){$env:ANDROID_HOME="$root/.tools/android-sdk"}
$classes="$root/.tools/device-store-classes"
New-Item -ItemType Directory -Force $classes | Out-Null
$jar="$env:ANDROID_HOME/platforms/android-36/android.jar"
& "$env:JAVA_HOME/bin/javac.exe" -encoding UTF-8 -cp $jar -d $classes "$root/app/src/main/java/cn/shunshouqu/app/PickupParser.java" "$root/app/src/main/java/cn/shunshouqu/app/PickupStore.java" "$root/tests/DeviceStoreHarness.java"
if($LASTEXITCODE){throw 'Harness compilation failed'}
$files=Get-ChildItem $classes -Recurse -Filter '*.class' | ForEach-Object FullName
& "$env:ANDROID_HOME/build-tools/35.0.0/d8.bat" --min-api 26 --lib $jar --output "$root/.tools/device-store-tests.zip" @files
if($LASTEXITCODE){throw 'Harness dex compilation failed'}
$adb="$env:ANDROID_HOME/platform-tools/adb.exe"
$target=if($Serial){@('-s',$Serial)}else{@('-d')}
& $adb @target push "$root/.tools/device-store-tests.zip" /data/local/tmp/ssq-store-tests.zip
& $adb @target shell 'CLASSPATH=/data/local/tmp/ssq-store-tests.zip app_process /system/bin cn.shunshouqu.app.DeviceStoreHarness'
if($LASTEXITCODE){throw 'Android database checks failed'}
