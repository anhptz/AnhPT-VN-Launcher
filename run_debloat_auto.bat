@echo off
chcp 65001 >nul
cd /d "%~dp0"
set ADB=platform-tools\adb.exe
set TV_IP=192.168.8.15

echo Ket noi toi %TV_IP%:5555...
"%ADB%" connect %TV_IP%:5555

echo.
echo === BAT DAU DEBLOAT AN TOAN CHO MECOOL KM2 ===
for /f "usebackq tokens=* delims=" %%i in ("debloat_km2.txt") do (
    if not "%%i"=="" (
        for /f "tokens=1" %%a in ("%%i") do (
            echo %%a | findstr /b "#" >nul
            if errorlevel 1 (
                echo [Tat] %%a
                "%ADB%" -s %TV_IP%:5555 shell pm disable-user --user 0 %%a
            )
        )
    )
)

echo.
echo === CAC PACKAGE DANG BI TAT (DISABLED) ===
"%ADB%" -s %TV_IP%:5555 shell pm list packages -d

echo.
echo === KIEM TRA MEMORY HIEN TAI ===
"%ADB%" -s %TV_IP%:5555 shell "cat /proc/meminfo | grep -E 'MemTotal|MemFree|MemAvailable'"

echo.
echo === HOAN TAT DEBLOAT ===
