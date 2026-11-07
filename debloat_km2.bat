@echo off
chcp 65001 >nul
title Toi uu Mecool KM2 - CleanYourTV
color 0A

echo ╔══════════════════════════════════════════════════════════════════════════╗
echo ║              CLEAN YOUR TV - TOI UU MECOOL KM2                           ║
echo ║         Dua tren nguyen ly: https://github.com/Gathin23/cleanyourtv      ║
echo ╠══════════════════════════════════════════════════════════════════════════╣
echo ║  - An toan: Chi DUNG LENH 'pm disable-user' de tat, khong xoa he thong   ║
echo ║  - Giu nguyen: Remote Bluetooth, Voice Search (Mic), Driver Amlogic 4K   ║
echo ║  - Phuc hoi: Chay restore_km2.bat bat ky luc nao de bat lai              ║
echo ╚══════════════════════════════════════════════════════════════════════════╝
echo.

set ADB=%~dp0platform-tools\adb.exe
if not exist "%ADB%" (
    echo [LOI] Khong tim thay ADB tai: %ADB%
    pause
    exit /b 1
)

set TV_IP=192.168.8.15
set /p USER_IP="Nhap dia chi IP cua Mecool KM2 (Nhan Enter de dung [%TV_IP%]): "
if not "%USER_IP%"=="" set TV_IP=%USER_IP%

echo.
echo Dang ket noi voi %TV_IP%:5555 ...
"%ADB%" connect %TV_IP%:5555
timeout /t 2 >nul

"%ADB%" devices | findstr "%TV_IP%" | findstr "unauthorized" >nul
if not errorlevel 1 (
    echo.
    echo ======================================================================
    echo [CHUYEN Y] XAC THUC RSA TREN TV:
    echo Tren man hinh TV dang hien hop thoai "Cho phep go loi USB?".
    echo Hay dung remote tich vao "Luon cho phep tu may tinh nay" roi bam [OK]!
    echo ======================================================================
    echo.
    echo Nhan phim bat ky sau khi da bam OK tren TV...
    pause >nul
)

"%ADB%" devices | findstr "%TV_IP%" | findstr "device" >nul
if errorlevel 1 (
    echo.
    echo [LOI] Chua the xac thuc hoac khong ket noi duoc voi %TV_IP%:5555!
    echo Vui long kiem tra:
    echo  1. TV va may tinh cung mang Wi-Fi.
    echo  2. Da bat USB Debugging & Network Debugging trong Developer Options.
    echo  3. Da chap nhan hop thoai gỡ lỗi trên TV.
    echo.
    pause
    exit /b 1
)

echo.
echo [OK] Ket noi thanh cong va da duoc cap quyen ADB!
echo.

echo ====================================================
echo  Sao luu danh sach package truoc khi toi uu...
echo ====================================================
"%ADB%" shell pm list packages > "%~dp0backup_packages_%TV_IP%.txt"
echo [OK] Danh sach da duoc luu vao backup_packages_%TV_IP%.txt
echo.

echo ====================================================
echo  Bat dau tat cac tien trinh bloatware & chay ngam
echo ====================================================
echo.
echo Nhan phim bat ky de bat dau, hoac dong cua so de huy...
pause >nul

set COUNT_OK=0
set COUNT_SKIP=0

echo.
echo Dang xu ly cac tien trinh trong debloat_km2.txt...
echo ----------------------------------------------------

for /f "usebackq tokens=* delims=" %%i in ("%~dp0debloat_km2.txt") do (
    if not "%%i"=="" (
        for /f "tokens=1" %%a in ("%%i") do (
            echo %%a | findstr /b "#" >nul
            if errorlevel 1 (
                echo  [-] Dang tat: %%a
                "%ADB%" shell pm disable-user --user 0 %%a >nul 2>&1
                if errorlevel 1 (
                    echo      ^> [SKIP] Khong ton tai hoac da bi tat
                    set /a COUNT_SKIP+=1
                ) else (
                    echo      ^> [OK] Da tat thanh cong
                    set /a COUNT_OK+=1
                )
            )
        )
    )
)

if exist "%~dp0debloat_optional_apps.txt" (
    echo.
    echo Dang kiem tra debloat_optional_apps.txt...
    for /f "usebackq tokens=* delims=" %%i in ("%~dp0debloat_optional_apps.txt") do (
        if not "%%i"=="" (
            for /f "tokens=1" %%a in ("%%i") do (
                echo %%a | findstr /b "#" >nul
                if errorlevel 1 (
                    echo  [-] Dang tat (tuy chon): %%a
                    "%ADB%" shell pm disable-user --user 0 %%a >nul 2>&1
                    if errorlevel 1 (
                        echo      ^> [SKIP] Khong ton tai hoac da bi tat
                        set /a COUNT_SKIP+=1
                    ) else (
                        echo      ^> [OK] Da tat thanh cong
                        set /a COUNT_OK+=1
                    )
                )
            )
        )
    )
)

echo.
echo ====================================================
echo  KET QUA:
echo ====================================================
echo   Da tat thanh cong : %COUNT_OK% package(s)
echo   Bo qua            : %COUNT_SKIP% package(s)
echo ====================================================
echo.

echo Khoi dong lai TV de ap dung thay doi va giai phong RAM? (Y/N):
set /p REBOOT_CHOICE="Lua chon (Mac dinh Y): "
if /i not "%REBOOT_CHOICE%"=="N" (
    echo Dang khoi dong lai Mecool KM2...
    "%ADB%" shell reboot
    echo TV dang khoi dong lai. Hoan tat!
)
echo.
pause
