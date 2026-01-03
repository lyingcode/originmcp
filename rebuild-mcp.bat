@echo off
echo ========================================
echo MCP Server 重新构建脚本
echo ========================================
echo.

echo [1/4] 检查 JAR 文件状态...
if exist target\originmcp-0.0.1-SNAPSHOT.jar (
    echo 发现旧的 JAR 文件
    echo.
    echo [2/4] 尝试删除旧的 JAR...

    REM 等待文件解锁
    timeout /t 2 /nobreak >nul

    del /F /Q target\originmcp-0.0.1-SNAPSHOT.jar 2>nul
    if exist target\originmcp-0.0.1-SNAPSHOT.jar (
        echo ⚠️  无法删除 JAR 文件（文件被锁定）
        echo.
        echo 📌 请手动操作：
        echo    1. 完全关闭 Cursor
        echo    2. 重新运行此脚本
        echo    3. 重新打开 Cursor
        pause
        exit /b 1
    ) else (
        echo ✅ 旧 JAR 已删除
    )
) else (
    echo ✅ 没有旧的 JAR 文件
)

echo.
echo [3/4] 清理并重新构建...
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 构建失败！
    pause
    exit /b 1
)

echo.
echo [4/4] 验证 JAR 文件...
if exist target\originmcp-0.0.1-SNAPSHOT.jar (
    echo ✅ JAR 文件创建成功！
    echo.
    echo 📍 文件位置: %CD%\target\originmcp-0.0.1-SNAPSHOT.jar
    echo.
    echo 🎉 构建完成！
    echo.
    echo 📋 下一步：
    echo    1. 在 Cursor 中重启 MCP Server
    echo    2. 或者完全重启 Cursor
    echo    3. 验证工具已加载 (应该看到 2 个工具)
) else (
    echo ❌ JAR 文件创建失败！
)

echo.
echo ========================================
pause
