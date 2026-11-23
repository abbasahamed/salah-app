# Repository Maintenance & Hygiene

This document defines repository maintenance standards, release hygiene, and git management practices for the **Salah** open-source repository.

---

## 1. Repository Structure & Boundaries

```text
Salah/
├── app/                  # Main Android application module
├── assets/
│   └── screenshots/      # Curated, public-facing screenshot gallery
├── core/
│   ├── designsystem/     # Design tokens, typography, colors, and icons
│   ├── location/         # Offline cities database & timezone spatial index
│   ├── prayer/           # Astronomical calculations & AlAdhan client
│   └── preferences/      # AndroidX DataStore preference management
├── docs/
│   ├── architecture/     # System architecture and technical specifications
│   ├── development/      # Setup, testing, and release workflows
│   ├── security/         # Security model and network policies
│   └── repository-maintenance.md # This document
├── tools/                # Offline geographic asset preprocessing tooling
├── .github/              # CI workflows and community templates
├── AGENTS.md             # Contributor and AI coding guidelines
├── CONTRIBUTING.md       # Open-source contribution guidelines
├── LICENSE               # Apache License 2.0
├── PRIVACY.md            # Privacy disclosures
├── README.md             # Project homepage
└── SECURITY.md           # Security policy and vulnerability disclosure
```

---

## 2. Media Asset Hygiene

1. **Curated Public Gallery Only:** Only a minimal, curated set of screenshots showcasing verified production features should exist in `assets/screenshots/`.
2. **Never Commit Iterative Test Screencaps:** Temporary test screenshots, screen recordings, device verification matrices, or emulator screencaps must not be committed to Git.
3. **External Archival:** Historical milestone screenshots from prior development cycles are permanently preserved in isolated backup vaults outside the repository (`~/Documents/Salah-Backups/`).

---

## 3. Git Commit & Release Guidelines

- **Atomic, Semantic Commits:** Follow Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`).
- **No Force-Pushes:** Do not force-push (`git push --force`) or rewrite public Git history on `main`.
- **Release Preservation:** The `v1.0.0` release tag and its associated release assets are immutable. Maintenance commits add clean documentation and build hygiene without moving existing release tags.

---

## 4. Secret & Dependency Auditing

- Run secret scanners (e.g. `gitleaks detect`) prior to tagging any release.
- Maintain pinned SHA actions in GitHub Actions workflows.
- Monitor dependencies via `gradle/libs.versions.toml`.

