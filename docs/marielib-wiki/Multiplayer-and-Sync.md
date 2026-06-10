# Multiplayer & Sync

MarieLib uses a server-authoritative architecture for player tracking data.

---

## 🌐 How It Works

The server is always the source of truth for:

- All registered value levels
- Total count
- Active tracking profile
- Source memory state
- Effect application

Clients receive synchronized snapshots from the server. Value changes initiated on the server propagate to connected clients through the consuming mod's network layer.

---

## 📡 What Syncs

| Data | Direction | Notes |
|---|---|---|
| Value levels | Server → Client | On change and on join |
| Total count | Server → Client | With value deltas |
| Config snapshot | Server → Client | Module locks and thresholds |
| Full tracking data | Server → Client | On world join |

The consuming mod wires sync callbacks through `MarieLibContext` (`onValuesDeltaReceived`, `onFullTrackingDataSynced`).

---

## 🔄 Current Status

Network sync infrastructure is **actively being expanded**. Core value delta sync works in reference implementations like Nourished, but additional sync features (config snapshots, profile sync, milestone state) are still in progress.

If players report mismatched values:

1. Verify same MarieLib version on server and all clients
2. Verify same consuming mod version
3. Run `/<modId> reload` and have affected players rejoin

---

## 📚 Related Pages

- [Server Administration](Server-Administration)
- [Configuration](Configuration)
- [Commands Reference](Commands-Reference)
