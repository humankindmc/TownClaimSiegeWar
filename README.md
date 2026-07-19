# TownClaimSiegeWar
***TownClaimSiegeWar*** is the SiegeWar fork for [TownClaim](https://github.com/humankindmc/townclaim). It installs as its own `TownClaimSiegeWar.jar` and uses `plugins/TownClaimSiegeWar`, separate from TownClaim's jar and data folder.

## Build

Build the `integration/siegewar` branch of the sibling TownClaim checkout first, then package this repository with Java 25:

```powershell
cd ..\townclaim
.\gradlew.bat build
cd ..\TownClaimSiegeWar
mvn package
```

Install both `townclaim-1.0.0-SNAPSHOT.jar` and `TownClaimSiegeWar-0.1.0-SNAPSHOT.jar`. Towny is not installed separately; the small compatibility surface SiegeWar still uses is bundled into this fork.

TownClaim towns, selected memberships, claims, town hearts, and nations are mirrored into SiegeWar and refreshed while the server runs. Occupation changes are written back to TownClaim. Town officers/mayors receive town-level SiegeWar authority, and leaders of a nation's leader town receive nation-level authority; grant the normal `siegewar.*` Bukkit nodes to additional soldiers. Because TownClaim does not yet expose diplomacy, every foreign nation is treated as a valid opponent. Economy-only SiegeWar features remain disabled while TownClaim's treasury backend is the standalone no-op implementation.

### Features
* ⚔️ **Sieges:** Wars are conducted by means of sieges. A siege occurs when a nation attacks a town.
* 🤖 **Automatic:** Sieges are started by players and automatically managed by the plugin. Daily staff management of sieges is not required.
* 🚩 **Geopolitical:** Players can "work out" their in-game geopolitical ambitions e.g. a nation can, in-principle, capture all the non-capital towns on a server, and/or it can use sieges or the threat of sieges to extract in-game money from enemy towns and nations.
* ☔ **Town Protections:** Towns cannot be damaged or stolen-from during sieges. Also, non-capital towns can activate the *Peaceful* option, making them immune to sieges, but vulnerable to instant subversion (capture). Captured towns remain in the /nation and /alliance chat channels of their home nations, and do not get added to the chat channels of their occupying nations."
* ☔️ **Nation Protections:** Nation capitals cannot be captured, and if they do get sieged, they receive double the usual post-siege immunity duration. 
* 🕒 **Cross-Timezone Support:** Each siege consists of 7 hours fighting time, spread over the course of a weekend, which allows nations in different IRL-time-zones to compete against each other.
### Videos
* [Introduction](https://www.youtube.com/watch?v=0UU9-lVuHSY): The narrator explains some features of SiegeWar, and how the plugin fits into a geopolitical context (*nation building, diplomacy, and war*).
* [Siege on Nefarious, Earthpol](https://www.youtube.com/watch?v=raiAhk2Ru5Y), featuring players assembling an army, marching to a SiegeZone, and attacking a fort surrounding a Siege Banner.
* [Siege of Cerberus, CCNET](https://www.youtube.com/watch?v=EM--SfQYNQA
), featuring infantry and cavalry contesting a Siege Banner.
* [Siege of Vienna, Earthpol](https://www.youtube.com/watch?v=ccQW0S05si8), featuring infantry contesting a Siege Banner, including bombing by TNT-minecart, and fighting on a high platform over the banner.
* [Siege of Livland, CCNET](https://www.youtube.com/watch?v=LomXsdhzK1Y), featuring infantry, tanks, and aircraft(!), contesting a Siege Banner
### Links
* [Download](https://github.com/TownyAdvanced/SiegeWar/releases)
* [Installation Guide](https://github.com/TownyAdvanced/SiegeWar/wiki/Siege-War-Installation)
* [User Guide](https://github.com/TownyAdvanced/SiegeWar/wiki/Siege-War-User-Guide)
* [Maven/Gradle information](https://jitpack.io/#TownyAdvanced/SiegeWar)
### Credits:
- *SiegeWar* was initially developed by [Goosius1](https://github.com/Goosius1), starting in the summer of 2019.
- Originally *SiegeWar* was a fork of *Towny*, being built directly into *Towny*. 
- *SiegeWar* was later transformed by [LlmDl](https://github.com/LlmDl) & [Warriorrr](https://github.com/Warriorrrr) into the plugin which exists today. This work took about 2 months, and involved adding many many API events to *Towny*, and changing the entire *SiegeWar* database to use *Towny* metadata. The plugin was released in January 2021.
- Following the release of the plugin, many new features and improvements were added, with key contributions from [Goosius1](https://github.com/Goosius1), [LlmDl](https://github.com/LlmDl), and [Warriorrr](https://github.com/Warriorrrr). Multiple other developers have contributed also, notably [Ceeedric](https://github.com/ceeedric).
- Special thanks to the servers *CCNET*, *DatBlock*, and *EarthPol*, who pioneered the use of the system, and have provided much valuable information, feedback, and bug reports.
- [Goosius1](https://github.com/Goosius1) was the maintainer of this repo for the TownyAdvanced org, until his retirement from *Minecraft* plugin development in October 2021.
- *SiegeWar* is now maintained by the TownyAdvanced org.

