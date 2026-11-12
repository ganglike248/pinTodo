# promo.html을 30fps로 캡처해 mp4로: python3 render.py h|v 출력.mp4 [미리보기 시각,시각,...]
import sys, os, json, subprocess, asyncio
from playwright.async_api import async_playwright
P = os.path.dirname(os.path.abspath(__file__))
mode, out = sys.argv[1], os.path.abspath(sys.argv[2])
times = [float(x) for x in sys.argv[3].split(",")] if len(sys.argv) > 3 else None
W, H = (1080, 1920) if mode == "v" else (1920, 1080)
FRAMES = json.load(open(f"{P}/work/frames.json"))
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--allow-file-access-from-files"])
        pg = await b.new_page(viewport={"width": W, "height": H}, device_scale_factor=1)
        await pg.add_init_script(f"window.FRAMES = {json.dumps(FRAMES)};")
        await pg.goto(f"file://{P}/promo.html" + ("#v" if mode == "v" else ""))
        await pg.wait_for_function("window.READY === true")
        total = await pg.evaluate("TOTAL")
        if times:
            for i, t in enumerate(times):
                await pg.evaluate(f"render({t})"); await pg.screenshot(path=f"{out}_{i:02d}.png")
            await b.close(); return
        n = int(round(total * 30))
        ff = subprocess.Popen(["ffmpeg", "-v", "error", "-y", "-f", "image2pipe", "-framerate", "30", "-i", "-",
            "-f", "lavfi", "-i", "anullsrc=r=44100:cl=stereo", "-shortest",
            "-c:v", "libx264", "-preset", "slow", "-crf", "17", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "96k",
            "-movflags", "+faststart", out], stdin=subprocess.PIPE)
        for f in range(n):
            await pg.evaluate(f"render({f / 30})")
            ff.stdin.write(await pg.screenshot(type="png"))
            if f % 300 == 0: print(mode, f, "/", n, flush=True)
        ff.stdin.close(); ff.wait(); await b.close(); print("done", out, total)
asyncio.run(main())
