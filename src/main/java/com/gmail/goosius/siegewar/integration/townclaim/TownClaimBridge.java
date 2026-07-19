package com.gmail.goosius.siegewar.integration.townclaim;

import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.settings.Settings;
import com.humankindmc.claims.ClaimsService;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TownClaimBridge {
    private TownClaimBridge() {
    }

    public static boolean load() {
        try {
            setStaticField(TownySettings.class, "config", Settings.getConfig());
            TownClaimMetadataStore.load(SiegeWar.getSiegeWar().getDataFolder());
            TownService towns = service(TownService.class);
            NationService nations = service(NationService.class);
            ClaimsService claims = service(ClaimsService.class);
            TownHeartRepository hearts = service(TownHeartRepository.class);
            TownyUniverse universe = TownyUniverse.getInstance();
            universe.setPermissionSource(new TownClaimPermissionSource());
            Map<UUID, TownClaimTown> townMap = loadTowns(universe, towns);
            loadResidents(universe, towns, townMap);
            loadClaims(universe, claims, hearts, townMap);
            loadNations(universe, nations, townMap);
            SiegeWar.info("TownClaim bridge loaded " + townMap.size() + " towns and " + universe.getNations().size() + " nations.");
            return true;
        } catch (Exception exception) {
            SiegeWar.severe("Could not load TownClaim data: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    public static void forceDisband(Town town) {
        TownService towns = service(TownService.class);
        if (!towns.forceDisband(town.getUUID()).success()) {
            throw new IllegalStateException("TownClaim could not disband " + town.getName());
        }
        try {
            TownyUniverse.getInstance().unregisterTown(town);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not remove " + town.getName() + " from the SiegeWar bridge", exception);
        }
    }

    private static Map<UUID, TownClaimTown> loadTowns(TownyUniverse universe, TownService service) throws Exception {
        Map<UUID, TownClaimTown> result = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            TownyWorld townyWorld = new TownyWorld(world.getName(), world.getUID());
            townyWorld.setUsingTowny(true);
            townyWorld.setWarAllowed(true);
            universe.registerTownyWorld(townyWorld);
        }
        for (com.humankindmc.claims.town.Town source : service.getTowns()) {
            TownClaimTown town = new TownClaimTown(source.name(), source.id());
            universe.registerTown(town);
            result.put(source.id(), town);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void loadResidents(TownyUniverse universe, TownService service, Map<UUID, TownClaimTown> towns) throws Exception {
        for (TownClaimTown town : towns.values()) {
            for (TownMember member : service.getMembers(town.getUUID())) {
                TownClaimResident resident = new TownClaimResident(member.playerName(), member.playerId());
                setField(resident, "town", town);
                ((List<Resident>) field(town, "residents").get(town)).add(resident);
                if (member.playerId().equals(service.getTown(town.getUUID()).orElseThrow().mayorId())) {
                    setField(town, "mayor", resident);
                }
                universe.registerResident(resident);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadClaims(TownyUniverse universe, ClaimsService service, TownHeartRepository hearts,
                                   Map<UUID, TownClaimTown> towns) throws Exception {
        for (TownClaimTown town : towns.values()) {
            TownBlock first = null;
            for (Claim claim : service.getClaimsForTown(town.getUUID())) {
                for (int x = claim.region().minChunkX(); x <= claim.region().maxChunkX(); x++) {
                    for (int z = claim.region().minChunkZ(); z <= claim.region().maxChunkZ(); z++) {
                        TownBlock block = new TownBlock(new WorldCoord(claim.worldName(), x, z));
                        setField(block, "town", town);
                        ((Map<WorldCoord, TownBlock>) field(town, "townBlocks").get(town)).put(block.getWorldCoord(), block);
                        universe.addTownBlock(block);
                        first = first == null ? block : first;
                    }
                }
            }
            TownHeartLocation heart = hearts.findByTown(town.getUUID()).orElse(null);
            TownBlock home = heart == null ? first : universe.getTownBlockOrNull(
                    new WorldCoord(heart.worldName(), Math.floorDiv(heart.x(), 16), Math.floorDiv(heart.z(), 16)));
            if (home != null) {
                setField(town, "homeBlock", home);
                TownyWorld world = home.getWorld();
                setField(town, "world", world);
                world.addTown(town);
                if (heart != null && world.getBukkitWorld() != null) {
                    town.setSpawn(new Location(world.getBukkitWorld(), heart.x() + 0.5, heart.y(), heart.z() + 0.5));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadNations(TownyUniverse universe, NationService service,
                                    Map<UUID, TownClaimTown> towns) throws Exception {
        for (com.humankindmc.claims.nation.Nation source : service.nations()) {
            TownClaimNation nation = new TownClaimNation(source.name(), source.id());
            for (UUID townId : service.memberTownIds(source.id())) {
                Town town = towns.get(townId);
                if (town != null) {
                    ((List<Town>) field(nation, "towns").get(nation)).add(town);
                    setField(town, "nation", nation);
                }
            }
            setField(nation, "capital", towns.get(source.capitalTownId()));
            universe.registerNation(nation);
        }
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
