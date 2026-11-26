#!/usr/bin/env bash
# post.html의 게시물 → store/insta/이름/1.png, 2.png… (1080×1350). 사용: bash store/insta-src/render.sh [이름] [장수]  (기본 01-intro 8)
# Chrome/Edge/Playwright Chromium 중 있는 것을 헤드리스로 사용. 다른 경로면 CHROME=… 로 지정
set -e
cd "$(dirname "$0")"
for c in "$CHROME" "$(ls "$LOCALAPPDATA"/ms-playwright/chromium-*/chrome-win*/chrome.exe 2>/dev/null | tail -1)" \
  "/c/Program Files/Google/Chrome/Application/chrome.exe" "/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"; do
  [ -n "$c" ] && [ -x "$c" ] && BROWSER="$c" && break
done
[ -z "$BROWSER" ] && { echo "Chrome을 찾지 못했어요. CHROME=경로 로 지정하세요"; exit 1; }
ID="${1:-01-intro}"; COUNT="${2:-8}"
SRC="$(pwd -W 2>/dev/null || pwd)"
mkdir -p "../insta/$ID"
for n in $(seq 1 "$COUNT"); do
  "$BROWSER" --headless --disable-gpu --hide-scrollbars --allow-file-access-from-files --force-device-scale-factor=1 \
    --window-size=1080,1350 --virtual-time-budget=4000 --screenshot="$SRC/../insta/$ID/$n.png" "file:///$SRC/post.html?id=$ID&n=$n" 2>/dev/null
  echo "store/insta/$ID/$n.png"
done
