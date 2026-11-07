@echo off
chcp 65001 >nul
title Xem trang thai package - Mecool KM2
color 0B

echo ╔══════════════════════════════════════════════════════════╗
echo ║    KIEM TRA TRANG THAI PACKAGE - Mecool KM2             ║
echo ║    Xem package nao dang bat/tat tren thiet bi           ║
echo ╚══════════════════════════════════════════════════════════╝
echo.

set ADB=%~dp0platform-tools\adb.exe
if not exist "%ADB%" (
    echo [LOI] Khong tim thay ADB tai: %ADB%
    pause
    exit /b 1
)

set /p TV_IP="Nhap dia chi IP cua Mecool KM2 (vd: 192.168.1.100): "

if "%TV_IP%"=="" (
    echo [LOI] Ban chua nhap dia chi IP!
    pause
    exit /b 1
)

echo.
echo Dang ket noi voi %TV_IP%:5555 ...
"%ADB%" connect %TV_IP%:5555
timeout /t 3 >nul

echo.
echo ====================================================
echo  DANH SACH TAT CA PACKAGE:
echo ====================================================
"%ADB%" shell pm list packages -e
echo.
echo (Chi hien thi package DANG BAT - enabled)
echo.

echo ====================================================
echo  PACKAGE DA BI TAT (disabled):
echo ====================================================
"%ADB%" shell pm list packages -d
echo.

echo ====================================================
echo  THONG TIN BO NHO:
echo ====================================================
"%ADB%" shell df -h /data
echo.

echo ====================================================
echo  THONG TIN RAM:
echo ====================================================
"%ADB%" shell cat /proc/meminfo | findstr "MemTotal MemFree MemAvailable"
echo.

pause
