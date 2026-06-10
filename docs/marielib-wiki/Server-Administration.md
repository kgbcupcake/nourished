# Server Administration

MarieLib fully supports dedicated NeoForge servers. Commands register under the consuming mod's id (e.g. `/nourished` for Nourished).

---

## 📦 Installing MarieLib on a Server

Both the consuming mod **and** MarieLib must be installed:

| Component | Required on |
|---|---|
| MarieLib | Server + all clients |
| Consuming mod (e.g. Nourished) | Server + all clients |

Verify matching versions of Minecraft, NeoForge, MarieLib, and the consuming mod before opening to players.

---

## 🌐 Server-Authoritative Tracking

MarieLib uses a server-authoritative architecture. The server controls:

- Value levels and decay
- Tracking profiles
- Source classifications
- Datapack-loaded definitions
- Compatibility settings

Clients receive synchronized data from the server. See [Multiplayer & Sync](Multiplayer-and-Sync).

---

## 🔄 Reloading

After datapack changes:

```
/<modId> reload
```

If only classifications changed:

```
/<modId> invalidatecache
```

may be sufficient.

---

## 🔒 Module Locks

Modpack authors can lock config modules server-side:

```json
{
  "marie_schema_version": 1,
  "locked": ["enableEffects", "enableDecay"],
  "server_only": ["bonusEffectThreshold"]
}
```

Path: `data/<ns>/<modId>/module_locks/<id>.json`

---

## 🔍 Diagnostics Workflow

When investigating issues on a live server:

1. Hold the problematic item
2. Run `/<modId> debug held`
3. Review `config/<modId>/debug/trace_dump.txt`
4. Run `/<modId> diagnostics` for datapack validation
5. Check `config/<modId>/unassigned_sources.txt` for gaps

---

## ✅ Recommended Workflow

1. Install matching MarieLib + consuming mod versions on server and clients
2. Verify datapacks load successfully
3. Run `/<modId> diagnostics` after major updates
4. Use classification traces when investigating food issues
5. Keep compat overrides under version control
6. Run `/<modId> invalidatecache` after scanner config changes

---

## 📚 Related Pages

- [Commands Reference](Commands-Reference)
- [Diagnostics & Tracing](Diagnostics-and-Tracing)
- [Multiplayer & Sync](Multiplayer-and-Sync)
- [Configuration](Configuration)
