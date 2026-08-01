# YouTube Hot 6 Orchestrator Scaffold

This directory is isolated from ForgeFlow's existing runtime. The scaffold contains the pipeline contract and pure ranking logic only. External collection, generation, rendering, and upload adapters are disabled until credentials and human approval policies are configured.

Run the local unit test from this directory:

```powershell
python -m unittest discover -s tests -v
```

Safety defaults: metadata-only inputs, no source video download, original sketch generation, private uploads, and mandatory approval before upload preparation.
