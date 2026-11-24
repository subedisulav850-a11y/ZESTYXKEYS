# FF AES / IV Extractor (Android)

VPN-style foreground service + Frida based AES key & IV extractor for any game.

## Features
- Type any game name + package
- VPN-type always-on foreground service
- Background + battery exemption
- Root + frida-server required
- Saves keys to `/sdcard/Download/ff_keys.txt`

## Requirements
- Rooted Android device
- Matching `frida-server` binary at `/data/local/tmp/frida-server`
- `frida` CLI available under `su`

## Build
Open in Android Studio or run GitHub Actions workflow.

## Usage
1. Install APK
2. Grant Superuser + VPN + Battery unrestricted
3. Enter package (e.g. `com.garena.game.ffi`)
4. Start Extractor
5. Open target game → Login
6. Keys appear in app + `/sdcard/Download/ff_keys.txt`
