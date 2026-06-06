# Nourished Roadmap

- Nourished began as a nutrition mod and has gradually evolved into a data-driven
  nutrition framework focused on classification, diagnostics, validation, and extensibility.

This roadmap reflects the long-term direction of the project.

---

## Infrastructure

### Server-Authoritative Synchronization

- Nutrition synchronization
- Registry synchronization
- Snapshot validation
- Multiplayer reliability

### Nourished Validation Engine

- Datapack validation
- Nutrient validation
- Classification coverage analysis
- Validation report generation

### Crash Diagnostics

- Runtime state capture
- Classification trace reporting
- Sync failure diagnostics
- Root cause analysis

### Nourished Compiler

- Architecture validation
- Registry validation
- Configuration validation
- Compiler-style diagnostics
- Automated fix recommendations

---

## Ecosystem Integrations

### Farmers Delight Integration

- Food classification support
- Nutrition balancing
- Datapack compatibility

### Quest & Progression Integration

- Quest triggers
- Nutrition objectives
- Progression hooks

### Advancement Integration

- Nutrition milestones
- Balanced diet achievements
- Progression tracking

### Accessories Integration

- Curios support
- Equipment-based nutrition effects
- Accessory hooks

---

## Long-Term Vision

Nourished is evolving toward a platform that provides:

- Data-driven nutrition systems
- Extensible addon support
- KubeJS integration
- Validation and diagnostics tooling
- Multiplayer-safe synchronization
- Framework-level APIs for other mods

- The goal is to make Nourished not only a nutrition mod, but a foundation for
  nutrition-focused gameplay, datapacks, addons, and integrations.

---

## Classification Investigation Note

### ID

CLS-017

Investigate unexpected protein contribution in Create Food ice cream sandwich classifications

### Priority

Low

### Status

Open

### Description

While reviewing runtime classification traces, a protein signal was detected for `createfood:chorus_fruit_ice_cream_sandwich`.

The final classification appears reasonable and no player-facing issue has been observed. However, the runtime resolver generated a protein contribution that was not immediately explainable from the known recipe composition.

### Example Trace

Item:
`createfood:chorus_fruit_ice_cream_sandwich`

Runtime Resolver:

- proteins: 0.3286
- fruits: 0.2571
- grains: 0.4143

Tag Contribution:

- dairy: 1.0000

Final Blend:

- dairy: 0.6667
- proteins: 0.1095
- grains: 0.1381
- fruits: 0.0857

### Expected Understanding

Recipe components appear to consist primarily of:

- Chorus Fruit Ice Cream
- Chocolate Graham Cracker

Expected nutrient sources:

- Dairy
- Fruits
- Grains

Protein contribution source is currently unknown.

### Investigation Goals

- Determine which ingredient or resolver rule contributes the protein signal.
- Verify whether dairy-derived ingredients intentionally generate protein weight.
- Verify whether inherited classifications from intermediate recipe components
- (i.e. `createfood:chorus_fruit_ice_cream_sandwich` -> `createfood:chorus_fruit_ice_cream` -> `createfood:chorus_fruit`)
  are contributing protein signals
- Confirm whether current behavior is expected.

### Current Action

No code changes required.

Classification result is acceptable and does not negatively affect gameplay.

### Future Improvement

ClassificationTrace should eventually support ingredient-level signal attribution.

Example:

Protein Sources:

- milk -> +0.20
- cream -> +0.08

This would allow rapid diagnosis of unexpected nutrient contributions.
