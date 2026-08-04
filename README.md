StorageDrawers
==============

A mod adding compartmental storage for Minecraft Fabric (unofficial port)

Versions
========

Each Minecraft version lives on its own branch. Releases are tagged `v<minecraft>-<mod version>`.

| Minecraft | Branch | Latest release |
|---|---|---|
| 26.2 | [port/26.2](https://github.com/chaevsfe/StorageDrawers/tree/port/26.2) | [v26.2-19.1.3](https://github.com/chaevsfe/StorageDrawers/releases/tag/v26.2-19.1.3) |
| 26.1 | [port/26.1](https://github.com/chaevsfe/StorageDrawers/tree/port/26.1) | [v26.1-19.1.3](https://github.com/chaevsfe/StorageDrawers/releases/tag/v26.1-19.1.3) |

Also on [Modrinth](https://modrinth.com/mod/storagedrawers-unofficial-fabric-port).

Before updating
===============

Enchanted and NBT-bearing may be deleted or reverted. Create a backup, if any problems: Empty modded containers of items into vanilla storage or your inventory before updating and report issues (https://github.com/chaevsfe/StorageDrawers/issues).

For Players
-----------

All credit goes to the original author.
- [Minecraft Forums](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2198533-storage-drawers-v1-10-7-v3-5-0-v4-0-0-updated-nov)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/storage-drawers)
- [Github Releases](https://github.com/jaquadro/StorageDrawers/releases)

There's also a discord community for Texel's mods: https://discord.gg/8WtpQfy

For Developers
--------------

#### Building

StorageDrawers is built using `gradle`. These commands should be enough to get you started:

```
git clone https://github.com/chaevsfe/StorageDrawers
cd StorageDrawers
git checkout port/26.1   # or port/26.2 -- pick the branch for your Minecraft version
./gradlew :fabric:build
```

Reporting Bugs
--------------

When reporting bugs, always include the version number of the mod.  If you're reporting a crash, include your client or server log depending on where the crash ocurred.
