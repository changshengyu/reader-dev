# Reader - 阅读3服务器版

一个功能丰富的网页阅读应用，基于阅读3.0开发的服务器版本，无需手机即可在浏览器中享受阅读体验。

> 🍴 Fork 自 [hectorqin/reader](https://github.com/hectorqin/reader)，已合并社区 PR（#648 #653 #667 #668 #701）并持续维护。

## ✨ 主要功能

- 📚 **书源管理** - 支持自定义书源，灵活获取书籍内容
- 🏠 **书架管理** - 个性化书架，管理你的阅读库
- 🔍 **强大搜索** - 快速搜索和并发搜书
- 📖 **阅读体验** - 自定义主题、翻页方式、手势支持
- 📱 **移动端适配** - 完美支持各种设备屏幕
- 🔄 **多种格式支持** - TXT、EPUB、UMD、PDF 本地书导入
- 📡 **WebDAV 同步** - 跨设备同步阅读进度
- 🎙️ **多媒体支持** - 听书、漫画、音频
- 📚 **高级功能** - 书籍分组、RSS 订阅、定时更新、文字替换过滤、本地书仓、Kindle 阅读

## 🐳 Docker 快速开始

### 拉取镜像

```bash
# 标准版（推荐）
docker pull youhe417/reader:latest

# Slim 版（轻量级）
docker pull youhe417/reader:slim-latest

# OpenJ9 版（启动快、省内存）
docker pull youhe417/reader:openj9-latest
```

### 运行容器

```bash
docker run -d \
  --name reader \
  -p 4396:8080 \
  -v /home/reader/logs:/logs \
  -v /home/reader/storage:/storage \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e READER_APP_SECURE=true \
  -e READER_APP_SECUREKEY=adminpwd \
  -e READER_APP_INVITECODE=registercode \
  youhe417/reader:latest
```

### 使用 Docker Compose（推荐）

```bash
wget https://raw.githubusercontent.com/youhe417/reader-dev/master/docker-compose.yaml
docker-compose up -d
```

## 🛠️ 自行构建

### 构建镜像

```bash
# 标准版
docker build -f Dockerfile.source -t youhe417/reader:latest .

# Slim 版
docker build -f Dockerfile.slim -t youhe417/reader:slim-latest .

# OpenJ9 版
docker build -f Dockerfile.openj9 -t youhe417/reader:openj9-latest .
```

### 推送到 Docker Hub

```bash
docker login
docker push youhe417/reader:latest
docker push youhe417/reader:slim-latest
docker push youhe417/reader:openj9-latest
```

## 📋 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 应用配置 | `prod` |
| `READER_APP_SECURE` | 是否启用安全模式 | `true` |
| `READER_APP_SECUREKEY` | 管理员密码 | `adminpwd` |
| `READER_APP_INVITECODE` | 注册邀请码 | `registercode` |
| `TZ` | 时区设置 | `Asia/Shanghai` |

## 🙏 致谢

- [hectorqin/reader](https://github.com/hectorqin/reader) - 原始项目
- [阅读3.0](https://github.com/gedoor/legado) - 灵感来源

## 📝 许可证

见 [LICENSE](LICENSE) 文件
