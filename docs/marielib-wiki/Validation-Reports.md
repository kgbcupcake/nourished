# Validation Reports

MarieLib includes a datapack validation engine that reports errors and warnings for loaded content.

---

## 🧪 Running Validation

```
/<modId> diagnostics
```

Runs validation against all loaded MarieLib datapack content and prints results to chat.

---

## 📋 What Gets Validated

The `DatapackDiagnostics` engine checks for:

- Missing value key references
- Invalid JSON schema fields
- Broken item or effect ID references
- Malformed datapack entries
- Missing `marie_schema_version` keys
- Conflicting source classifications

---

## 📊 Output Format

Diagnostics are printed with severity levels:

| Severity | Color | Meaning |
|---|---|---|
| ERROR | Red | Will cause broken behavior |
| WARNING | Yellow | May cause unexpected behavior |

Output is capped at 100 lines in chat. Full diagnostic history is available through the diagnostics singleton.

---

## 🔧 Schema Templates

Generate valid JSON templates for any supported datapack type:

```
/<modId> schema source_classifications
/<modId> schema effects
/<modId> schema compat
```

See [Datapack Support](Datapack-Support) for the full list of types.

---

## 💡 Tips

- Run diagnostics after every datapack change.
- Fix ERROR-level issues before deploying to a live server.
- Use schema templates to avoid common formatting mistakes.

---

## 📚 Related Pages

- [Datapack Support](Datapack-Support)
- [Diagnostics & Tracing](Diagnostics-and-Tracing)
- [Commands Reference](Commands-Reference)
