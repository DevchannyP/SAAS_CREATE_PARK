# YouTube Hot 6 Ranking Magazine Scaffold

## Scope

The first increment establishes a testable control plane without collecting, downloading, rendering, or uploading third-party media. It is isolated under three boundaries:

- `io.forgeflow.youtubemagazine`: management API and persistence
- `frontend/src/YoutubeMagazineApp.tsx`: dedicated `/youtube-magazine` console
- `orchestrator`: pipeline DSL and engine adapter namespace

## State flow

`DRAFT -> APPROVED -> UPLOAD_READY`

`UPLOAD_READY` means an approved job is eligible for a future uploader adapter. It does not perform a YouTube upload in this scaffold. This keeps local tests deterministic and prevents accidental publication.

## API contract

- `GET/POST /api/v1/youtube-magazine/jobs`
- `GET /api/v1/youtube-magazine/jobs/{id}`
- `POST /api/v1/youtube-magazine/jobs/{id}/approve`
- `POST /api/v1/youtube-magazine/jobs/{id}/upload`
- `GET /api/v1/youtube-magazine/videos`
- `GET /api/v1/youtube-magazine/groups`

All commands inherit ForgeFlow's trusted-gateway, request ID, actor, and idempotency protections.

## Isolation rules

1. Use the official YouTube Data API for public metadata.
2. Do not download or splice source videos.
3. Generate original illustrations from abstract scene descriptions; do not trace source frames.
4. Keep upload privacy `private` by default.
5. Require human approval until quality and risk gates are implemented and validated.
6. Mount future credentials only into the orchestrator/uploader runtime, never the web client.

## Next adapters

Implement each disabled DSL stage behind its existing engine namespace. Pass artifacts between stages using a per-job workspace outside the current ForgeFlow control root. The Spring API should submit jobs and read sanitized status/artifact metadata; it should not execute FFmpeg or OAuth operations in-process.

## Implemented MVP collection path

The isolated `youtube-orchestrator` service now supports demo and live collection modes. The management API calls it through the internal Compose network, persists every collected candidate, and creates a six-item ranked group with duplicate channels removed. The console exposes this flow through the **인기 영상 수집** button. Demo mode is the default; live mode is opt-in through environment configuration.

## Implemented magazine-plan path

A job linked to an exact six-item group can generate a persisted `MAGAZINE_PLAN` artifact. The artifact contains a 6-to-1 countdown, commentary-only narration, metadata-based hot-part evidence, source attribution, original-sketch prompts, estimated duration, quality checks, and content-risk checks. Generation advances a draft job to `SCRIPT_READY`; human approval remains mandatory before upload preparation.

## Implemented technical-render path

The orchestrator can produce a non-publishable 1080x1920 FFmpeg technical preview, six original SVG ranking cards, an SRT file, and a `RENDER_MANIFEST`. Files are written to a dedicated Docker volume; the API mounts that volume read-only and streams previews to the console. The audio is an explicit synthetic test tone, not TTS. Approval requires `RENDERED_PREVIEW`, quality >= 90, and risk <= 30.

The separate quality stage uses `ffprobe` to measure the rendered file rather than trusting render metadata. It checks dimensions, H.264 video, an audio stream, preview duration, non-empty output, six subtitle segments, and six rank cards. A persisted `QUALITY_REPORT` and `QUALITY_PASSED` stage are required before human approval.

## Implemented upload-package path

After human approval, the orchestrator creates an `UPLOAD_PACKAGE` containing private-by-default YouTube metadata, a 1280x720 original SVG thumbnail, six source links, the rendered MP4 SHA-256, quality and risk scores, and explicit upload blockers. Technical previews remain `readyForApiUpload=false` because they contain synthetic test audio and are not final renders. No YouTube API write is performed by this stage.
