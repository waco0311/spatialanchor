<p align="center">
  <img src="docs/spatial_anchor_icon.png" width="160" alt="Spatial Anchor">
  <img src="docs/spatial_anchor_icon_active.png" width="160" alt="Spatial Anchor (active)">
</p>

# Spatial Anchor

[日本語](README.md)

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
- Look at the anchor with **Engineer's Goggles** to see:
  - Active / Inactive (reason: not on an airship, insufficient stress, too slow, standby)
  - Anchor position (while active)
  - Ship weight and required stress
  - Stress impact (Create's standard display)
- **Hold Shift** while hovering the item: shows detailed description

### Display Link Integration
Three Display Link sources are available (supports labels, Nixie Tubes and Flap Displays).

| Source | Output |
|--------|--------|
| Anchor Status | `online` / `offline` |
| Anchor Position | Current world position of the anchor `x y z` |
| Ship Weight | Weight of the ship |

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
| `weightRecalcInterval` | Ship weight recalculation interval (ticks) | 40 |

Example: Setting `weightMultiplier = 2.0` doubles the required stress, increasing difficulty.

---

## Changelog

### v0.2.0
- Fixed stress consumption not being applied to the kinetic network while active
- Fixed overstress flickering and activation at 0 su right after placement
- Status display moved from right-click (chat) to Engineer's Goggles
- Display Link split into three sources (Status / Position / Weight), added display names

### v0.1.0
- Initial release
