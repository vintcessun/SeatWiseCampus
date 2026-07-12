#!/bin/bash
# deploy.sh — 从远程拉取最新代码，重新构建 Docker 并启动
set -e

echo "=== 1/5 切换至 main 分支 ==="
git checkout main

echo "=== 2/5 拉取远程最新代码（upstream/main） ==="
git fetch upstream
git merge upstream/main

echo "=== 3/5 停掉旧容器 ==="
docker compose down --remove-orphans 2>/dev/null || true

echo "=== 4/5 重新构建镜像（无缓存） ==="
docker compose build --no-cache

echo "=== 5/5 启动容器 ==="
docker compose up -d

echo "=== 部署完成 ==="
docker compose ps
