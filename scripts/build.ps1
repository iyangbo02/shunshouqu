param([string[]]$Tasks=@(':app:assembleDebug',':app:assembleDebugAndroidTest',':notification-fixture:assembleDebug',':app:lintDebug'))
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME) { $env:JAVA_HOME=(Get-ChildItem "$root/.tools/jdk" -Directory | Select-Object -First 1).FullName }
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME="$root/.tools/android-sdk" }
Push-Location $root
try {
  if (-not (Test-Path './gradlew.bat')) { & './.tools/gradle/gradle-8.13/bin/gradle.bat' wrapper --gradle-version 8.13; if ($LASTEXITCODE) { throw 'Wrapper generation failed' } }
  & './gradlew.bat' @Tasks
  if ($LASTEXITCODE) { throw 'Build failed' }
} finally { Pop-Location }
