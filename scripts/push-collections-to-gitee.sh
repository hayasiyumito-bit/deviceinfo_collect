#!/usr/bin/env bash
# 将本仓库中的 collections/ 目录单独推送到 Gitee（远端仓库根目录 = collections 内文件）。
# 使用 git subtree split，无需在 collections 下嵌套第二个 .git。
#
# 用法（在仓库根目录执行）：
#   ./scripts/push-collections-to-gitee.sh
# 可选：指定远程名（默认 gitee-device-collection）
#   ./scripts/push-collections-to-gitee.sh my-remote
#
# 环境变量：
#   GITEE_COLLECTION_URL  覆盖默认 Gitee 地址
#
# 前置条件：
#   1) 已在 Gitee 创建空仓库 https://gitee.com/Yumito/device-collection.git
#   2) 已配置好对该 URL 的认证（SSH 或 https 凭据）
#   3) 若远端已有提交且历史不一致，可能需加 --force（请自行改脚本最后一行，慎用）

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

REMOTE_NAME="${1:-gitee-device-collection}"
URL="${GITEE_COLLECTION_URL:-https://gitee.com/Yumito/device-collection.git}"

if [[ ! -d collections ]]; then
  echo "error: 未找到 collections/ 目录（请在仓库根执行）" >&2
  exit 1
fi

if ! git remote get-url "$REMOTE_NAME" &>/dev/null; then
  echo "添加远程: $REMOTE_NAME -> $URL"
  git remote add "$REMOTE_NAME" "$URL"
else
  echo "远程已存在: $REMOTE_NAME ($(git remote get-url "$REMOTE_NAME"))"
fi

TMP_BRANCH="tmp/collections-only-$(date +%s)"
echo "生成子树分支（仅含 collections/ 内容，可能较慢）…"
git subtree split -P collections -b "$TMP_BRANCH"

echo "推送到 Gitee main（如需改分支名请编辑脚本）…"
git push "$REMOTE_NAME" "$TMP_BRANCH:main"

echo "删除本地临时分支 $TMP_BRANCH"
git branch -D "$TMP_BRANCH"

echo "完成。Gitee 仓库根目录即原 collections/ 下文件。"
