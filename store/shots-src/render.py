# compose.html → store/screenshots/01~08.png (1080×2160), store/feature-1024x500.png (2배로 그린 뒤 줄여서 선명하게)
# python3 store/shots-src/render.py [출력폴더]  (기본: store/)
import sys, os, asyncio
from playwright.async_api import async_playwright
from PIL import Image
P = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.dirname(P)
NAMES = ["01-hook", "02-list", "03-quick-add", "04-notification", "05-snooze", "06-repeat", "07-widget", "08-no-ads"]
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--allow-file-access-from-files"])
        pg = await b.new_page(viewport={"width": 1200, "height": 1200}, device_scale_factor=1)
        await pg.goto(f"file://{P}/compose.html")
        await pg.wait_for_function("window.READY === true")
        os.makedirs(f"{OUT}/screenshots", exist_ok=True)
        for i, n in enumerate(NAMES):
            await pg.locator(f"#s{i + 1}").screenshot(path=f"{OUT}/screenshots/{n}.png")
        pg2 = await b.new_page(viewport={"width": 1200, "height": 800}, device_scale_factor=2)
        await pg2.goto(f"file://{P}/compose.html")
        await pg2.wait_for_function("window.READY === true")
        tmp = f"{OUT}/.feature@2x.png"
        await pg2.locator("#feature2").screenshot(path=tmp)
        Image.open(tmp).convert("RGB").resize((1024, 500), Image.LANCZOS).save(f"{OUT}/feature-1024x500.png")
        os.remove(tmp)
        await b.close()
        print("done", OUT)
asyncio.run(main())
