import json
import os
import subprocess
import uuid
import hashlib
from datetime import datetime, timedelta, timezone
from html import escape
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import urlopen

from engines.youtube_rank import calc_hot_score

def _demo_items(category_id: str):
    now = datetime.now(timezone.utc)
    topics = ["예능 반전", "게임 승부", "반려동물 순간"]
    items = []
    for index in range(18):
        topic = topics[index // 6]
        items.append({
            "videoId": f"demo{index + 1:07d}", "title": f"{topic} 화제 장면 {index % 6 + 1}",
            "channelTitle": f"Demo Channel {index + 1}", "description": "테스트 전용 합성 메타데이터",
            "tags": [topic, "hot6"], "categoryId": category_id,
            "publishedAt": (now - timedelta(hours=index + 1)).isoformat().replace("+00:00", "Z"),
            "viewCount": 900000 - index * 23000, "likeCount": 41000 - index * 900,
            "commentCount": 3200 - index * 80, "thumbnailUrl": "", "topic": topic,
        })
    return items

def _official_items(region: str, category_id: str, max_results: int):
    key = os.environ.get("YOUTUBE_API_KEY", "").strip()
    if not key:
        raise ValueError("YOUTUBE_API_KEY is required in live mode")
    query = urlencode({"part": "snippet,statistics,contentDetails", "chart": "mostPopular", "regionCode": region,
                       "videoCategoryId": category_id, "maxResults": max_results, "key": key})
    with urlopen("https://www.googleapis.com/youtube/v3/videos?" + query, timeout=15) as response:
        payload = json.load(response)
    items = []
    for raw in payload.get("items", []):
        snippet, stats = raw.get("snippet", {}), raw.get("statistics", {})
        items.append({"videoId": raw["id"], "title": snippet.get("title", ""), "channelTitle": snippet.get("channelTitle", ""),
                      "description": snippet.get("description", ""), "tags": snippet.get("tags", []), "categoryId": snippet.get("categoryId", category_id),
                      "publishedAt": snippet.get("publishedAt"), "viewCount": int(stats.get("viewCount", 0)), "likeCount": int(stats.get("likeCount", 0)),
                      "commentCount": int(stats.get("commentCount", 0)), "thumbnailUrl": snippet.get("thumbnails", {}).get("high", {}).get("url", ""),
                      "topic": "인기 영상"})
    return items

def collect_rank_group(config):
    region = str(config.get("regionCode", "KR")).upper()
    category = str(config.get("categoryId", "24"))
    maximum = min(max(int(config.get("maxResults", 18)), 6), 50)
    mode = os.environ.get("YOUTUBE_COLLECTOR_MODE", "demo").lower()
    items = _official_items(region, category, maximum) if mode == "live" else _demo_items(category)[:maximum]
    now = datetime.now(timezone.utc)
    for item in items:
        published = datetime.fromisoformat(item["publishedAt"].replace("Z", "+00:00"))
        item["hotScore"] = calc_hot_score(item["viewCount"], item["likeCount"], item["commentCount"], max((now - published).total_seconds() / 3600, 1))
    ranked = sorted(items, key=lambda item: item["hotScore"], reverse=True)
    topic_counts = {}
    for item in ranked:
        topic_counts[item["topic"]] = topic_counts.get(item["topic"], 0) + 1
    eligible = [topic for topic, count in topic_counts.items() if count >= 6]
    topic = eligible[0] if eligible else "인기 영상"
    selected, channels = [], set()
    for item in ranked:
        if item["topic"] == topic and item["channelTitle"] not in channels:
            channels.add(item["channelTitle"]); selected.append(item)
        if len(selected) == 6: break
    if len(selected) < 6:
        selected = ranked[:6]
    return {"mode": mode, "regionCode": region, "categoryId": category, "collectedCount": len(items), "videos": ranked,
            "group": {"title": f"{topic} TOP 6", "topicKeyword": topic,
                      "items": [{**item, "rankNo": index + 1, "reason": "hot_score와 채널 중복 제거 기준"} for index, item in enumerate(selected)]}}

def generate_magazine(payload):
    items = payload.get("items", [])
    if len(items) != 6:
        raise ValueError("exactly six ranked items are required")
    group = payload.get("group", {})
    entries = []
    for item in reversed(items):
        rank = int(item["rankNo"])
        title = str(item.get("title", ""))[:100]
        channel = str(item.get("channelTitle", ""))[:80]
        views = int(item.get("viewCount", 0))
        comments = int(item.get("commentCount", 0))
        entries.append({
            "rankNo": rank, "videoId": item.get("videoId"), "sourceTitle": title,
            "hotPart": {"evidence": "metadata_engagement", "reason": f"views={views}, comments={comments}, hot_score={item.get('score')}", "timestamp": None},
            "narration": f"{rank}\uc704\ub294 {channel}\uc758 {title}\uc785\ub2c8\ub2e4. \uc9e7\uc740 \uc2dc\uac04 \uc548\uc5d0 \ub192\uc740 \ubc18\uc751\uc744 \ub9cc\ub4e4\uba70 \uc624\ub298\uc758 \ud654\uc81c \uc601\uc0c1\uc73c\ub85c \uc62c\ub790\uc2b5\ub2c8\ub2e4.",
            "sketchPrompt": f"black and white pencil sketch, editorial ranking magazine, abstract scene inspired by: {title}, original composition, no logo, no copyrighted character, no real person likeness",
            "sourceAttribution": {"channelTitle": channel, "videoId": item.get("videoId")}
        })
    estimated = 8 + sum(max(7, len(entry["narration"]) // 8) for entry in entries) + 5
    quality_score = 94 if estimated <= 90 else 82
    return {"schemaVersion": 1, "jobId": payload.get("jobId"), "format": payload.get("format", "SHORTS"),
            "title": f"\uc624\ub298 \uc720\ud29c\ube0c\uc5d0\uc11c \ub09c\ub9ac\ub09c {group.get('topicKeyword', '')} TOP 6",
            "intro": "\uc624\ub298 \uc720\ud29c\ube0c\uc5d0\uc11c \uac00\uc7a5 \ub728\uac70\uc6b4 \uc601\uc0c1 TOP 6, \uc9c0\uae08 \uc2dc\uc791\ud569\ub2c8\ub2e4.",
            "entries": entries, "outro": "\ub0b4\uc77c\ub3c4 \ud654\uc81c\uc758 \uc601\uc0c1\ub9cc \uace8\ub77c \uc815\ub9ac\ud574\ub4dc\ub9b4\uac8c\uc694.",
            "estimatedDurationSec": estimated,
            "quality": {"score": quality_score, "checks": {"sixEntries": True, "durationWithin90Sec": estimated <= 90, "attributionIncluded": True}},
            "risk": {"score": 12, "level": "LOW", "checks": {"sourceClipUsed": False, "frameCopied": False, "privateUploadRequired": True}}}

def _srt_time(seconds):
    return f"00:00:{seconds:02d},000"

def render_preview(payload):
    job_id = str(uuid.UUID(str(payload.get("jobId"))))
    plan = payload.get("plan", {})
    entries = plan.get("entries", [])
    if len(entries) != 6:
        raise ValueError("a six-entry magazine plan is required")
    root = Path(os.environ.get("MAGAZINE_OUTPUT_ROOT", "/output")).resolve()
    target = (root / job_id).resolve()
    if target.parent != root:
        raise ValueError("invalid output path")
    target.mkdir(parents=True, exist_ok=True)
    cards = []
    for entry in entries:
        rank = int(entry["rankNo"])
        title = escape(str(entry.get("sourceTitle", ""))[:60])
        svg = target / f"rank_{rank}.svg"
        svg.write_text(f'''<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="1920"><rect width="1080" height="1920" fill="#0b0f18"/><circle cx="540" cy="680" r="310" fill="none" stroke="#ff4d2e" stroke-width="18"/><text x="540" y="770" text-anchor="middle" font-family="sans-serif" font-size="320" font-weight="bold" fill="#ffffff">{rank}</text><text x="540" y="1160" text-anchor="middle" font-family="sans-serif" font-size="46" fill="#ffffff">{title}</text><text x="540" y="1260" text-anchor="middle" font-family="sans-serif" font-size="30" fill="#8d99aa">ORIGINAL MAGAZINE CARD</text></svg>''', encoding="utf-8")
        cards.append(svg.name)
    srt_lines = []
    for index, entry in enumerate(entries):
        start, end = index * 2, index * 2 + 2
        srt_lines.extend([str(index + 1), f"{_srt_time(start)} --> {_srt_time(end)}", str(entry.get("narration", "")), ""])
    subtitle = target / "narration.srt"; subtitle.write_text("\n".join(srt_lines), encoding="utf-8")
    video = target / "preview.mp4"
    command = ["ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=0x0b0f18:s=1080x1920:r=30",
               "-f", "lavfi", "-i", "sine=frequency=220:sample_rate=48000", "-t", "12",
               "-vf", "drawbox=x=70:y=70:w=940:h=1780:color=0xff4d2e@0.8:t=8",
               "-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "96k", str(video)]
    completed = subprocess.run(command, capture_output=True, text=True, timeout=120)
    if completed.returncode != 0:
        raise RuntimeError("ffmpeg preview render failed: " + completed.stderr[-300:])
    manifest = {"schemaVersion": 1, "kind": "TECHNICAL_PREVIEW", "publishable": False, "jobId": job_id,
                "videoPath": f"{job_id}/preview.mp4", "subtitlePath": f"{job_id}/narration.srt", "rankCards": cards,
                "durationSec": 12, "resolution": "1080x1920", "audio": "synthetic_test_tone",
                "quality": {"score": 90, "checks": {"resolution": True, "duration": True, "sixCards": len(cards) == 6, "realTts": False}}}
    (target / "render_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest

def quality_check(payload):
    job_id = str(uuid.UUID(str(payload.get("jobId"))))
    root = Path(os.environ.get("MAGAZINE_OUTPUT_ROOT", "/output")).resolve()
    target = (root / job_id).resolve()
    if target.parent != root:
        raise ValueError("invalid output path")
    video, subtitle = target / "preview.mp4", target / "narration.srt"
    if not video.is_file() or not subtitle.is_file():
        raise ValueError("render outputs are incomplete")
    probe = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "stream=codec_type,codec_name,width,height:format=duration,size", "-of", "json", str(video)], capture_output=True, text=True, timeout=30)
    if probe.returncode != 0:
        raise RuntimeError("ffprobe failed: " + probe.stderr[-300:])
    media = json.loads(probe.stdout)
    streams = media.get("streams", [])
    video_stream = next((stream for stream in streams if stream.get("codec_type") == "video"), {})
    audio_stream = next((stream for stream in streams if stream.get("codec_type") == "audio"), {})
    duration = float(media.get("format", {}).get("duration", 0))
    size = int(media.get("format", {}).get("size", 0))
    subtitle_blocks = subtitle.read_text(encoding="utf-8").count(" --> ")
    cards = list(target.glob("rank_*.svg"))
    checks = {"width1080": video_stream.get("width") == 1080, "height1920": video_stream.get("height") == 1920,
              "h264Video": video_stream.get("codec_name") == "h264", "audioPresent": bool(audio_stream),
              "previewDuration": 5 <= duration <= 90, "nonEmptyFile": size > 10000,
              "sixSubtitleSegments": subtitle_blocks == 6, "sixRankCards": len(cards) == 6,
              "technicalPreviewOnly": True}
    passed = all(checks.values())
    score = round(sum(1 for value in checks.values() if value) / len(checks) * 100)
    report = {"schemaVersion": 1, "jobId": job_id, "passed": passed, "score": score, "publishable": False,
              "measured": {"width": video_stream.get("width"), "height": video_stream.get("height"), "durationSec": duration,
                           "sizeBytes": size, "videoCodec": video_stream.get("codec_name"), "audioCodec": audio_stream.get("codec_name"),
                           "subtitleSegments": subtitle_blocks, "rankCards": len(cards)}, "checks": checks}
    (target / "quality_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return report

def prepare_upload(payload):
    job_id = str(uuid.UUID(str(payload.get("jobId"))))
    root = Path(os.environ.get("MAGAZINE_OUTPUT_ROOT", "/output")).resolve()
    target = (root / job_id).resolve()
    if target.parent != root:
        raise ValueError("invalid output path")
    video = target / "preview.mp4"
    if not video.is_file():
        raise ValueError("preview video is missing")
    plan, quality = payload.get("plan", {}), payload.get("quality", {})
    if not quality.get("passed"):
        raise ValueError("a passed quality report is required")
    title = str(plan.get("title", "YouTube Hot 6"))[:100]
    sources = []
    for entry in plan.get("entries", []):
        attribution = entry.get("sourceAttribution", {})
        video_id = str(attribution.get("videoId", ""))
        sources.append({"rankNo": entry.get("rankNo"), "channelTitle": attribution.get("channelTitle", ""),
                        "videoId": video_id, "url": "https://www.youtube.com/watch?v=" + video_id})
    description = "\uc624\ub298\uc758 \ud654\uc81c \uc601\uc0c1\uc744 \ub7ad\ud0b9\uacfc \ud574\uc124\ub85c \uc815\ub9ac\ud55c \uc624\ub9ac\uc9c0\ub110 \ub9e4\uac70\uc9c4 \ucf58\ud150\uce20\uc785\ub2c8\ub2e4.\n\n\ucc38\uace0 \ucd9c\ucc98:\n" + "\n".join(f"{source['rankNo']}\uc704: {source['url']} ({source['channelTitle']})" for source in sources)
    digest = hashlib.sha256(video.read_bytes()).hexdigest()
    thumbnail = target / "upload_thumbnail.svg"
    thumbnail.write_text(f'''<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720"><rect width="1280" height="720" fill="#0b0f18"/><text x="640" y="280" text-anchor="middle" font-family="sans-serif" font-size="88" font-weight="bold" fill="#ffffff">YOUTUBE HOT 6</text><text x="640" y="440" text-anchor="middle" font-family="sans-serif" font-size="160" font-weight="bold" fill="#ff4d2e">TOP 6</text><text x="640" y="560" text-anchor="middle" font-family="sans-serif" font-size="34" fill="#9aa5b5">ORIGINAL RANKING MAGAZINE</text></svg>''', encoding="utf-8")
    blockers = ["technical_preview", "synthetic_test_audio"]
    package = {"schemaVersion": 1, "jobId": job_id, "readyForApiUpload": False, "blockers": blockers,
               "video": {"path": f"{job_id}/preview.mp4", "sha256": digest, "sizeBytes": video.stat().st_size},
               "thumbnail": {"path": f"{job_id}/upload_thumbnail.svg", "width": 1280, "height": 720},
               "metadata": {"snippet": {"title": title, "description": description, "tags": ["\uc720\ud29c\ube0c\ub7ad\ud0b9", "\ud56b\uc774\uc288", "TOP6", "\uc624\ub298\uc758\uc601\uc0c1"], "categoryId": "24"},
                            "status": {"privacyStatus": "private", "selfDeclaredMadeForKids": False}},
               "sources": sources, "qualityScore": quality.get("score"), "riskScore": payload.get("riskScore")}
    (target / "upload_package.json").write_text(json.dumps(package, ensure_ascii=False, indent=2), encoding="utf-8")
    return package

class Handler(BaseHTTPRequestHandler):
    def _json(self, status, body):
        data = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(status); self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data))); self.end_headers(); self.wfile.write(data)
    def do_GET(self):
        self._json(200, {"status": "UP", "mode": os.environ.get("YOUTUBE_COLLECTOR_MODE", "demo")}) if self.path == "/health" else self._json(404, {"error": "not_found"})
    def do_POST(self):
        try:
            body = json.loads(self._read_body() or b"{}")
            if self.path == "/v1/collect-rank-group": self._json(200, collect_rank_group(body))
            elif self.path == "/v1/generate-magazine": self._json(200, generate_magazine(body))
            elif self.path == "/v1/render-preview": self._json(200, render_preview(body))
            elif self.path == "/v1/quality-check": self._json(200, quality_check(body))
            elif self.path == "/v1/prepare-upload": self._json(200, prepare_upload(body))
            else: self._json(404, {"error": "not_found"})
        except Exception as exc:
            self._json(422, {"error": type(exc).__name__, "detail": str(exc)[:300]})
    def _read_body(self):
        limit = 65536
        if self.headers.get("Transfer-Encoding", "").lower() != "chunked":
            return self.rfile.read(min(int(self.headers.get("Content-Length", "0")), limit))
        data = bytearray()
        while True:
            size_line = self.rfile.readline(32).strip().split(b";", 1)[0]
            size = int(size_line, 16)
            if size == 0:
                self.rfile.readline(); break
            if len(data) + size > limit:
                raise ValueError("request body exceeds 65536 bytes")
            data.extend(self.rfile.read(size)); self.rfile.read(2)
        return bytes(data)
    def log_message(self, pattern, *args):
        print(pattern % args, flush=True)

if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 8090), Handler).serve_forever()
