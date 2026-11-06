$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME) { $env:JAVA_HOME=(Get-ChildItem "$root/.tools/jdk" -Directory | Select-Object -First 1).FullName }
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME="$root/.tools/android-sdk" }
$signingDirectory=Join-Path $env:LOCALAPPDATA 'Shunshouqu/signing'
$store=Join-Path $signingDirectory 'release.p12'
$protectedPassword=Join-Path $signingDirectory 'password.xml'
New-Item -ItemType Directory -Force $signingDirectory | Out-Null
if ((Test-Path $store) -xor (Test-Path $protectedPassword)) { throw 'Incomplete signing material; restore the original key and protected password. Do not generate a replacement.' }
if (-not (Test-Path $store)) {
    # DPAPI encrypts this password for the current Windows user. Never save plaintext in the project.
    $randomBytes=[byte[]]::new(48)
    [Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
    $secure=ConvertTo-SecureString ([Convert]::ToBase64String($randomBytes)) -AsPlainText -Force
    $secure | Export-Clixml -LiteralPath $protectedPassword
}
$secure=Import-Clixml -LiteralPath $protectedPassword
$env:SSQ_RELEASE_PASSWORD=[Net.NetworkCredential]::new('',$secure).Password
$env:SSQ_RELEASE_STORE=$store
try {
    if (-not (Test-Path $store)) {
        & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $store -storetype PKCS12 -storepass:env SSQ_RELEASE_PASSWORD -keypass:env SSQ_RELEASE_PASSWORD -alias shunshouqu-release -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Shunshouqu App, OU=Mobile, O=Shunshouqu, C=CN' -noprompt
        if ($LASTEXITCODE) { throw 'Release key generation failed' }
    }
    Push-Location $root
    try {
        & ./gradlew.bat :app:assembleRelease :app:lintRelease --console plain
        if ($LASTEXITCODE) { throw 'Release build failed' }
        & "$env:JAVA_HOME/bin/java.exe" -jar "$env:ANDROID_HOME/build-tools/35.0.0/lib/apksigner.jar" verify --print-certs app/build/outputs/apk/release/app-release.apk
        if ($LASTEXITCODE) { throw 'Release signature verification failed' }
        $apkMetadata=Get-Content app/build/outputs/apk/release/output-metadata.json -Raw | ConvertFrom-Json
        $appVersion=$apkMetadata.elements[0].versionName
        New-Item -ItemType Directory -Force delivery | Out-Null
        Copy-Item app/build/outputs/apk/release/app-release.apk "delivery/顺手取-$appVersion-release.apk" -Force
    } finally { Pop-Location }
} finally {
    Remove-Item Env:SSQ_RELEASE_PASSWORD,Env:SSQ_RELEASE_STORE -ErrorAction SilentlyContinue
}
