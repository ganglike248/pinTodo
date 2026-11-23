# 스크린샷 도우미: source store/shots-src/ui.sh
P=$(cd "$(dirname "${(%):-%x}")" && pwd); W=$P/work; mkdir -p $W/raw
dump(){ adb -e shell "uiautomator dump /sdcard/u.xml >/dev/null; cat /sdcard/u.xml"; }
center(){ dump | grep -o "$1[^>]*bounds=\"[^\"]*\"" | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1 | sed -E 's/\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]/\1 \2 \3 \4/' | awk '{print int(($1+$3)/2), int(($2+$4)/2)}'; }
tap(){ c=$(center "$1"); if [ -n "$c" ]; then adb -e shell input tap ${=c}; echo "tap $1 @ $c"; else echo "MISSING $1"; fi; }
texts(){ dump | grep -o '\(text\|content-desc\)="[^"]\+"' | sort -u | head -${1:-80}; }
shot(){ adb -e exec-out screencap -p > $W/raw/$1.png; echo "shot $1"; }
seed(){ python3 $P/seed.py $W/seed ${1:-list} && adb -e shell am force-stop com.pintodo && adb -e push $W/seed/todos.xml $W/seed/ui.xml /data/local/tmp/ >/dev/null && for f in todos ui; do adb -e shell "cat /data/local/tmp/$f.xml | run-as com.pintodo sh -c 'cat > shared_prefs/$f.xml'"; done; }
SHADE(){ adb -e shell cmd statusbar expand-notifications; sleep 1.5; }
CLOSE(){ adb -e shell cmd statusbar collapse; sleep 0.6; }
APP(){ CLOSE; seed ${1:-list}; adb -e shell am start -n com.pintodo/.ui.MainActivity >/dev/null; sleep 4; adb -e shell am start -n com.pintodo/.ui.MainActivity >/dev/null 2>&1; sleep 3; }
demo(){
  adb -e shell settings put global sysui_demo_allowed 1
  b(){ adb -e shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
  b enter; b clock -e hhmm 0941; b battery -e level 100 -e plugged false
  b network -e wifi show -e level 4 -e fully true; b network -e mobile hide
  b notifications -e visible false
}
clock(){ adb -e shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm $(date +%H%M) >/dev/null; sleep 0.3; }
