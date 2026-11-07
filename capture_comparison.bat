@echo off
chcp 65001 >nul
cd /d "%~dp0"
set ADB=platform-tools\adb.exe
set TV_IP=192.168.8.31

echo ========================================================
echo   CHUP ANH SO SANH: GOOGLE TV GOC vs CLEANYOURTV-VN
echo ========================================================
echo.
echo [1/4] Dang ket noi ADB toi %TV_IP%:5555...
ping -n 2 -w 1000 %TV_IP% >nul
"%ADB%" connect %TV_IP%:5555

echo.
echo [2/4] Dang mo giao dien Google TV goc de chup man hinh...
"%ADB%" -s %TV_IP%:5555 shell input keyevent KEYCODE_WAKEUP
"%ADB%" -s %TV_IP%:5555 shell pm enable com.google.android.tvlauncher
"%ADB%" -s %TV_IP%:5555 shell pm enable com.google.android.tvrecommendations
"%ADB%" -s %TV_IP%:5555 shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
"%ADB%" -s %TV_IP%:5555 shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n com.google.android.tvlauncher/.MainActivity
ping -n 6 127.0.0.1 >nul
"%ADB%" -s %TV_IP%:5555 shell screencap -p /sdcard/google_tv_original.png
"%ADB%" -s %TV_IP%:5555 pull /sdcard/google_tv_original.png google_tv_original.png
"%ADB%" -s %TV_IP%:5555 shell rm -f /sdcard/google_tv_original.png

echo.
echo [3/4] Dang chuyen lai ve giao dien CleanYourTV-VN va chup man hinh...
"%ADB%" -s %TV_IP%:5555 shell pm disable-user --user 0 com.google.android.tvlauncher
"%ADB%" -s %TV_IP%:5555 shell pm disable-user --user 0 com.google.android.tvrecommendations
"%ADB%" -s %TV_IP%:5555 shell cmd package set-home-activity --user 0 vn.cleanyourtv.dashboard/.MainActivity
"%ADB%" -s %TV_IP%:5555 shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n vn.cleanyourtv.dashboard/.MainActivity
"%ADB%" -s %TV_IP%:5555 shell input keyevent KEYCODE_HOME
ping -n 5 127.0.0.1 >nul
"%ADB%" -s %TV_IP%:5555 shell screencap -p /sdcard/cleanyourtv_vn_modified.png
"%ADB%" -s %TV_IP%:5555 pull /sdcard/cleanyourtv_vn_modified.png cleanyourtv_vn_modified.png
"%ADB%" -s %TV_IP%:5555 shell rm -f /sdcard/cleanyourtv_vn_modified.png

echo.
echo [4/4] Hoan tat chup 2 man hinh!
