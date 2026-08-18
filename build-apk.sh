#!/bin/zsh
# Build a signed, installable Android APK in Kotlin with Mess Ledger web core.
set -euo pipefail

project_root="${0:A:h}"
workspace_root="${project_root:h}"
web_dir="${workspace_root}/web"

sdk_root="${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}"
java_home="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
kotlinc_bin="/Applications/Android Studio.app/Contents/plugins/Kotlin/kotlinc/bin/kotlinc"
kotlin_stdlib="/Applications/Android Studio.app/Contents/plugins/Kotlin/kotlinc/lib/kotlin-stdlib.jar"

# Detect latest available build tools
if [[ -d "${sdk_root}/build-tools/36.0.0" ]]; then
  build_tools="${sdk_root}/build-tools/36.0.0"
elif [[ -d "${sdk_root}/build-tools/35.0.0" ]]; then
  build_tools="${sdk_root}/build-tools/35.0.0"
else
  build_tools="${sdk_root}/build-tools/34.0.0"
fi

platform_jar="${sdk_root}/platforms/android-34/android.jar"
build_dir="${project_root}/build"
assets_dir="${project_root}/assets"
unsigned_apk="${build_dir}/MessLedger-unsigned.apk"
aligned_apk="${build_dir}/MessLedger-aligned.apk"
apk="${build_dir}/messledger.apk"
keystore="${build_dir}/mess-ledger-debug.keystore"

export JAVA_HOME="${java_home}"
export PATH="${JAVA_HOME}/bin:${PATH}"

for required in "${platform_jar}" "${build_tools}/aapt2" "${build_tools}/d8" "${build_tools}/zipalign" "${build_tools}/apksigner" "${java_home}/bin/javac" "${kotlinc_bin}" "${kotlin_stdlib}"; do
  [[ -e "${required}" ]] || { print -u2 "Missing required tool/dependency: ${required}"; exit 1; }
done

print "==> Cleaning build directories..."
rm -rf "${build_dir}" "${assets_dir}"
mkdir -p "${build_dir}/classes" "${build_dir}/generated" "${build_dir}/res-compiled" "${build_dir}/dex" "${assets_dir}"

print "==> Copying web core assets from ${web_dir}..."
cp -R "${web_dir}/." "${assets_dir}/"
rm -rf "${assets_dir}/.DS_Store"

print "==> Compiling Android resources with AAPT2..."
"${build_tools}/aapt2" compile --dir "${project_root}/res" -o "${build_dir}/res-compiled"
resource_files=("${build_dir}/res-compiled"/*.flat)
"${build_tools}/aapt2" link \
  -I "${platform_jar}" \
  --manifest "${project_root}/AndroidManifest.xml" \
  -R "${resource_files[@]}" \
  --auto-add-overlay \
  --java "${build_dir}/generated" \
  -o "${build_dir}/resources.apk"

print "==> Compiling R.java..."
"${java_home}/bin/javac" \
  -source 8 -target 8 \
  -bootclasspath "${platform_jar}" \
  -d "${build_dir}/classes" \
  "${build_dir}/generated/com/messledger/app/R.java"

print "==> Compiling Kotlin source..."
"${kotlinc_bin}" \
  -jvm-target 1.8 \
  -cp "${platform_jar}:${build_dir}/classes:${kotlin_stdlib}" \
  -d "${build_dir}/classes" \
  "${project_root}/src/com/messledger/app/MainActivity.kt"

print "==> Dexing Kotlin bytecode & standard library with D8..."
class_files=("${build_dir}/classes"/**/*.class)
"${build_tools}/d8" \
  --min-api 23 \
  --lib "${platform_jar}" \
  --output "${build_dir}/dex" \
  "${kotlin_stdlib}" \
  "${class_files[@]}"

print "==> Packaging APK..."
cp "${build_dir}/resources.apk" "${unsigned_apk}"
(cd "${build_dir}/dex" && zip -q -u "${unsigned_apk}" classes.dex)
(cd "${project_root}" && zip -q -r "${unsigned_apk}" assets)

print "==> Zipaligning APK..."
"${build_tools}/zipalign" -f 4 "${unsigned_apk}" "${aligned_apk}"

print "==> Signing APK with debug certificate..."
if [[ ! -f "${keystore}" ]]; then
  "${java_home}/bin/keytool" -genkeypair -keystore "${keystore}" -storepass android -keypass android \
    -alias mess-ledger -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Mess Ledger Debug" >/dev/null 2>&1
fi

"${build_tools}/apksigner" sign --ks "${keystore}" --ks-pass pass:android --key-pass pass:android \
  --out "${apk}" "${aligned_apk}"

print "==> Verifying APK signature..."
"${build_tools}/apksigner" verify --verbose "${apk}"

print "========================================================"
print " Successfully built: ${apk}"
print "========================================================"
