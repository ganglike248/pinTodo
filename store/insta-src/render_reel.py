# reel.html?id=이름 → store/insta/reel-이름.mp4 (1080×1920, 30fps, H.264). 이름: intro(소개), day(같은 하루 다른 결과)
# 준비: pip install playwright imageio-ffmpeg  (브라우저는 이미 있는 Chrome/Playwright Chromium을 씀)
# 사용: python store/insta-src/render_reel.py intro           → 전체 렌더링
#       python store/insta-src/render_reel.py intro 1.2,5,9   → 그 시각(초)의 프레임만 work/reel/이름-*.png 로 (확인용)
import sys, os, glob, subprocess, shutil
from playwright.sync_api import sync_playwright
import imageio_ffmpeg

P = os.path.dirname(os.path.abspath(__file__))
ID = sys.argv[1] if len(sys.argv) > 1 else "intro"
OUT = os.path.join(os.path.dirname(P), "insta", f"reel-{ID}.mp4")
WORK = os.path.join(P, "work", "reel")
FPS = 30
SUB = 6   # 한 프레임을 SUB번 나눠 찍어 평균을 냄 → 움직이는 부분에 자연스러운 잔상(모션 블러)

def browser_path():
    cands = sorted(glob.glob(os.path.join(os.environ.get("LOCALAPPDATA", ""), "ms-playwright", "chromium-*", "chrome-win*", "chrome.exe")))
    cands += [r"C:\Program Files\Google\Chrome\Application\chrome.exe", r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"]
    return next((c for c in reversed(cands) if os.path.exists(c)), None)

def main():
    preview = [float(x) for x in sys.argv[2].split(",")] if len(sys.argv) > 2 else None
    os.makedirs(WORK, exist_ok=True)
    with sync_playwright() as p:
        b = p.chromium.launch(executable_path=browser_path(), args=["--allow-file-access-from-files", "--force-device-scale-factor=1"])
        pg = b.new_page(viewport={"width": 1080, "height": 1920}, device_scale_factor=1)
        pg.goto("file:///" + P.replace("\\", "/") + "/reel.html?id=" + ID)
        pg.wait_for_function("window.READY === true")
        dur = pg.evaluate("window.DURATION")
        if preview:
            for t in preview:
                pg.evaluate("t => window.seek(t)", t * 1000)
                pg.screenshot(path=os.path.join(WORK, f"{ID}-{t:05.2f}.png"))
            b.close(); print("preview", WORK); return
        frames = os.path.join(WORK, "frames")
        shutil.rmtree(frames, ignore_errors=True); os.makedirs(frames)
        n = int(dur / 1000 * FPS * SUB)
        for i in range(n):
            pg.evaluate("t => window.seek(t)", i * 1000 / (FPS * SUB))
            pg.screenshot(path=os.path.join(frames, f"{i:05d}.png"))
            if i % 300 == 0: print(f"{i}/{n}", flush=True)
        b.close()
    # 소리 없는 오디오 트랙을 같이 넣음(일부 편집기·업로드에서 오디오가 없으면 거부하는 경우 대비). 음악은 인스타에서 고름
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-loglevel", "error", "-framerate", str(FPS * SUB), "-i", os.path.join(frames, "%05d.png"),
                    "-f", "lavfi", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100", "-shortest", "-vf", f"tmix=frames={SUB},fps={FPS}",
                    "-c:v", "libx264", "-preset", "slow", "-crf", "16", "-pix_fmt", "yuv420p", "-r", str(FPS),
                    "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", OUT], check=True)
    print("done", OUT, os.path.getsize(OUT) // 1024, "KB")

main()
