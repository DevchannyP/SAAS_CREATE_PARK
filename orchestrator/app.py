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

class Handler(BaseHTTPRequestHandler):
    def _json(self, status, body):
        data = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(status); self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data))); self.end_headers(); self.wfile.write(data)
    def do_GET(self):
        self._json(200, {"status": "UP", "mode": os.environ.get("YOUTUBE_COLLECTOR_MODE", "demo")}) if self.path == "/health" else self._json(404, {"error": "not_found"})
    def do_POST(self):
        if self.path != "/v1/collect-rank-group": return self._json(404, {"error": "not_found"})
        try:
            length = min(int(self.headers.get("Content-Length", "0")), 4096)
            body = json.loads(self.rfile.read(length) or b"{}")
            self._json(200, collect_rank_group(body))
        except Exception as exc:
            self._json(422, {"error": type(exc).__name__, "detail": str(exc)[:300]})
    def log_message(self, pattern, *args):
        print(pattern % args, flush=True)

if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 8090), Handler).serve_forever()
