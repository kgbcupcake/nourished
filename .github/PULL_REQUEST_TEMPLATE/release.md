# Release Gate Checklist

## 🔴 Runtime Correctness
- [ ] No client/server desync in gameplay state (diet, nutrients, effects)
- [ ] All server-side state mutations are synced to clients
- [ ] No missing effect reapplication after state changes
- [ ] No unsafe mutation of player or shared global state
- [ ] No gameplay logic depends on client-only state

## 🧩 API Safety
- [ ] No null-dereferenced public API entry points
- [ ] Public API methods are safe for external mod use
- [ ] No unintended lifecycle exposure
- [ ] External mods cannot corrupt internal system state via API

## ⚙️ Performance
- [ ] No per-tick computations that can be cached or event-driven
- [ ] No repeated recalculation of food classification, nutrient values, balance score, or decay logic
- [ ] No object allocation in confirmed hot paths (tick / eat / render)

## 🚫 Scope Integrity
- [ ] No feature redesigns bundled into this release
- [ ] No incomplete systems flagged as bugs (synergies, milestones)
- [ ] All changes map to confirmed runtime behavior issues

## 📋 Release Tasks
- [ ] Audit prompt run against repomix — no 🔴 issues
- [ ] `scanner_spec.json` deleted if bundled tags changed
- [ ] Jar built with `clean` — copied to mods folder and tested
- [ ] Changelog generated (`git-cliff --bump`)
- [ ] Version bumped in `gradle.properties`
- [ ] Committed and pushed to `dev`
- [ ] Merged to `main`
- [ ] Tag created and pushed (triggers Modrinth publish)
