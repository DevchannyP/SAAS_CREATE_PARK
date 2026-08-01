import json
import os
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
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
