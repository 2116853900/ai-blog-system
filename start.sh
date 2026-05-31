#!/usr/bin/env bash
# ============================================================
#  AI 信息站一键启动脚本 (bash / Git-Bash / WSL / macOS / Linux)
#  - 后端: Spring Boot (端口 8080)
#  - 前端: Vite (端口 5173)
#  在同一终端启动两者，Ctrl-C 一并停止。
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo
echo ">_ AI 信息站 — 正在启动…"
echo "------------------------------------------------------------"
echo "  后端 http://localhost:8080   前端 http://localhost:5173"
echo "------------------------------------------------------------"
echo

# ---- 环境检查 ----
need() { command -v "$1" >/dev/null 2>&1 || { echo "[错误] 未找到 $1，请先安装并配置 PATH"; exit 1; }; }
need java
need mvn
need node

# ---- 前端依赖（首次自动安装）----
if [ ! -d "frontend/node_modules" ]; then
  echo "[前端] 首次运行，正在安装依赖 npm install …"
  ( cd frontend && npm install )
fi

PIDS=()
cleanup() {
  echo
  echo "正在停止服务…"
  for pid in "${PIDS[@]:-}"; do
    [ -n "${pid:-}" ] && kill "$pid" 2>/dev/null || true
  done
  # 等待子进程退出，避免孤儿进程
  wait 2>/dev/null || true
  echo "已停止。"
}
trap cleanup INT TERM EXIT

# ---- 启动后端 ----
echo "[后端] 启动 Spring Boot …"
( cd backend && mvn -q spring-boot:run ) &
PIDS+=($!)

# ---- 启动前端 ----
echo "[前端] 启动 Vite 开发服务器 …"
( cd frontend && npm run dev ) &
PIDS+=($!)

echo
echo "两个服务正在启动中。后端首次启动会自动建表并写入示例数据（约 20-40 秒）。"
echo "完成后访问: http://localhost:5173   默认管理员: admin / admin123"
echo "按 Ctrl-C 停止全部服务。"
echo

# 任一进程退出即结束（并触发 cleanup 停止另一个）
wait -n
