# HUD & Display

MarieLib provides framework hooks for on-screen value bar rendering. Consuming mods implement the actual HUD appearance.

---

## 📊 Value Bars

Consuming mods register value keys with colors and display names. MarieLib tracks levels and exposes them to client renderers.

Each value bar shows a normalized level from `0.0` to `1.0`.

---

## 🎨 Custom Rendering

Mods can attach a custom `ValueRenderer` to a `ValueDefinition`:

```java
ValueDefinition custom = ValueDefinition.builder("energy")
    .displayName("Energy")
    .color(0xFF00FF00)
    .customRenderer(myRenderer)
    .build();
```

`ValueRenderer` receives `GuiGraphics`, position, and current level for custom draw logic.

---

## ⚙️ Module Toggles

HUD display is controlled by the `enableHud` module flag in `ModuleCache`. Consuming mods expose this in their config screen.

Additional HUD settings (position, opacity, hide thresholds) are owned by the consuming mod. For Nourished's nutrition HUD, see the [Nourished HUD & Display page](https://github.com/kgbcupcake/nourished/wiki/HUD-and-Display).

---

## 🔔 Toasts

MarieLib provides a toast manager for critical value notifications. Enabled via the `enableToasts` module flag.

---

## 📚 Related Pages

- [API Reference](API-Reference)
- [Configuration](Configuration)
- [Getting Started](Getting-Started)
