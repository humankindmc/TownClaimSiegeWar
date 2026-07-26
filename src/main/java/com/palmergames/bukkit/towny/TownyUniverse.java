package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnPoint;
import com.palmergames.bukkit.towny.object.SpawnPointLocation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.util.Trie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory Towny object registry used by the TownClaim compatibility layer. */
public final class TownyUniverse {
    private static final TownyUniverse INSTANCE = new TownyUniverse();

    private final Map<UUID, Resident> residentsById = new ConcurrentHashMap<>();
    private final Map<String, Resident> residentsByName = new ConcurrentHashMap<>();
    private final Map<UUID, Town> townsById = new ConcurrentHashMap<>();
    private final Map<String, Town> townsByName = new ConcurrentHashMap<>();
    private final Map<UUID, Nation> nationsById = new ConcurrentHashMap<>();
    private final Map<String, Nation> nationsByName = new ConcurrentHashMap<>();
    private final Map<UUID, TownyWorld> worldsById = new ConcurrentHashMap<>();
    private final Map<String, TownyWorld> worldsByName = new ConcurrentHashMap<>();
    private final Map<WorldCoord, TownBlock> townBlocks = new ConcurrentHashMap<>();
    private final Map<SpawnPointLocation, SpawnPoint> spawnPoints = new ConcurrentHashMap<>();
    private final Trie residentsTrie = new Trie();
    private final Trie townsTrie = new Trie();
    private final Trie nationsTrie = new Trie();
    private TownyPermissionSource permissionSource;

    private TownyUniverse() {
    }

    public static TownyUniverse getInstance() {
        return INSTANCE;
    }

    public TownyPermissionSource getPermissionSource() {
        return permissionSource;
    }

    public void setPermissionSource(TownyPermissionSource permissionSource) {
        this.permissionSource = permissionSource;
    }

    public boolean hasResident(String name) { return residentsByName.containsKey(key(name)); }
    public boolean hasResident(UUID id) { return residentsById.containsKey(id); }
    public Resident getResident(String name) { return residentsByName.get(key(name)); }
    public Resident getResident(UUID id) { return residentsById.get(id); }
    public Collection<Resident> getResidents() { return List.copyOf(residentsById.values()); }
    public Trie getResidentsTrie() { return residentsTrie; }

    public void registerResident(Resident resident) throws AlreadyRegisteredException {
        if (hasResident(resident.getUUID()) || hasResident(resident.getName())) throw new AlreadyRegisteredException();
        residentsById.put(resident.getUUID(), resident);
        residentsByName.put(key(resident.getName()), resident);
        residentsTrie.addKey(resident.getName());
    }

    public void unregisterResident(Resident resident) throws NotRegisteredException {
        if (residentsById.remove(resident.getUUID()) == null) throw new NotRegisteredException();
        residentsByName.remove(key(resident.getName()));
        residentsTrie.removeKey(resident.getName());
    }

    public boolean hasTown(String name) { return townsByName.containsKey(key(name)); }
    public boolean hasTown(UUID id) { return townsById.containsKey(id); }
    public Town getTown(String name) { return townsByName.get(key(name)); }
    public Town getTown(UUID id) { return townsById.get(id); }
    public Collection<Town> getTowns() { return List.copyOf(townsById.values()); }
    public Trie getTownsTrie() { return townsTrie; }

    public void registerTown(Town town) throws AlreadyRegisteredException {
        if (hasTown(town.getUUID()) || hasTown(town.getName())) throw new AlreadyRegisteredException();
        townsById.put(town.getUUID(), town);
        townsByName.put(key(town.getName()), town);
        townsTrie.addKey(town.getName());
    }

    public void unregisterTown(Town town) throws NotRegisteredException {
        if (townsById.remove(town.getUUID()) == null) throw new NotRegisteredException();
        townsByName.remove(key(town.getName()));
        townsTrie.removeKey(town.getName());
    }

    public boolean hasNation(String name) { return nationsByName.containsKey(key(name)); }
    public boolean hasNation(UUID id) { return nationsById.containsKey(id); }
    public Nation getNation(String name) { return nationsByName.get(key(name)); }
    public Nation getNation(UUID id) { return nationsById.get(id); }
    public Collection<Nation> getNations() { return List.copyOf(nationsById.values()); }
    public Trie getNationsTrie() { return nationsTrie; }

    public void registerNation(Nation nation) throws AlreadyRegisteredException {
        if (hasNation(nation.getUUID()) || hasNation(nation.getName())) throw new AlreadyRegisteredException();
        nationsById.put(nation.getUUID(), nation);
        nationsByName.put(key(nation.getName()), nation);
        nationsTrie.addKey(nation.getName());
    }

    public void unregisterNation(Nation nation) throws NotRegisteredException {
        if (nationsById.remove(nation.getUUID()) == null) throw new NotRegisteredException();
        nationsByName.remove(key(nation.getName()));
        nationsTrie.removeKey(nation.getName());
    }

    public void registerTownyWorld(TownyWorld world) {
        worldsById.put(world.getUUID(), world);
        worldsByName.put(key(world.getName()), world);
    }

    public TownyWorld getWorld(UUID id) { return worldsById.get(id); }
    public TownyWorld getWorld(String name) { return worldsByName.get(key(name)); }
    public boolean hasTownyWorld(String name) { return worldsByName.containsKey(key(name)); }
    public List<TownyWorld> getTownyWorlds() { return List.copyOf(worldsById.values()); }
    public Map<String, TownyWorld> getWorldMap() { return worldsByName; }

    public TownBlock getTownBlock(WorldCoord coord) throws NotRegisteredException {
        TownBlock block = townBlocks.get(coord);
        if (block == null) throw new NotRegisteredException();
        return block;
    }

    public TownBlock getTownBlockOrNull(WorldCoord coord) { return townBlocks.get(coord); }
    public Map<WorldCoord, TownBlock> getTownBlocks() { return townBlocks; }
    public boolean hasTownBlock(WorldCoord coord) { return townBlocks.containsKey(coord); }
    public void addTownBlock(TownBlock block) { townBlocks.put(block.getWorldCoord(), block); }

    public void removeTownBlock(TownBlock block) {
        townBlocks.remove(block.getWorldCoord());
        if (block.hasTown()) block.getTownOrNull().removeTownBlock(block);
        if (block.hasResident()) block.getResidentOrNull().removeTownBlock(block);
    }

    public void removeTownBlocks(List<TownBlock> blocks) {
        new ArrayList<>(blocks).forEach(this::removeTownBlock);
    }

    public void addSpawnPoint(SpawnPoint point) { spawnPoints.put(point.getSpawnPointLocation(), point); }
    public void removeSpawnPoint(SpawnPointLocation location) { spawnPoints.remove(location); }
    public List<Resident> getJailedResidentMap() { return List.of(); }

    private static String key(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
