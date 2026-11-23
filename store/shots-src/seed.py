# 스토어 스크린샷용 예시 할 일 → todos.xml / ui.xml (디버그 빌드에 run-as로 넣음)
# python3 seed.py 출력폴더 [list|one|noti]
import json, datetime, html, sys, os
from zoneinfo import ZoneInfo
KST = ZoneInfo("Asia/Seoul"); now = datetime.datetime.now(KST)
ms = lambda d: int(d.timestamp() * 1000)
today = now.replace(hour=0, minute=0, second=0, microsecond=0)
ed = lambda d: (d.date() - datetime.date(1970, 1, 1)).days
anchor = ed(today) - 20
daily = list(range(1, 8))
friday = today + datetime.timedelta(days=(4 - today.weekday()) % 7 or 7)

def T(id, title, mins_ago, **k):
    t = dict(id=id, title=title, memo="", notify=True, pinned=True, alertMode="SILENT", repeatDays=[], repeatType="NONE",
             repeatInterval=1, monthDay=1, dailyStart=540, createdAt=ms(now) - mins_ago * 60_000)
    t.update(k)
    t.setdefault("alertedKey", t["createdAt"])  # 이미 울린 것으로 (스크린샷 찍을 때 소리 안 나게)
    return t

shop = [{"t": "우유", "d": True}, {"t": "계란", "d": True}, {"t": "두부", "d": False}, {"t": "대파", "d": False}, {"t": "키친타월", "d": False}]
returns = T(1, "택배 반품 신청", 10, color="BLUE", memo="수거 신청까지 해야 끝", endAt=ms(today.replace(hour=23, minute=59)))
mode = sys.argv[2] if len(sys.argv) > 2 else "list"
if mode == "list":
    todos = [
        returns,
        T(2, "장보기", 70, color="GREEN", items=shop),
        T(3, "엄마한테 전화하기", 200, color="YELLOW"),
        T(4, "영양제 먹기", 3000, color="PURPLE", repeatType="WEEKLY", repeatDays=daily, dailyStart=21 * 60, dailyEnd=23 * 60, repeatAnchor=anchor),
        T(5, "관리비 내기", 3000, color="RED", repeatType="MONTHLY", monthDay=25, dailyStart=10 * 60, repeatAnchor=anchor),
        T(6, "과제 제출", 3000, color="ORANGE", startAt=ms(friday.replace(hour=9)), endAt=ms(friday.replace(hour=23, minute=59))),
        T(7, "도서관 책 반납", 3000, notify=False),
    ]
elif mode == "one":   # 알림 1개 (묶음 없이)
    todos = [T(1, "택배 반품 신청", 1, color="BLUE", memo="수거 신청까지 해야 끝")]
else:                 # 알림 3개 묶음: 맨 위가 방금 뜬 '신규'(체크리스트)
    todos = [
        T(1, "택배 반품 신청", 40, color="BLUE", memo="수거 신청까지 해야 끝", endAt=ms(today.replace(hour=23, minute=59))),
        T(2, "장보기", 2, color="GREEN", items=shop),
        T(3, "엄마한테 전화하기", 200, color="YELLOW"),
    ]
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
j = json.dumps(todos, ensure_ascii=False)
open(out + "/todos.xml", "w").write(f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n<string name=\"list\">{html.escape(j, quote=True)}</string>\n<int name=\"next_id\" value=\"20\" />\n</map>\n")
open(out + "/ui.xml", "w").write("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map><boolean name=\"onboarded\" value=\"true\" /><boolean name=\"exact_banner_hidden\" value=\"true\" /><boolean name=\"battery_banner_hidden\" value=\"true\" /><boolean name=\"review_asked\" value=\"true\" /></map>\n")
