# VPB Fresh Player Animations Compat v0.1.2

Hotfix release.

## Fixed

- Fresh installs now enable the intended aim-only compatibility behavior by default.
- Debug overlay remains disabled by default.
- Debug logging remains disabled by default.
- Pose tracing remains disabled by default.

## Default behavior

- Normal third-person Fresh Player Animations behavior while simply holding a gun.
- Compatibility arm/gun lock only while aiming.
- Problematic sleeve layers are hidden during the active aim lock.
- Held gun alignment while aiming.
- No first-person rendering changes.
- No gameplay, damage, projectile, recipe, sound, or networking changes.

## Why this release exists

v0.1.1 disabled debug defaults correctly, but a fresh install without a config still used `armMode=off`, meaning the mod loaded but did nothing. v0.1.2 fixes that.
