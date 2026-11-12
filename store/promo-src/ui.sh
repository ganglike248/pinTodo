# 에뮬레이터 UI 조작 도우미: source ui.sh 후 tap 'text="X"'
dump(){ adb -e shell "uiautomator dump /sdcard/u.xml >/dev/null; cat /sdcard/u.xml"; }
center(){ dump | grep -o "$1[^>]*bounds=\"[^\"]*\"" | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1 | sed -E 's/\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]/\1 \2 \3 \4/' | awk '{print int(($1+$3)/2), int(($2+$4)/2)}'; }
tap(){ c=$(center "$1"); if [ -n "$c" ]; then adb -e shell input tap $c; echo "tap $1 @ $c"; else echo "MISSING $1"; fi; }
texts(){ dump | grep -o '\(text\|content-desc\)="[^"]\+"' | sort -u | head -${1:-60}; }
# 녹화: rec 이름 최대초 → (조작) → stoprec; pull 이름
rec(){ adb -e shell rm -f /sdcard/$1.mp4; (adb -e shell screenrecord --bit-rate 12000000 --time-limit $2 /sdcard/$1.mp4 >/dev/null 2>&1 &); sleep 1.2; }
stoprec(){ adb -e shell pkill -INT screenrecord; }
pull(){ while adb -e shell pidof screenrecord >/dev/null 2>&1; do sleep 0.5; done; sleep 0.5; mkdir -p $W/rec; adb -e pull /sdcard/$1.mp4 $W/rec/$1.mp4 >/dev/null && ffprobe -v error -show_entries format=duration -of csv=p=0 $W/rec/$1.mp4; }
# 예시 데이터 넣기: seed [noti]
seed(){ python3 $P/seed.py $W/seed $1 && adb -e shell am force-stop com.pintodo && adb -e push $W/seed/todos.xml $W/seed/ui.xml /data/local/tmp/ >/dev/null && for f in todos ui; do adb -e shell "cat /data/local/tmp/$f.xml | run-as com.pintodo sh -c 'cat > shared_prefs/$f.xml'"; done; }
