@echo off
chcp 65001 >nul
cd /d "%~dp0"

REM 确保先编译
if not exist "out\clock\AppMain.class" (
    echo [提示] 未检测到编译产物，正在自动编译...
    call 编译.bat
    if %errorlevel% neq 0 exit /b 1
)

REM 从项目根目录运行（classpath 指向 out）
echo 启动小樱花时钟...
start javaw -client -Xms32m -Xmx128m -cp out clock.AppMain
echo 已启动！时钟窗口在屏幕右侧。
