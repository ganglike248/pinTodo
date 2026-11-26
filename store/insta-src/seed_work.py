# 직장인 쓰임새 게시물 촬영용 예시: 일하다 생기는 자잘한 일 → todos.xml / ui.xml  (python seed_work.py 출력폴더)
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
    T(1, "거래처 메일 회신", 20, color="BLUE", memo="견적서 첨부"),
    T(2, "경비 정산 올리기", 120, color="ORANGE", endAt=ms(day(0, 23, 59))),
    T(3, "회의 자료 출력", 60, color="RED", startAt=ms(day(0, 15))),
    T(4, "주간 보고 작성", 3000, color="PURPLE", repeatType="WEEKLY", repeatDays=[5], dailyStart=16 * 60, repeatAnchor=(today.date() - datetime.date(1970, 1, 1)).days - 20),
    T(5, "사무용품 주문", 3000, notify=False),
]
j = json.dumps(todos, ensure_ascii=False)
open(out + "/todos.xml", "w", encoding="utf-8").write(f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n<string name=\"list\">{html.escape(j, quote=True)}</string>\n<int name=\"next_id\" value=\"40\" />\n</map>\n")
open(out + "/ui.xml", "w", encoding="utf-8").write("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map><boolean name=\"onboarded\" value=\"true\" /><boolean name=\"exact_banner_hidden\" value=\"true\" /><boolean name=\"battery_banner_hidden\" value=\"true\" /><boolean name=\"review_asked\" value=\"true\" /></map>\n")
print("ok", len(todos))
