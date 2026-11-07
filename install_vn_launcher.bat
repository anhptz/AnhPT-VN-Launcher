@echo off
chcp 65001 >nul
cd /d "%~dp0"
set ADB=platform-tools\adb.exe
set TV_IP=192.168.8.15
if not "%~1"=="" set TV_IP=%~1

echo ========================================================
echo   CAI DAT GIAO DIEN CLEANYOURTV-VN CHO MECOOL KM2
echo ========================================================
echo.
echo [1/4] Dang ket noi ADB toi %TV_IP%:5555...
ping -n 2 -w 1000 %TV_IP% >nul
"%ADB%" connect %TV_IP%:5555

echo.
echo [2/4] Dang cai dat CleanYourTV-VN.apk...
"%ADB%" -s %TV_IP%:5555 push CleanYourTV-VN.apk /data/local/tmp/CleanYourTV-VN.apk
"%ADB%" -s %TV_IP%:5555 shell pm install -r /data/local/tmp/CleanYourTV-VN.apk
"%ADB%" -s %TV_IP%:5555 shell rm -f /data/local/tmp/CleanYourTV-VN.apk

echo.
echo [3/4] Dang khoi chay va dat lam Launcher mac dinh...
"%ADB%" -s %TV_IP%:5555 shell pm grant vn.cleanyourtv.dashboard android.permission.ACCESS_FINE_LOCATION
"%ADB%" -s %TV_IP%:5555 shell pm grant vn.cleanyourtv.dashboard android.permission.ACCESS_COARSE_LOCATION
"%ADB%" -s %TV_IP%:5555 shell pm grant vn.cleanyourtv.dashboard android.permission.READ_MEDIA_IMAGES
"%ADB%" -s %TV_IP%:5555 shell pm grant vn.cleanyourtv.dashboard android.permission.READ_EXTERNAL_STORAGE
"%ADB%" -s %TV_IP%:5555 shell cmd notification allow_listener vn.cleanyourtv.dashboard/vn.cleanyourtv.dashboard.TvNotificationListener
"%ADB%" -s %TV_IP%:5555 shell settings delete secure enabled_accessibility_services
"%ADB%" -s %TV_IP%:5555 shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n vn.cleanyourtv.dashboard/.MainActivity
"%ADB%" -s %TV_IP%:5555 shell pm disable-user --user 0 com.google.android.tvlauncher
"%ADB%" -s %TV_IP%:5555 shell pm disable-user --user 0 com.google.android.tvrecommendations
"%ADB%" -s %TV_IP%:5555 shell pm disable-user --user 0 com.gathin.tvhome
"%ADB%" -s %TV_IP%:5555 shell cmd package set-home-activity --user 0 vn.cleanyourtv.dashboard/.MainActivity
"%ADB%" -s %TV_IP%:5555 shell input keyevent KEYCODE_HOME

echo.
echo [4/4] Dang chup anh man hinh TV de kiem tra...
ping -n 6 127.0.0.1 >nul
"%ADB%" -s %TV_IP%:5555 shell screencap -p /sdcard/vn_home.png
"%ADB%" -s %TV_IP%:5555 pull /sdcard/vn_home.png vn_home_screenshot.png
"%ADB%" -s %TV_IP%:5555 shell rm -f /sdcard/vn_home.png

echo.
echo === HOAN TAT CAI DAT CLEANYOURTV-VN ===
