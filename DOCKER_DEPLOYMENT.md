# Docker Hub 部署指南

本文档说明如何使用 GitHub Actions 自动构建和推送 Docker 镜像到 Docker Hub。

## 前置条件

1. 确保你已 Fork 了此仓库到你的 GitHub 账户
2. 拥有 Docker Hub 账户和访问令牌

## 设置步骤

### 1. 获取 Docker Hub 访问令牌

1. 访问 [Docker Hub 账户设置](https://hub.docker.com/settings/personal-access-tokens)
2. 点击"Generate New Token"
3. 创建一个 token（用于 CI/CD）
4. 复制 token（稍后需要用到）

### 2. 配置 GitHub Actions Secrets

在你的 GitHub 仓库设置中添加以下 secrets：

1. 访问你的仓库 Settings > Secrets and variables > Actions
2. 点击 "New repository secret"
3. 添加以下两个 secrets：
   - **DOCKER_USERNAME**: 你的 Docker Hub 用户名（例如：youhe417）
   - **DOCKER_PASSWORD**: 你的 Docker Hub 访问令牌

### 3. 验证工作流文件

工作流文件位于 `.github/workflows/docker-push.yml`，包含以下功能：

- ✅ 自动构建三种镜像版本（标准版、Slim 版、OpenJ9 版）
- ✅ 支持多平台构建（amd64、arm64、arm/v7）
- ✅ 在推送到 master/main 分支时自动触发
- ✅ 支持 tag 发布时自动构建
- ✅ 支持手动触发（workflow_dispatch）

## 使用方法

### 自动触发

工作流会在以下情况自动触发：

1. **推送到 master 或 main 分支** - 构建并推送镜像，标签为 `latest`, `slim-latest`, `openj9-latest`
2. **推送 tag（格式 v*）** - 例如推送 `v1.0.0` 会构建相应版本的镜像
3. **手动触发** - 访问 Actions 标签页，点击 "Build and Push Docker Images" 工作流，然后点击 "Run workflow"

### 构建命令示例

如果你想在本地构建（需要安装 Docker）：

```bash
# 构建标准版
docker build -f Dockerfile.source -t youhe417/reader:latest .

# 构建 Slim 版
docker build -f Dockerfile.slim -t youhe417/reader:slim-latest .

# 构建 OpenJ9 版
docker build -f Dockerfile.openj9 -t youhe417/reader:openj9-latest .

# 推送到 Docker Hub
docker login
docker push youhe417/reader:latest
docker push youhe417/reader:slim-latest
docker push youhe417/reader:openj9-latest
```

## 工作流状态检查

1. 访问你的仓库的 Actions 标签页
2. 查看最新的工作流运行状态
3. 点击具体的工作流查看详细日志

## 常见问题

**Q: 为什么工作流没有运行？**
- 检查 Secrets 是否正确配置
- 确认推送的分支是 master 或 main
- 检查工作流文件语法是否正确

**Q: 如何修改镜像名称或标签？**
- 编辑 `.github/workflows/docker-push.yml` 文件
- 修改 `IMAGE_NAME` 变量（第 8 行）为你的镜像名称

**Q: 如何只构建某一个版本？**
- 编辑 `.github/workflows/docker-push.yml` 的 `matrix.include` 部分
- 注释掉不需要的构建配置

## 镜像信息

### 三种版本说明

| 版本 | Dockerfile | 说明 | 适用场景 |
|------|-----------|------|---------|
| latest | Dockerfile.source | 标准版，包含所有功能 | 推荐，功能完整 |
| slim-latest | Dockerfile.slim | 轻量版，精简功能 | 存储空间有限 |
| openj9-latest | Dockerfile.openj9 | OpenJ9 JVM，启动快省内存 | 需要快速启动或低内存环境 |

### 标签格式

- `latest`/`slim-latest`/`openj9-latest` - master 分支最新版
- `v1.0.0`, `v1.0`, `v1` - 对应版本 tag

## 相关资源

- [Docker Hub](https://hub.docker.com)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Docker 官方文档](https://docs.docker.com)
