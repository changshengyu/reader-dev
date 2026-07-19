#!/bin/bash
# Docker 镜像本地构建脚本

set -e

# 配置
DOCKER_USERNAME="${DOCKER_USERNAME:-youhe417}"
REGISTRY="${REGISTRY:-docker.io}"
IMAGE_NAME="${IMAGE_NAME:-$DOCKER_USERNAME/reader}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Reader Docker 镜像构建脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker 已安装${NC}"
docker --version
echo ""

# 步骤 1: 构建前端
echo -e "${YELLOW}[步骤 1/4] 构建前端代码...${NC}"
cd web
npm install
npm run build
cd ..
echo -e "${GREEN}✓ 前端构建完成${NC}"
echo ""

# 步骤 2: 构建后端
echo -e "${YELLOW}[步骤 2/4] 构建后端代码...${NC}"
if [ -d "src/main/resources/web" ]; then
    rm -rf src/main/resources/web
fi
mv ./web/dist ./src/main/resources/web
rm -f src/main/java/com/htmake/reader/ReaderUIApplication.kt
gradle -b cli.gradle assemble --info
if [ -f "./build/libs/reader.jar" ]; then
    mv ./build/libs/reader.jar ./reader.jar
else
    # 尝试找到构建好的 jar 文件
    find ./build/libs -name "*.jar" -exec mv {} ./reader.jar \;
fi
echo -e "${GREEN}✓ 后端构建完成${NC}"
echo ""

# 步骤 3: 登录 Docker Hub
echo -e "${YELLOW}[步骤 3/4] 登录 Docker Hub...${NC}"
if [ -z "$DOCKER_PASSWORD" ]; then
    echo "请输入 Docker Hub 密码或访问令牌:"
    read -s DOCKER_PASSWORD
fi

echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin
echo -e "${GREEN}✓ Docker Hub 登录成功${NC}"
echo ""

# 步骤 4: 构建并推送镜像
echo -e "${YELLOW}[步骤 4/4] 构建并推送镜像...${NC}"
echo ""

# 定义要构建的镜像
declare -a IMAGES=(
    "Dockerfile.source:latest"
    "Dockerfile.slim:slim-latest"
    "Dockerfile.openj9:openj9-latest"
)

for IMAGE_SPEC in "${IMAGES[@]}"; do
    IFS=':' read -r DOCKERFILE TAG <<< "$IMAGE_SPEC"
    FULL_IMAGE_NAME="$IMAGE_NAME:$TAG"
    
    echo -e "${YELLOW}正在构建: $FULL_IMAGE_NAME${NC}"
    docker build -f $DOCKERFILE -t $FULL_IMAGE_NAME .
    
    echo -e "${YELLOW}正在推送: $FULL_IMAGE_NAME${NC}"
    docker push $FULL_IMAGE_NAME
    
    echo -e "${GREEN}✓ $FULL_IMAGE_NAME 完成${NC}"
    echo ""
done

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ 所有镜像构建和推送完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "已推送的镜像:"
echo "  - $IMAGE_NAME:latest"
echo "  - $IMAGE_NAME:slim-latest"
echo "  - $IMAGE_NAME:openj9-latest"
echo ""
echo "可以使用以下命令拉取镜像:"
echo "  docker pull $IMAGE_NAME:latest"
echo "  docker pull $IMAGE_NAME:slim-latest"
echo "  docker pull $IMAGE_NAME:openj9-latest"
echo ""
