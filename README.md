HD Skins (Offline Mode)
========

![License](https://img.shields.io/github/license/MineLittlePony/HDSkins)
![](https://img.shields.io/badge/api-fabric-orange.svg)

The Minecraft HDSkins mod from the VoxelModPack.

This is an offline-mode modification which doesn't require connection to a third-party skin server. I made it for local co-op purposes, but you might find it otherwise useful.

Skins are stored in the user/home folder (i.e. `C:\Users\your username\hdskins_offline.json`). This path is currently hardcoded.

## Building

1. Some JDK version way greater than 8 is required. The official documentation is outdated. Install it using https://adoptopenjdk.net/

2. Open a terminal window in the same directory as the sources (git clone or extracted from zip). Run the following command (windows).

```
gradlew build
```

3. After some time, the built mod will be in `/build/libs`.

By the way, I did not have luck with the official build.gradle. I had to comment out half of it to make it work.
