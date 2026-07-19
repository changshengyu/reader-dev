@echo off
REM Docker 镜像本地构建脚本 (Windows)

setlocal enabledelayedexpansion

REM 配置
set DOCKER_USERNAME=youhe417
set REGISTRY=docker.io
set IMAGE_NAME=%DOCKER_USERNAME%/reader

echo ========================================
echo Reader Docker 镜像构建脚本 (Windows)
echo ========================================
echo.

REM 检查 Docker 是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未安装，请先安装 Docker
    echo 访问 https://docs.docker.com/docker-for-windows/install/
    exit /b 1
)

echo [✓] Docker 已安装
docker --version
echo.

REM 步骤 1: 构建前端
echo [步骤 1/4] 构建前端代码...
cd web
call npm install
call npm run build
cd ..
if errorlevel 1 (
    echo [错误] 前端构建失败
    exit /b 1
)
echo [✓] 前端构建完成
echo.

REM 步骤 2: 构建后端
echo [步骤 2/4] 构建后端代码...
if exist src\main\resources\web (
    rmdir /s /q src\main\resources\web
)
move web\dist src\main\resources\web
del /q src\main\java\com\htmake\reader\ReaderUIApplication.kt
call gradlew -b cli.gradle assemble --info
if errorlevel 1 (
    echo [错误] 后端构建失败
    exit /b 1
)
for /r "build\libs" %%F in (*.jar) do (
    move "%%F" reader.jar
    goto build_success
)
:build_success
echo [✓] 后端构建完成
echo.

REM 步骤 3: 登录 Docker Hub
echo [步骤 3/4] 登录 Docker Hub...
set /p DOCKER_PASSWORD=请输入 Docker Hub 访问令牌:
echo !DOCKER_PASSWORD! | docker login -u %DOCKER_USERNAME% --password-stdin
if errorlevel 1 (
    echo [错误] Docker Hub 登录失败
    exit /b 1
)
echo [✓] Docker Hub 登录成功
echo.

REM 步骤 4: 构建并推送镜像
echo [步骤 4/4] 构建并推送镜像...
echo.

REM 构建标准版
echo 正在构建: %IMAGE_NAME%:latest
docker build -f Dockerfile.source -t %IMAGE_NAME%:latest .
if errorlevel 1 (
    echo [错误] 标准版镜像构建失败
    exit /b 1
)
echo 正在推送: %IMAGE_NAME%:latest
docker push %IMAGE_NAME%:latest
echo [✓] %IMAGE_NAME%:latest 完成
echo.

REM 构建 Slim 版
echo 正在构建: %IMAGE_NAME%:slim-latest
docker build -f Dockerfile.slim -t %IMAGE_NAME%:slim-latest .
if errorlevel 1 (
    echo [错误] Slim 版镜像构建失败
    exit /b 1
)
echo 正在推送: %IMAGE_NAME%:slim-latest
docker push %IMAGE_NAME%:slim-latest
echo [✓] %IMAGE_NAME%:slim-latest 完成
echo.

REM 构建 OpenJ9 版
echo 正在构建: %IMAGE_NAME%:openj9-latest
docker build -f Dockerfile.openj9 -t %IMAGE_NAME%:openj9-latest .
if errorlevel 1 (
    echo [错误] OpenJ9 版镜像构建失败
    exit /b 1
)
echo 正在推送: %IMAGE_NAME%:openj9-latest
docker push %IMAGE_NAME%:openj9-latest
echo [✓] %IMAGE_NAME%:openj9-latest 完成
echo.

echo ========================================
echo [✓] 所有镜像构建和推送完成！
echo ========================================
echo.
echo 已推送的镜像:
echo   - %IMAGE_NAME%:latest
echo   - %IMAGE_NAME%:slim-latest
echo   - %IMAGE_NAME%:openj9-latest
echo.
echo 可以使用以下命令拉取镜像:
echo   docker pull %IMAGE_NAME%:latest
echo   docker pull %IMAGE_NAME%:slim-latest
echo   docker pull %IMAGE_NAME%:openj9-latest
echo.

endlocal
