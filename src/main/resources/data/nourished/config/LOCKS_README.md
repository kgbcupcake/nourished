# Nourished: Config Locks

`locks.json` lets a server operator or modpack creator lock specific config
keys against client-side (config screen) edits, and mark others as
server-authoritative.

## Schema

```json
{
  "locked": [
    "nourished:decay_rate_multiplier"
  ],
  "server_only": [
    "nourished:hardcore_mode"
  ]
}
```

A JSON object with two arrays of config key strings:

- `locked`: keys that cannot be changed from the client config screen once
  locked
- `server_only`: keys that only the server's value applies; client-side
  overrides are ignored entirely

## Which file wins

1. Bundled built-in defaults (ships empty by default)
2. `config/nourished/locks.json` on disk — edit this one
3. A datapack override, if present, replaces the config folder version
   entirely

## Notes

- Run `/nourished reload` to pick up changes without restarting