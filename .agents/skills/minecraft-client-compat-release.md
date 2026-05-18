# minecraft-client-compat-release

Use this repo-local skill when building, validating, testing, and preparing release handoff for the Fabric client compatibility mod.

## Build Command Detection

Prefer the Gradle wrapper if present:

```powershell
.\gradlew build
```

If no wrapper exists during project bootstrap, use the repository-approved Gradle setup and add a wrapper as part of implementation.

For this project, the target loader is Fabric on Minecraft `1.21.11`.

## Jar Validation

After build:

```powershell
Get-ChildItem "build\libs" -Filter "*.jar" | Sort-Object LastWriteTime -Descending
jar tf "build\libs\<built-jar>.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|com/joao2/vpbfpa"
```

Check:

- Filename and version are expected.
- `fabric.mod.json` exists.
- Client environment is declared.
- Milestone 1 has no mixin metadata. Mixin config exists only after mixins are implemented in a later milestone.
- No VPB, Fresh, EMF, ETF, or Blue Archive assets are bundled.

## GoodCraft Test Deployment

Only copy into GoodCraft after build success and jar validation:

```powershell
$Profile = "C:\Users\joao2\AppData\Roaming\ModrinthApp\profiles\Good_Craft test version"
Get-ChildItem "$Profile\mods" -Filter "vpb-fpa-compat-*.jar"
Copy-Item "build\libs\<built-jar>.jar" "$Profile\mods\"
```

If replacing older test jars, move or remove only prior `vpb-fpa-compat-*.jar` files and document exact filenames.

## Release Checklist

Before release handoff:

- README explains purpose, supported environment, config, limitations, and no-asset-redistribution policy.
- CHANGELOG is updated.
- `docs/test-matrix.md` includes test coverage and results.
- `docs/release-checklist.md` exists.
- Rollback instructions are documented.
- The mod remains client-side only unless a server-side requirement is proven and documented.

## Safety Rules

- Do not edit GoodCraft configs.
- Do not edit jars/zips/resource packs/content packs.
- Do not redistribute third-party assets.
- Keep test deployment reversible.
