#!/usr/bin/env bash
# PreToolUse hook (Bash, git commit*): blocks commits that don't touch CHANGELOG.md.
# Only Claude can write a good prose entry, so this just forces the step, not the wording.
set -euo pipefail

REPO_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$REPO_DIR"

if git diff --cached --name-only 2>/dev/null | grep -qx 'CHANGELOG.md'; then
  exit 0
fi

cat <<'EOF'
{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"CHANGELOG.md has no staged changes. Before committing: add an entry under the \"[ Unreleased ]\" section (Added/Changed/Fixed/Removed, matching the file's existing style) describing this change, `git add CHANGELOG.md`, then retry the commit."}}
EOF
exit 0