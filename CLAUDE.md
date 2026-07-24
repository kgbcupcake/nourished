## Changelog: discipline

Whenever a task results in a shipped fix, feature, or behavior change, update
`CHANGELOG.md` as part of that same task — before considering the task done,
not as a separate follow-up.

- Edit the existing `CHANGELOG.md` in place. Never create a duplicate
  changelog file.
- Match the file's existing format/style exactly (headers, versioning,
  date format, etc.) — don't introduce a new structure.
- Check for an existing entry covering the same change before adding one;
  don't duplicate.
- If a task only produces internal/investigation output (no shipped change —
  e.g. a diagnostic pass, a read-only source review), do not add a changelog
  entry for it.
