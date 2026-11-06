$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME) { $env:JAVA_HOME=(Get-ChildItem "$root/.tools/jdk" -Directory | Select-Object -First 1).FullName }
New-Item -ItemType Directory -Force "$root/.tools/test-classes" | Out-Null
& "$env:JAVA_HOME/bin/javac.exe" -encoding UTF-8 -d "$root/.tools/test-classes" "$root/app/src/main/java/cn/shunshouqu/app/PickupParser.java" "$root/tests/ParserTests.java"
if ($LASTEXITCODE) { throw 'Compilation failed' }
& "$env:JAVA_HOME/bin/java.exe" -cp "$root/.tools/test-classes" ParserTests
if ($LASTEXITCODE) { throw 'Parser tests failed' }
