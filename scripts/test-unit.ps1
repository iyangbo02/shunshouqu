$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$tasks=@(':app:testDebugUnitTest')
$previousAlias=$env:SSQ_TEST_ALIAS
try {
    # Windows JDK 17 reads Gradle worker argument files in the system encoding.
    # An ASCII directory junction avoids corrupting a Chinese classpath; no source is copied.
    if($IsWindows -and $root -match '[^\x00-\x7F]') {
        $hash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($root))).Substring(0,12)
        $aliasParent=Join-Path $env:LOCALAPPDATA 'Shunshouqu/unit-test-paths'
        New-Item -ItemType Directory -Force $aliasParent | Out-Null
        $testAlias=Join-Path $aliasParent $hash
        if(Test-Path -LiteralPath $testAlias){
            $existing=Get-Item -LiteralPath $testAlias
            if($existing.LinkType -ne 'Junction' -or $existing.Target -ne $root){throw 'Test path already exists with a different target.'}
        }else{New-Item -ItemType Junction -Path $testAlias -Target $root | Out-Null}
        $env:SSQ_TEST_ALIAS=$testAlias
        New-Item -ItemType Directory -Force "$root/.tools" | Out-Null
        $initFile=Join-Path $root '.tools/unit-test-path.init.gradle'
        @'
gradle.projectsEvaluated {
    allprojects {
        tasks.withType(Test).configureEach {
            doFirst {
                def prefix = rootProject.projectDir.absolutePath + File.separator
                def alias = System.getenv('SSQ_TEST_ALIAS') + File.separator
                classpath = files(classpath.files.collect {
                    def path = it.absolutePath
                    file(path.startsWith(prefix) ? alias + path.substring(prefix.length()) : path)
                })
            }
        }
    }
}
'@ | Set-Content -LiteralPath $initFile -Encoding UTF8
        $tasks+=@('--init-script',$initFile)
    }
    & "$PSScriptRoot/build.ps1" -Tasks $tasks
}finally{$env:SSQ_TEST_ALIAS=$previousAlias}
