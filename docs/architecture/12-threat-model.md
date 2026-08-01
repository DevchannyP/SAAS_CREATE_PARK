# 12. Threat model

Assets are control source/secrets, target primary worktree, approved design,
patch integrity, evidence, and human authority. Adversaries include malicious
project content, compromised tools, path tricks, forged browser input, and
prompt-injected agents.

Required tests cover traversal, absolute/drive/UNC paths, symlink/junction,
`git -C`, child interpreters, other runs, `.env`/SSH keys, HTTP/DNS/localhost,
Docker socket, prompt injection, test deletion, forged scores, and SaaS edits.
Controls are OS/container boundaries, minimal mounts, path component checks,
typed commands, network denial, content hashes, immutable evidence, independent
governance, authorization, redaction, and append-only audits.

Residual risk: host/container-runtime compromise is outside application isolation
and requires hardened hosts, signed images, patching, and runtime monitoring.
