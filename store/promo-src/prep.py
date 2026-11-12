# 녹화본 → 30fps JPG 프레임(work/clips/rN) + 화면이 바뀌는 구간 출력(장면 시각 맞출 때 참고)
import subprocess, sys, os, json, numpy as np
from PIL import Image
W = os.path.join(os.path.dirname(os.path.abspath(__file__)), "work")
counts = {}
for name in sys.argv[1:] or ["r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8"]:
    src = f"{W}/rec/{name}.mp4"; out = f"{W}/clips/{name}"; os.makedirs(out, exist_ok=True)
    for f in os.listdir(out): os.remove(os.path.join(out, f))
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", src, "-vf", "fps=30,tpad=stop_mode=clone:stop_duration=6,scale=720:1600:flags=lanczos",
                    "-q:v", "3", f"{out}/%04d.jpg"], check=True)
    fs = sorted(os.listdir(out)); counts[name] = len(fs); prev = None; ev = []
    for i, f in enumerate(fs):
        a = np.asarray(Image.open(os.path.join(out, f)).convert("L").crop((0, 80, 720, 1600)).resize((180, 380)), dtype=np.float32)
        if prev is not None and np.abs(a - prev).mean() > 0.25: ev.append(i / 30)
        prev = a
    g = []
    for t in ev:
        if g and t - g[-1][1] <= 0.15: g[-1][1] = t
        else: g.append([t, t])
    print(name, len(fs), [(round(a, 2), round(b, 2)) for a, b in g])
old = json.load(open(f"{W}/frames.json")) if os.path.exists(f"{W}/frames.json") else {}
old.update(counts); json.dump(old, open(f"{W}/frames.json", "w"))
