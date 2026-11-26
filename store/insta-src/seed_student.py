# 학생 쓰임새 게시물 촬영용 예시: 마감이 있는 과제·발표·시험 → todos.xml / ui.xml  (python seed_student.py 출력폴더)
import json, datetime, html, sys, os
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
now = datetime.datetime.now().astimezone()
ms = lambda d: int(d.timestamp() * 1000)
today = now.replace(hour=0, minute=0, second=0, microsecond=0)
day = lambda n, h, m=0: (today + datetime.timedelta(days=n)).replace(hour=h, minute=m)

def T(id, title, mins_ago, **k):
    t = dict(id=id, title=title, memo="", notify=True, pinned=True, alertMode="SILENT", repeatDays=[], repeatType="NONE",
             repeatInterval=1, monthDay=1, dailyStart=540, createdAt=ms(now) - mins_ago * 60_000)
    t.update(k); t.setdefault("alertedKey", t["createdAt"]); return t

todos = [
    T(1, "자료구조 과제 제출", 30, color="RED", memo="LMS에 PDF로 올리기", endAt=ms(day(0, 23, 59))),
    T(2, "조별 발표 자료 보내기", 90, color="BLUE", endAt=ms(day(1, 18))),
    T(3, "중간고사 범위 정리", 200, color="GREEN", items=[{"t": "3장 스택·큐", "d": True}, {"t": "4장 트리", "d": False}, {"t": "5장 그래프", "d": False}]),
    T(4, "도서관 책 반납", 3000, color="ORANGE", startAt=ms(day(3, 10))),
]
j = json.dumps(todos, ensure_ascii=False)
open(out + "/todos.xml", "w", encoding="utf-8").write(f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n<string name=\"list\">{html.escape(j, quote=True)}</string>\n<int name=\"next_id\" value=\"40\" />\n</map>\n")
open(out + "/ui.xml", "w", encoding="utf-8").write("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map><boolean name=\"onboarded\" value=\"true\" /><boolean name=\"exact_banner_hidden\" value=\"true\" /><boolean name=\"battery_banner_hidden\" value=\"true\" /><boolean name=\"review_asked\" value=\"true\" /></map>\n")
print("ok", len(todos))
