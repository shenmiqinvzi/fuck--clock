@echo off
chcp 65001 >nul
echo ========================================
echo   小樱花时钟 — 编译
echo ========================================
cd /d "%~dp0"

REM 创建输出目录
if not exist "out" mkdir out

REM 编译所有源文件
echo 正在编译...
javac -encoding UTF-8 -d out clock\*.java
if %errorlevel% neq 0 (
    echo [失败] 编译出错，请检查上方错误信息。
    pause
    exit /b 1
)

echo [成功] 编译完成！class 文件输出到 out\ 目录。
echo 双击 运行.bat 启动程序。
pause
