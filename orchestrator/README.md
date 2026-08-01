# YouTube Hot 6 Orchestrator Scaffold

This directory is isolated from ForgeFlow's existing runtime. The scaffold contains the pipeline contract and pure ranking logic only. External collection, generation, rendering, and upload adapters are disabled until credentials and human approval policies are configured.

Run the local unit test from this directory:

```powershell
python -m unittest discover -s tests -v
```

Safety defaults: metadata-only inputs, no source video download, original sketch generation, private uploads, and mandatory approval before upload preparation.

## Collector modes

- `YOUTUBE_COLLECTOR_MODE=demo`: generates deterministic synthetic metadata and never calls an external service.
- `YOUTUBE_COLLECTOR_MODE=live`: calls the official `videos.list` `mostPopular` endpoint and requires `YOUTUBE_API_KEY`.

The container exposes `GET /health`, `POST /v1/collect-rank-group`, `POST /v1/generate-magazine`, `POST /v1/render-preview`, `POST /v1/quality-check`, and `POST /v1/prepare-upload` only inside the Compose network. The browser always communicates through the Spring management API. Render previews are marked non-publishable and stored in the dedicated `/output` volume; quality checks independently measure them with `ffprobe`. Upload preparation creates metadata and integrity artifacts only and never calls YouTube.
