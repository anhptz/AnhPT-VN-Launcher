$ErrorActionPreference = "Stop"

$Root = $PSScriptRoot
$JdkBin = "$Root\.build-tools\jdk-17.0.20.1+1\bin"
$BuildTools = "$Root\.build-tools\android-14"
$AndroidJar = "$Root\.build-tools\android-34\android.jar"
$Proj = "$Root\vn-launcher"
$Out = "$Proj\build"

if (Test-Path $Out) { Remove-Item -Recurse -Force $Out }
New-Item -ItemType Directory -Force -Path "$Out\gen", "$Out\classes", "$Out\dex" | Out-Null

Write-Host "[1/5] Compiling resources with aapt2..."
& "$BuildTools\aapt2.exe" compile "$Proj\res\values\strings.xml" -o "$Out\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

& "$BuildTools\aapt2.exe" link -o "$Out\unaligned.apk" `
    -I $AndroidJar `
    --manifest "$Proj\AndroidManifest.xml" `
    --java "$Out\gen" `
    "$Out\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "[2/5] Compiling Java sources with javac..."
$JavaFiles = Get-ChildItem -Recurse "$Proj\src", "$Out\gen" -Filter "*.java" | ForEach-Object { $_.FullName }
& "$JdkBin\javac.exe" -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AndroidJar;$BuildTools\core-lambda-stubs.jar" -d "$Out\classes" $JavaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "[3/5] Converting classes to DEX with d8..."
$ClassFiles = Get-ChildItem -Recurse "$Out\classes" -Filter "*.class" | ForEach-Object { $_.FullName }
& "$JdkBin\java.exe" -cp "$BuildTools\lib\d8.jar" com.android.tools.r8.D8 `
    --lib $AndroidJar `
    --min-api 26 `
    --output "$Out\dex" `
    $ClassFiles
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "[4/5] Packaging classes.dex and aligning APK..."
& "$BuildTools\aapt.exe" add "$Out\unaligned.apk" "$Out\dex\classes.dex" | Out-Null
# Note: aapt add puts full path unless run from inside $Out\dex
Remove-Item "$Out\unaligned.apk"
& "$BuildTools\aapt2.exe" link -o "$Out\unaligned.apk" `
    -I $AndroidJar `
    --manifest "$Proj\AndroidManifest.xml" `
    "$Out\res.zip"
Push-Location "$Out\dex"
& "$BuildTools\aapt.exe" add "$Out\unaligned.apk" "classes.dex" | Out-Null
Pop-Location

& "$BuildTools\zipalign.exe" -f -p 4 "$Out\unaligned.apk" "$Out\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "[5/5] Signing APK..."
$Keystore = "$Proj\debug.keystore"
if (-not (Test-Path $Keystore)) {
    & "$JdkBin\keytool.exe" -genkeypair -v `
        -keystore $Keystore `
        -storepass android `
        -alias androiddebugkey `
        -keypass android `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -dname "CN=Android Debug,O=Android,C=US" | Out-Null
}

$FinalApk = "$Root\CleanYourTV-VN.apk"
& "$JdkBin\java.exe" -jar "$BuildTools\lib\apksigner.jar" sign `
    --ks $Keystore `
    --ks-pass pass:android `
    --key-pass pass:android `
    --ks-key-alias androiddebugkey `
    --out $FinalApk `
    "$Out\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host "[SUCCESS] Built APK: $FinalApk ($((Get-Item $FinalApk).Length) bytes)"
