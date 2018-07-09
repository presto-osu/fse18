
## Install APK

To install the instrumented APK, run the following commands.

```bash
adb forward tcp:4444 localabstract:/adb-hub
adb connect 127.0.0.1:4444
```