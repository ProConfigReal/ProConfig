# ProConfig

A clean, bilingual (English / Persian) network profile manager for Android with a pure black-purple "Liquid Glass" UI, powered by the Xray core.

## Features

- **Panel-style home screen**: connection status, live up/down speed, session totals and duration, one-tap connect button
- **Profile manager**: add/edit VMess, VLESS, Trojan and Shadowsocks profiles manually or import from links (vmess/vless/trojan/ss)
- **Subscriptions**: import and refresh remote profile lists, latency preserved between refreshes
- **Latency test**: per-profile TCP ping and bulk test-all
- **Modern transports**: TLS, Reality (pbk/sid/spiderX), WebSocket, gRPC, H2
- **Bilingual UI**: English + Persian (RTL), per-app language selection (Android 13+ per-app locales with autoStore)
- **Themes**: Dark (pure black + purple, default), Light (white + purple), System
- **Live stats**: tun2socks REST API traffic stream, notification with stop action

## Tech

| Component | Version |
|---|---|
| Xray-core | v26.3.27 (static, arm64-v8a + x86_64) |
| tun2socks | v2.7.0 (static, restapi /traffic) |
| Android Gradle Plugin | 8.5.2 |
| Gradle | 8.7 |
| Kotlin | 2.0.20 |
| Min / Target SDK | 24 / 34 |

## Architecture

```
VpnService.establish()  ->  TUN fd
      |
      +-- libexec_helper.so (JNI fork+execve, clears FD_CLOEXEC on the TUN fd)
      |       |-- libxray.so      run -c config.json   (socks5 127.0.0.1:10808)
      |       +-- libtun2socks.so --device fd://N --proxy socks5://127.0.0.1:10808 --restapi 127.0.0.1:10810
      |
      +-- Self-bypass: addDisallowedApplication(packageName) keeps core traffic out of the tunnel
```

- `exec_helper` is a tiny JNI library that performs `fork()` + `execve()` and clears `FD_CLOEXEC`
  on the inherited TUN fd (Java `Os.fork()` is not public API).
- Xray and tun2socks ship as static binaries packaged as `libxray.so` / `libtun2socks.so` inside
  `jniLibs` (`useLegacyPackaging = true`), extracted by the system to `nativeLibraryDir` and
  executed from there — the Android 10+ W^X-compliant path.
- Traffic stats stream from tun2socks `GET /traffic` (JSON lines, per-second).

## Build

Requirements: JDK 17, Android SDK 34, NDK r26d (or any modern NDK), Gradle 8.7.

```bash
# 1. Build exec_helper (once)
cd exec_helper
ndk-build NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=./Android.mk NDK_APPLICATION_MK=./Application.mk
cp libs/arm64-v8a/libexec_helper.so ../app/src/main/jniLibs/arm64-v8a/
cp libs/x86_64/libexec_helper.so   ../app/src/main/jniLibs/x86_64/

# 2. Assemble
./gradlew assembleRelease
```

The release keystore is **not** committed. Create your own:

```bash
keytool -genkeypair -v -keystore keystore/proconfig.jks -alias proconfig \
  -keyalg RSA -keysize 2048 -validity 10950 \
  -storepass '<pass>' -keypass '<pass>' \
  -dname "CN=ProConfig, O=ProConfig, C=IR"
```

and update `app/build.gradle.kts` signing config accordingly.

## Project layout

```
app/src/main/java/com/proconfig/app/
  core/      ProxyConfig, ConfigParser (vmess/vless/trojan/ss), XrayConfigBuilder,
             ConfigRepository (local JSON storage), SubscriptionManager (OkHttp)
  service/   ProVpnService (VPN lifecycle), ProcessRunner (JNI), StateBus (LiveData),
             TrafficMonitor (tun2socks /traffic stream)
  ui/        Home (panel), Configs, Subscriptions, Settings, About, edit dialog, adapters
  util/      Prefs (SharedPreferences)
exec_helper/ JNI fork+exec library (C)
```

## License

MIT
