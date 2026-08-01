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
