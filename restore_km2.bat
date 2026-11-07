@echo off
chcp 65001 >nul
title Khoi phuc Mecool KM2 - Undo Debloat
color 0E

echo ╔══════════════════════════════════════════════════════════╗
echo ║      KHOI PHUC Mecool KM2 - Undo Debloat               ║
echo ║  Bat lai tat ca package da bi tat tren thiet bi          ║
echo ╚══════════════════════════════════════════════════════════╝
echo.

set ADB=%~dp0platform-tools\adb.exe
if not exist "%ADB%" (
    echo [LOI] Khong tim thay ADB tai: %ADB%
    pause
    exit /b 1
)

set TV_IP=192.168.8.15
set /p USER_IP="Nhap dia chi IP cua Mecool KM2 (Mac dinh [%TV_IP%]): "
if not "%USER_IP%"=="" set TV_IP=%USER_IP%

echo.
echo Dang ket noi voi %TV_IP%:5555 ...
"%ADB%" connect %TV_IP%:5555
timeout /t 2 >nul

"%ADB%" devices | findstr "%TV_IP%" >nul
if errorlevel 1 (
    echo [LOI] Khong ket noi duoc voi Mecool KM2!
    echo Hay kiem tra lai IP va chap nhan xac thuc RSA tren TV.
    pause
    exit /b 1
)

echo.
echo [OK] Da ket noi thanh cong!
echo Dang bat lai tat ca package da tat...
echo -----------------------------------------------

set COUNT_OK=0

REM Khoi phuc tu file debloat_km2.txt
if exist "%~dp0debloat_km2.txt" (
    for /f "usebackq tokens=* delims=" %%i in ("%~dp0debloat_km2.txt") do (
        if not "%%i"=="" (
            echo %%i | findstr /b "#" >nul
            if errorlevel 1 (
                echo  Dang bat lai: %%i
                "%ADB%" shell pm enable %%i >nul 2>&1
                if not errorlevel 1 set /a COUNT_OK+=1
            )
        )
    )
)

REM Khoi phuc tu file debloat_optional_apps.txt neu co
if exist "%~dp0debloat_optional_apps.txt" (
    for /f "usebackq tokens=* delims=" %%i in ("%~dp0debloat_optional_apps.txt") do (
        if not "%%i"=="" (
            for /f "tokens=1" %%a in ("%%i") do (
                echo %%a | findstr /b "#" >nul
                if errorlevel 1 (
                    echo  Dang bat lai: %%a
                    "%ADB%" shell pm enable %%a >nul 2>&1
                    if not errorlevel 1 set /a COUNT_OK+=1
                )
            )
        )
    )
)

echo.
echo Dang bat lai Google TV Launcher mac dinh...
"%ADB%" shell pm enable com.google.android.tvlauncher >nul 2>&1
"%ADB%" shell pm enable com.google.android.tvrecommendations >nul 2>&1
"%ADB%" shell pm enable com.spocky.projengmenu >nul 2>&1
"%ADB%" shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity >nul 2>&1

echo.
echo -----------------------------------------------
echo ====================================================
echo  HOAN TAT! Da bat lai %COUNT_OK% package(s)
echo ====================================================
echo.
echo Khoi dong lai TV de ap dung? (Y/N):
set /p REBOOT_CHOICE="Lua chon (Mac dinh Y): "
if /i not "%REBOOT_CHOICE%"=="N" (
    echo Dang khoi dong lai Mecool KM2...
    "%ADB%" shell reboot
)
echo.
pause
