# 기록 탭 촬영용 예시: 이번 주에 완료한 할 일 여러 개 + 진행 중 몇 개 → todos.xml / ui.xml
import json, datetime, html, sys, os
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
now = datetime.datetime.now().astimezone()
ms = lambda d: int(d.timestamp() * 1000)
today = now.replace(hour=0, minute=0, second=0, microsecond=0)

def T(id, title, **k):
    t = dict(id=id, title=title, memo="", notify=True, pinned=True, alertMode="SILENT", repeatDays=[], repeatType="NONE",
             repeatInterval=1, monthDay=1, dailyStart=540, createdAt=ms(now) - 3 * 24 * 3600_000)
    t.update(k); t.setdefault("alertedKey", t["createdAt"]); return t

def done(id, title, days_ago, h, m, **k):
    d = (today - datetime.timedelta(days=days_ago)).replace(hour=h, minute=m)
    if d > now: d = now - datetime.timedelta(minutes=5)
    return T(id, title, doneAt=ms(d), createdAt=ms(d) - 6 * 3600_000, **k)

todos = [
    T(1, "장보기", color="GREEN", items=[{"t": "우유", "d": True}, {"t": "계란", "d": True}, {"t": "두부", "d": False}]),
    T(2, "엄마한테 전화하기", color="YELLOW"),
    done(10, "택배 반품 신청", 0, 9, 40, color="BLUE", memo="수거 신청까지 해야 끝"),
    done(11, "관리비 내기", 0, 8, 15, color="RED"),
    done(12, "보고서 제출", 1, 17, 50, color="ORANGE"),
    done(13, "세탁소 옷 찾기", 1, 12, 20),
    done(14, "도서관 책 반납", 2, 19, 5, color="PURPLE"),
    done(15, "치과 예약 전화", 3, 10, 30, color="BLUE"),
    done(16, "우유 사기", 3, 18, 40, color="GREEN"),
]
j = json.dumps(todos, ensure_ascii=False)
open(out + "/todos.xml", "w", encoding="utf-8").write(f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n<string name=\"list\">{html.escape(j, quote=True)}</string>\n<int name=\"next_id\" value=\"40\" />\n</map>\n")
open(out + "/ui.xml", "w", encoding="utf-8").write("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map><boolean name=\"onboarded\" value=\"true\" /><boolean name=\"exact_banner_hidden\" value=\"true\" /><boolean name=\"battery_banner_hidden\" value=\"true\" /><boolean name=\"review_asked\" value=\"true\" /></map>\n")
print("ok", len(todos))
