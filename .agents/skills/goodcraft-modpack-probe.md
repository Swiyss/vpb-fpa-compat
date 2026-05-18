# goodcraft-modpack-probe

Use this repo-local skill when safely inspecting the GoodCraft Modrinth profile for loader, Minecraft version, installed mods, resource packs, configs, logs, and Point Blank content packs.

## Profile Path

```powershell
$Profile = "C:\Users\joao2\AppData\Roaming\ModrinthApp\profiles\Good_Craft test version"
```

Always quote paths because the profile path contains spaces.

## Safe Inventory Commands

```powershell
Get-ChildItem "$Profile"
Get-ChildItem "$Profile\mods" | Sort-Object Name
Get-ChildItem "$Profile\resourcepacks" | Sort-Object Name
Get-ChildItem "$Profile\config" | Sort-Object Name
Get-ChildItem "$Profile\pointblank" | Sort-Object Name
```

## Metadata And Logs

```powershell
Get-Content "$Profile\modrinth.index.json" -Raw
Get-Content "$Profile\profile.json" -Raw
Get-Content "$Profile\logs\latest.log" -TotalCount 200
Select-String -Path "$Profile\logs\latest.log" -Pattern "Loading Minecraft|Fabric Loader|NeoForge|pointblank|entity_model_features|entity_texture_features|Fresh|PlayerAnimator|GeckoLib|Iris|Sodium"
```

If `modrinth.index.json` or `profile.json` is absent, record that and use logs plus installed filenames as evidence.

## Jar And Zip Inspection

```powershell
jar tf "path\to\some.jar" | Select-String -Pattern "fabric.mod.json|neoforge.mods.toml|mods.toml|pointblank|playeranimator|emf|etf"
jar tf "path\to\some.zip" | Select-String -Pattern "player|jem|jpm|animation|pointblank|blue"
```

Useful Point Blank and Fresh checks:

```powershell
jar tf "$Profile\mods\pointblank-fabric-1.21.11-2.0.1.jar" | Select-String -Pattern "fabric.mod.json|mixins|GunItem|player|animation"
jar tf "$Profile\resourcepacks\FA+Player-v1.0.zip" | Select-String -Pattern "player\.jem|player_slim\.jem|jpm|right_arm|left_arm|sleeve"
jar tf "$Profile\pointblank\bluearchive-ext v0.6.zip" | Select-String -Pattern "ext.json|pack.mcmeta|assets/pointblank/items|ba_"
```

## Rules

- Do not edit jars or zips in place.
- Do not delete modpack files.
- Do not edit GoodCraft configs.
- Copy files into the repo for analysis only if needed.
- Keep exact filenames and versions in findings.
- Do not redistribute copied third-party assets.
