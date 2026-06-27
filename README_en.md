# Spatial Anchor

An add-on mod for Create Aeronautics (Simulated) that **completely locks** an airship at the coordinates where it was activated.

This mod uses AI.
---

## Overview

When supplied with stress (rotation), this block holds an airship firmly in place. It locks not only position but **orientation as well**, so the ship stays at a fixed point in the air without drifting from wind or momentum.

Useful for stationary airship docking, fixed sky fortresses, and similar setups.

---

## Requirements

### Supported Version
- **Minecraft 1.21.1**
- **NeoForge**

### Required Mods (all mandatory)
| Mod | Version |
|-----|---------|
| Create | 6.0.10-280 or later |
| Sable | 2.0.3 or later |
| Create Aeronautics (Simulated) | 1.2.1 or later |

> Create's own dependencies such as Flywheel are also required.

---

## Multiplayer Setup

### Both Server and Clients
1. Install all required mods listed above
2. Place `spatialanchor-x.x.x.jar` into the `mods/` folder

### Important Notes
- **The mod set and versions must match exactly between the server and all clients.** Mismatched versions will prevent connection.
- Anchor settings are managed server-side (`config/spatialanchor-server.toml`). No client-side configuration is required.

---

## How to Use

1. **Craft**: Make it in a Mechanical Crafter (same 5x5 layout as the Crushing Wheel)
2. **Place**: Put the anchor block on an assembled airship
3. **Activate**: Supply rotation to the anchor
   - Requires at least **64 RPM**
   - Requires **stress (Su)** proportional to the ship's weight
4. Once active, the ship is locked completely in place
5. **Release**: Stop the rotation, or the anchor releases automatically when stress is insufficient

### Status Check
- **Right-click** the anchor: shows status, locked coordinates, ship weight, and required stress
- **Hold Shift** while hovering the item: shows detailed description

---

## About Required Stress

Required stress is **proportional to the ship's weight**. Heavier ships need more stress.

- A high-output stress source (e.g. a Creative Motor) can anchor even heavy ships at 64 RPM
- A weak stress source (e.g. only a Water Wheel) will fail to start on heavy ships due to insufficient stress
- Increasing the rotation speed (64-256 RPM) does **not** change stress consumption

---

## Configuration (config/spatialanchor-server.toml)

| Option | Description | Default |
|--------|-------------|---------|
| `weightMultiplier` | Multiplier for required stress | 1.0 |
| `stressPerWeightUnit` | Stress per unit of weight | 4.0 |
| `minSpeed` | Minimum rotation speed to activate | 64 |
| `maxSpeed` | Speed at which stress consumption caps | 256 |

Example: Setting `weightMultiplier = 2.0` doubles the required stress, increasing difficulty.

---

## Known Limitations
- Display Link integration is not yet supported (planned for a future update)
