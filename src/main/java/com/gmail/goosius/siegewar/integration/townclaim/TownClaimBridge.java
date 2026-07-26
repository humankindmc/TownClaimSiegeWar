package com.gmail.goosius.siegewar.integration.townclaim;

import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.enums.SiegeRemoveReason;
import com.gmail.goosius.siegewar.metadata.TownMetaDataController;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.Settings;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.gmail.goosius.siegewar.utils.SiegeWarTownPeacefulnessUtil;
import com.humankindmc.claims.ClaimManager;
import com.humankindmc.claims.ClaimsService;
import com.humankindmc.claims.integration.TownOperationGuard;
import com.humankindmc.claims.model.Claim;
import com.humankindmc.claims.nation.NationService;
import com.humankindmc.claims.town.TownMember;
import com.humankindmc.claims.town.TownService;
import com.humankindmc.claims.townheart.TownHeartLocation;
import com.humankindmc.claims.townheart.storage.TownHeartRepository;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TownClaimBridge {
    private static final Map<UUID, TownClaimTown> TOWNS = new HashMap<>();
    private static final Map<UUID, TownClaimResident> RESIDENTS = new HashMap<>();
    private static final Map<UUID, TownClaimNation> NATIONS = new HashMap<>();
    private static final Map<UUID, TownClaimTerritory> TERRITORIES = new HashMap<>();
    private static final Map<WorldCoord, TownBlock> TOWN_BLOCKS = new HashMap<>();

    private static TownyUniverse universe;
    private static TownService townService;
    private static NationService nationService;
    private static ClaimsService claimsService;
    private static TownHeartRepository heartRepository;
    private static boolean loaded;
    private static boolean synchronizing;

    private TownClaimBridge() {
    }

    public static boolean load() {
        try {
            configureTownySettings();
            TownClaimMetadataStore.load(SiegeWar.getSiegeWar().getDataFolder());
            townService = service(TownService.class);
            nationService = service(NationService.class);
            claimsService = service(ClaimsService.class);
            heartRepository = service(TownHeartRepository.class);
            TownOperationGuard operationGuard = new TownClaimOperationGuard();
            townService.setOperationGuard(operationGuard);
            ((ClaimManager) claimsService).setOperationGuard(operationGuard);
            nationService.setOperationGuard(operationGuard);
            universe = TownyUniverse.getInstance();
            universe.setPermissionSource(new TownClaimPermissionSource());
            loaded = true;
            synchronizeNow();
            SiegeWar.info("TownClaim bridge loaded " + TOWNS.size() + " towns and " + NATIONS.size() + " nations.");
            return true;
        } catch (Exception exception) {
            unload();
            SiegeWar.severe("Could not load TownClaim data: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    public static void configureTownySettings() {
        try {
            setStaticField(TownySettings.class, "config", Settings.getConfig());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not configure the Towny compatibility classes", exception);
        }
    }

    /** Refreshes compatibility objects in place so active sieges keep stable town and nation identities. */
    public static void synchronize() {
        if (!loaded || synchronizing) {
            return;
        }
        try {
            synchronizeNow();
        } catch (Exception exception) {
            SiegeWar.severe("Could not synchronize TownClaim data: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public static void forceDisband(Town town) {
        if (!townService.forceDisband(town.getUUID()).success()) {
            throw new IllegalStateException("TownClaim could not disband " + town.getName());
        }
        synchronize();
    }

    public static void unload() {
        if (townService != null) {
            townService.setOperationGuard(null);
        }
        if (claimsService instanceof ClaimManager manager) {
            manager.setOperationGuard(null);
        }
        if (nationService != null) {
            nationService.setOperationGuard(null);
        }
        loaded = false;
    }

    static Town town(UUID townId) {
        Town town = TOWNS.get(townId);
        if (town == null) {
            synchronize();
            town = TOWNS.get(townId);
        }
        return town;
    }

    public static boolean areAtWar(UUID firstNationId, UUID secondNationId) {
        return nationService != null && nationService.areAtWar(firstNationId, secondNationId);
    }

    public static boolean isTerritory(Town town) {
        return town instanceof TownClaimTerritory;
    }

    public static boolean captureTerritory(Siege siege, Nation occupier) {
        return captureTerritory(siege.getTown(), occupier);
    }

    public static boolean captureTerritory(Town town, Nation occupier) {
        if (!(town instanceof TownClaimTerritory territory)) {
            return false;
        }
        com.humankindmc.claims.nation.Nation targetNation = nationService.nation(occupier.getUUID())
                .orElseThrow(() -> new IllegalStateException("TownClaim nation not found: " + occupier.getUUID()));
        if (claimsService.transferConnectedClaimsToNation(territory.anchorClaimId(), targetNation) == 0) {
            throw new IllegalStateException("TownClaim territory no longer exists: " + territory.getName());
        }
        synchronize();
        return true;
    }

    static Town siegeTarget(Claim claim) {
        if (claim == null) {
            return null;
        }
        TownBlock block = TOWN_BLOCKS.get(new WorldCoord(claim.worldName(), claim.region().minChunkX(),
                claim.region().minChunkZ()));
        return block == null ? null : block.getTownOrNull();
    }

    public static void occupy(Town town, Nation occupier) {
        try {
            moveTownToNation(town.getUUID(), occupier.getUUID());
            setNation(town, occupier);
            town.setConquered(true);
            TownMetaDataController.setOccupyingNationUUID(town, occupier);
            town.save();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not occupy " + town.getName() + " for " + occupier.getName(), exception);
        }
    }

    public static void clearOccupation(Town town) {
        try {
            moveTownToNation(town.getUUID(), null);
            setNation(town, null);
            town.setConquered(false);
            TownMetaDataController.removeOccupyingNationUUID(town);
            town.save();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not clear the occupation of " + town.getName(), exception);
        }
    }

    private static void synchronizeNow() throws Exception {
        synchronizing = true;
        try {
            syncWorlds();
            syncTowns();
            syncResidents();
            syncNations();
            syncClaims();
            removeSiegesAtPeace();
        } finally {
            synchronizing = false;
        }
    }

    private static void syncWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (universe.hasTownyWorld(world.getName())) {
                continue;
            }
            TownyWorld townyWorld = new TownyWorld(world.getName(), world.getUID());
            townyWorld.setUsingTowny(true);
            townyWorld.setWarAllowed(true);
            universe.registerTownyWorld(townyWorld);
        }
    }

    private static void syncTowns() throws Exception {
        Map<UUID, com.humankindmc.claims.town.Town> sources = townService.getTowns().stream()
                .collect(Collectors.toMap(com.humankindmc.claims.town.Town::id, Function.identity()));
        for (UUID townId : Set.copyOf(TOWNS.keySet())) {
            if (!sources.containsKey(townId)) {
                removeTown(TOWNS.remove(townId));
            }
        }
        for (com.humankindmc.claims.town.Town source : sources.values()) {
            TownClaimTown town = TOWNS.get(source.id());
            if (town == null) {
                town = new TownClaimTown(source.name(), source.id());
                universe.registerTown(town);
                TOWNS.put(source.id(), town);
                initializeTown(source, town);
            } else if (!town.getName().equals(source.name())) {
                renameTown(town, source.name());
            }
        }
    }

    private static void initializeTown(com.humankindmc.claims.town.Town source, Town town) {
        if (!town.hasMeta("siegewar_siegeImmunityEndTime")) {
            long immunityMillis = (long) (SiegeWarSettings.getSiegeImmunityNewTownsHours() * 60 * 60 * 1000);
            TownMetaDataController.setSiegeImmunityEndTime(town, source.createdAt().toEpochMilli() + immunityMillis);
        }
        if (SiegeWarSettings.getWarCommonPeacefulTownsEnabled() && !town.hasMeta("siegewar_peaceSetting")) {
            boolean peaceful = SiegeWarSettings.getNewTownPeacefulness();
            SiegeWarTownPeacefulnessUtil.setTownPeacefulness(town, peaceful);
            SiegeWarTownPeacefulnessUtil.setDesiredTownPeacefulness(town, peaceful);
        }
        town.save();
    }

    @SuppressWarnings("unchecked")
    private static void syncResidents() throws Exception {
        Map<UUID, TownMember> sources = new LinkedHashMap<>();
        Map<UUID, List<TownMember>> memberships = new HashMap<>();
        for (UUID townId : TOWNS.keySet()) {
            List<TownMember> members = List.copyOf(townService.getMembers(townId));
            memberships.put(townId, members);
            members.forEach(member -> sources.put(member.playerId(), member));
        }

        for (UUID residentId : Set.copyOf(RESIDENTS.keySet())) {
            if (!sources.containsKey(residentId)) {
                TownClaimResident resident = RESIDENTS.remove(residentId);
                setField(resident, "town", null);
                universe.unregisterResident(resident);
            }
        }
        for (TownMember source : sources.values()) {
            TownClaimResident resident = RESIDENTS.get(source.playerId());
            if (resident == null) {
                resident = new TownClaimResident(source.playerName(), source.playerId());
                universe.registerResident(resident);
                RESIDENTS.put(source.playerId(), resident);
            } else if (!resident.getName().equals(source.playerName())) {
                renameResident(resident, source.playerName());
            }
            setField(resident, "town", null);
        }

        for (TownClaimTown town : TOWNS.values()) {
            ((List<Resident>) field(town, "residents").get(town)).clear();
            setField(town, "mayor", null);
            UUID mayorId = townService.getTown(town.getUUID()).orElseThrow().mayorId();
            for (TownMember member : memberships.getOrDefault(town.getUUID(), List.of())) {
                TownClaimResident resident = RESIDENTS.get(member.playerId());
                ((List<Resident>) field(town, "residents").get(town)).add(resident);
                if (townService.getTownForPlayer(member.playerId())
                        .map(com.humankindmc.claims.town.Town::id).filter(town.getUUID()::equals).isPresent()) {
                    setField(resident, "town", town);
                }
                if (member.playerId().equals(mayorId)) {
                    setField(town, "mayor", resident);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void syncClaims() throws Exception {
        Map<WorldCoord, TownClaimTown> expected = new LinkedHashMap<>();
        Set<UUID> expectedTerritories = new java.util.HashSet<>();
        for (TownClaimTown town : TOWNS.values()) {
            List<List<Claim>> groups = claimGroups(claimsService.getClaimsForTown(town.getUUID()));
            TownHeartLocation heart = heartRepository.findByTown(town.getUUID()).orElse(null);
            Claim heartClaim = heart == null ? null : claimsService
                    .getClaimAtBlock(heart.worldName(), heart.x(), heart.z()).orElse(null);
            List<Claim> mainGroup = groups.stream()
                    .filter(group -> heartClaim != null && group.stream().anyMatch(claim -> claim.id().equals(heartClaim.id())))
                    .findFirst()
                    .orElse(groups.isEmpty() ? List.of() : groups.getFirst());
            int outpostNumber = 0;
            for (List<Claim> group : groups) {
                TownClaimTown target = town;
                if (group != mainGroup) {
                    Claim anchor = anchor(group);
                    target = territory(anchor, town.getName() + " Outpost " + ++outpostNumber);
                    prepareTerritory((TownClaimTerritory) target, town, town.getNationOrNull(),
                            SiegeWarTownPeacefulnessUtil.isTownPeaceful(town));
                    expectedTerritories.add(target.getUUID());
                }
                mapClaims(expected, group, target);
            }
        }
        for (TownClaimNation nation : NATIONS.values()) {
            TownClaimTown capital = (TownClaimTown) nation.getCapital();
            int territoryNumber = 0;
            for (List<Claim> group : claimGroups(claimsService.getClaimsForNation(nation.getUUID()))) {
                Claim anchor = anchor(group);
                TownClaimTerritory target = territory(anchor,
                        nation.getName() + " Territory " + ++territoryNumber);
                prepareTerritory(target, capital, nation, false);
                expectedTerritories.add(target.getUUID());
                mapClaims(expected, group, target);
            }
        }

        for (UUID territoryId : Set.copyOf(TERRITORIES.keySet())) {
            if (!expectedTerritories.contains(territoryId)) {
                removeTown(TERRITORIES.remove(territoryId));
            }
        }

        for (WorldCoord coord : Set.copyOf(TOWN_BLOCKS.keySet())) {
            TownBlock block = TOWN_BLOCKS.get(coord);
            if (expected.get(coord) != block.getTownOrNull()) {
                universe.removeTownBlock(block);
                TOWN_BLOCKS.remove(coord);
            }
        }
        for (TownClaimTown town : allSiegeTargets()) {
            TownyWorld oldWorld = town.getHomeblockWorld();
            if (oldWorld != null) {
                try {
                    oldWorld.removeTown(town);
                } catch (Exception ignored) {
                }
            }
            ((Map<WorldCoord, TownBlock>) field(town, "townBlocks").get(town)).clear();
            setField(town, "homeBlock", null);
            setField(town, "world", null);
        }

        Map<UUID, TownBlock> firstBlocks = new HashMap<>();
        for (Map.Entry<WorldCoord, TownClaimTown> entry : expected.entrySet()) {
            TownBlock block = TOWN_BLOCKS.get(entry.getKey());
            if (block == null) {
                block = new TownBlock(entry.getKey());
                setField(block, "town", entry.getValue());
                universe.addTownBlock(block);
                TOWN_BLOCKS.put(entry.getKey(), block);
            }
            ((Map<WorldCoord, TownBlock>) field(entry.getValue(), "townBlocks").get(entry.getValue()))
                    .put(entry.getKey(), block);
            firstBlocks.putIfAbsent(entry.getValue().getUUID(), block);
        }

        for (TownClaimTown town : allSiegeTargets()) {
            TownHeartLocation heart = town instanceof TownClaimTerritory
                    ? null : heartRepository.findByTown(town.getUUID()).orElse(null);
            TownBlock home = heart == null ? firstBlocks.get(town.getUUID()) : universe.getTownBlockOrNull(
                    new WorldCoord(heart.worldName(), Math.floorDiv(heart.x(), 16), Math.floorDiv(heart.z(), 16)));
            if (home == null) {
                continue;
            }
            setField(town, "homeBlock", home);
            TownyWorld world = home.getWorld();
            setField(town, "world", world);
            world.addTown(town);
            if (heart != null && world.getBukkitWorld() != null) {
                town.setSpawn(new Location(world.getBukkitWorld(), heart.x() + 0.5, heart.y(), heart.z() + 0.5));
            } else if (world.getBukkitWorld() != null) {
                town.setSpawn(new Location(world.getBukkitWorld(), (home.getX() << 4) + 8.5,
                        world.getBukkitWorld().getMinHeight(), (home.getZ() << 4) + 8.5));
            }
        }
    }

    private static List<List<Claim>> claimGroups(Collection<Claim> claims) {
        Map<UUID, Claim> remaining = claims.stream()
                .collect(Collectors.toMap(Claim::id, Function.identity()));
        List<List<Claim>> groups = new ArrayList<>();
        while (!remaining.isEmpty()) {
            Claim first = remaining.values().iterator().next();
            List<Claim> group = List.copyOf(claimsService.getConnectedClaims(first));
            group.forEach(claim -> remaining.remove(claim.id()));
            groups.add(group);
        }
        groups.sort(Comparator.comparing((List<Claim> group) -> anchor(group).createdAt())
                .thenComparing(group -> anchor(group).id()));
        return groups;
    }

    private static Claim anchor(List<Claim> claims) {
        return claims.stream().min(Comparator.comparing(Claim::createdAt).thenComparing(Claim::id)).orElseThrow();
    }

    private static TownClaimTerritory territory(Claim anchor, String name) throws Exception {
        UUID id = territoryId(anchor.id());
        TownClaimTerritory territory = TERRITORIES.get(id);
        if (territory == null) {
            territory = new TownClaimTerritory(name, id, anchor.id());
            TownMetaDataController.setSiegeImmunityEndTime(territory, 0);
            universe.registerTown(territory);
            TERRITORIES.put(id, territory);
        } else if (!territory.getName().equals(name)) {
            renameTown(territory, name);
        }
        return territory;
    }

    static UUID territoryId(UUID anchorClaimId) {
        return UUID.nameUUIDFromBytes(("townclaim-territory:" + anchorClaimId)
                .getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static void prepareTerritory(TownClaimTerritory territory, TownClaimTown residentsFrom,
                                         Nation nation, boolean peaceful) throws Exception {
        setField(territory, "nation", nation);
        List<Resident> residents = (List<Resident>) field(territory, "residents").get(territory);
        residents.clear();
        if (residentsFrom != null) {
            residents.addAll((List<Resident>) field(residentsFrom, "residents").get(residentsFrom));
            setField(territory, "mayor", field(residentsFrom, "mayor").get(residentsFrom));
        } else {
            setField(territory, "mayor", null);
        }
        if (SiegeWarTownPeacefulnessUtil.isTownPeaceful(territory) != peaceful) {
            SiegeWarTownPeacefulnessUtil.setTownPeacefulness(territory, peaceful);
            SiegeWarTownPeacefulnessUtil.setDesiredTownPeacefulness(territory, peaceful);
        }
    }

    private static void mapClaims(Map<WorldCoord, TownClaimTown> expected, Collection<Claim> claims,
                                  TownClaimTown target) {
        for (Claim claim : claims) {
            for (int x = claim.region().minChunkX(); x <= claim.region().maxChunkX(); x++) {
                for (int z = claim.region().minChunkZ(); z <= claim.region().maxChunkZ(); z++) {
                    expected.put(new WorldCoord(claim.worldName(), x, z), target);
                }
            }
        }
    }

    private static Collection<TownClaimTown> allSiegeTargets() {
        List<TownClaimTown> targets = new ArrayList<>(TOWNS.values());
        targets.addAll(TERRITORIES.values());
        return targets;
    }

    @SuppressWarnings("unchecked")
    private static void syncNations() throws Exception {
        Map<UUID, com.humankindmc.claims.nation.Nation> sources = nationService.nations().stream()
                .collect(Collectors.toMap(com.humankindmc.claims.nation.Nation::id, Function.identity()));
        for (UUID nationId : Set.copyOf(NATIONS.keySet())) {
            if (!sources.containsKey(nationId)) {
                removeNation(NATIONS.remove(nationId));
            }
        }
        for (com.humankindmc.claims.nation.Nation source : sources.values()) {
            TownClaimNation nation = NATIONS.get(source.id());
            if (nation == null) {
                nation = new TownClaimNation(source.name(), source.id());
                universe.registerNation(nation);
                NATIONS.put(source.id(), nation);
            } else if (!nation.getName().equals(source.name())) {
                renameNation(nation, source.name());
            }
        }

        for (TownClaimTown town : TOWNS.values()) {
            setField(town, "nation", null);
        }
        for (TownClaimNation nation : NATIONS.values()) {
            ((List<Town>) field(nation, "towns").get(nation)).clear();
            setField(nation, "capital", null);
        }
        for (com.humankindmc.claims.nation.Nation source : sources.values()) {
            TownClaimNation nation = NATIONS.get(source.id());
            for (UUID townId : nationService.memberTownIds(source.id())) {
                TownClaimTown town = TOWNS.get(townId);
                if (town != null) {
                    ((List<Town>) field(nation, "towns").get(nation)).add(town);
                    setField(town, "nation", nation);
                }
            }
            setField(nation, "capital", TOWNS.get(source.capitalTownId()));
        }
        for (TownClaimTown town : TOWNS.values()) {
            boolean occupied = TownMetaDataController.hasOccupyingNationUUID(town);
            town.setConquered(occupied);
            if (!occupied) {
                continue;
            }
            TownClaimNation occupier = NATIONS.get(TownMetaDataController.getOccupyingNationUUID(town));
            if (occupier == null) {
                TownMetaDataController.removeOccupyingNationUUID(town);
                town.setConquered(false);
                town.save();
            }
        }
    }

    private static void removeSiegesAtPeace() {
        for (Siege siege : new ArrayList<>(SiegeController.getSieges())) {
            if (!siege.getStatus().isActive() || !(siege.getAttacker() instanceof Nation attacker)) {
                continue;
            }
            Nation defender = siege.getTown().getNationOrNull();
            if (defender != null && !areAtWar(attacker.getUUID(), defender.getUUID())) {
                SiegeController.removeSiege(siege, SiegeRemoveReason.PEACE);
            }
        }
    }

    private static void moveTownToNation(UUID townId, UUID targetNationId) {
        if (!nationService.forceMoveTown(townId, targetNationId).success()) {
            throw new IllegalStateException("TownClaim could not move town " + townId + " to nation " + targetNationId);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setNation(Town town, Nation nation) throws Exception {
        Nation oldNation = town.getNationOrNull();
        if (oldNation != null) {
            ((List<Town>) field(oldNation, "towns").get(oldNation)).remove(town);
        }
        setField(town, "nation", nation);
        if (nation != null) {
            List<Town> towns = (List<Town>) field(nation, "towns").get(nation);
            if (!towns.contains(town)) {
                towns.add(town);
            }
        }
    }

    private static void removeTown(TownClaimTown town) throws Exception {
        if (SiegeController.hasSiege(town.getUUID())) {
            SiegeController.removeSiege(SiegeController.getSiegeByTownUUID(town.getUUID()), SiegeRemoveReason.TOWN_DELETE);
        }
        for (WorldCoord coord : Set.copyOf(TOWN_BLOCKS.keySet())) {
            TownBlock block = TOWN_BLOCKS.get(coord);
            if (block.getTownOrNull() == town) {
                universe.removeTownBlock(block);
                TOWN_BLOCKS.remove(coord);
            }
        }
        if (town.hasWorld()) {
            try {
                town.getHomeblockWorld().removeTown(town);
            } catch (Exception ignored) {
            }
        }
        universe.unregisterTown(town);
    }

    private static void removeNation(TownClaimNation nation) throws Exception {
        for (Siege siege : new ArrayList<>(SiegeController.getSieges())) {
            if (nation.getUUID().equals(siege.getAttacker().getUUID())) {
                SiegeController.removeSiege(siege, SiegeRemoveReason.NATION_DELETE);
            }
        }
        universe.unregisterNation(nation);
    }

    private static void renameTown(TownClaimTown town, String name) throws Exception {
        universe.unregisterTown(town);
        town.setName(name);
        universe.registerTown(town);
    }

    private static void renameResident(TownClaimResident resident, String name) throws Exception {
        universe.unregisterResident(resident);
        resident.setName(name);
        universe.registerResident(resident);
    }

    private static void renameNation(TownClaimNation nation, String name) throws Exception {
        universe.unregisterNation(nation);
        nation.setName(name);
        universe.registerNation(nation);
    }

    private static <T> T service(Class<T> type) {
        RegisteredServiceProvider<T> registration = Bukkit.getServicesManager().getRegistration(type);
        if (registration == null) {
            throw new IllegalStateException(type.getSimpleName() + " is not registered by TownClaim");
        }
        return registration.getProvider();
    }

    private static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        Field field = field(type, name);
        field.set(null, value);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        field(target.getClass(), name).set(target, value);
    }

    private static Field field(Object target, String name) throws NoSuchFieldException {
        return field(target instanceof Class<?> type ? type : target.getClass(), name);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
}
