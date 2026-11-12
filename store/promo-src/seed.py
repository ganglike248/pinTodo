# 홍보 영상용 예시 할 일 → todos.xml / ui.xml (디버그 빌드에 run-as로 넣음)
import json, datetime, html, sys, os
from zoneinfo import ZoneInfo
KST = ZoneInfo("Asia/Seoul"); now = datetime.datetime.now(KST)
ms = lambda d: int(d.timestamp() * 1000)
today = now.replace(hour=0, minute=0, second=0, microsecond=0)
ed = lambda d: (d.date() - datetime.date(1970, 1, 1)).days
def T(id, title, **k):
    t = dict(id=id, title=title, memo="", notify=True, pinned=True, alertMode="SILENT", repeatDays=[], repeatType="NONE",
             repeatInterval=1, monthDay=1, dailyStart=540, createdAt=ms(now) - 120_000 * (10 - id))
    t.update(k); return t
daily = list(range(1, 8)); anchor = ed(today) - 20
todos = [
    T(1, "팀 회의 자료 준비", color="BLUE", memo="발표 슬라이드 3장 · 수치 다시 확인"),
    T(2, "장보기", color="GREEN", items=[{"t": "우유", "d": True}, {"t": "계란", "d": True}, {"t": "두부", "d": False}, {"t": "사과", "d": False}, {"t": "키친타월", "d": False}]),
    T(3, "저녁 약 먹기", color="PURPLE", repeatType="WEEKLY", repeatDays=daily, dailyStart=17 * 60, dailyEnd=22 * 60, repeatAnchor=anchor),
    T(4, "물 한 잔 마시기", remindEvery=120),
    T(5, "영양제 챙기기", color="YELLOW", repeatType="WEEKLY", repeatDays=daily, dailyStart=8 * 60, repeatAnchor=anchor, hiddenKey=ms(today.replace(hour=8))),
    T(6, "요가 수업", color="RED", repeatType="WEEKLY", repeatDays=[2, 4], dailyStart=19 * 60 + 30, dailyEnd=21 * 60 + 30, repeatAnchor=anchor),
    T(7, "월세 이체", color="ORANGE", repeatType="MONTHLY", monthDay=25, dailyStart=10 * 60, repeatAnchor=anchor),
    T(8, "도서관 책 반납", notify=False),
]
if len(sys.argv) > 2 and sys.argv[2] == "noti":   # 알림 장면: PinTodo 알림 1개만 (2개부터는 묶음으로 접힘)
    todos = [T(1, "치과 예약 전화하기", color="BLUE", memo="오후 2시 전에 꼭!")]
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
j = json.dumps(todos, ensure_ascii=False)
open(out + "/todos.xml", "w").write(f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n<string name=\"list\">{html.escape(j, quote=True)}</string>\n<int name=\"next_id\" value=\"20\" />\n</map>\n")
open(out + "/ui.xml", "w").write("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map><boolean name=\"onboarded\" value=\"true\" /><boolean name=\"exact_banner_hidden\" value=\"true\" /><boolean name=\"battery_banner_hidden\" value=\"true\" /><boolean name=\"review_asked\" value=\"true\" /></map>\n")
