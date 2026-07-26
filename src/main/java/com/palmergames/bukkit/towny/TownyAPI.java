package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Towny-shaped read API backed by the TownClaim compatibility registry. */
public final class TownyAPI {
    private static final TownyAPI INSTANCE = new TownyAPI();
    private final TownyUniverse universe = TownyUniverse.getInstance();

    private TownyAPI() {
    }

    public static TownyAPI getInstance() { return INSTANCE; }
    public Nation getNation(String name) { return universe.getNation(name); }
    public Nation getNation(UUID id) { return universe.getNation(id); }
    public Resident getResident(String name) { return universe.getResident(name); }
    public Resident getResident(UUID id) { return universe.getResident(id); }
    public Resident getResident(Player player) { return player == null ? null : getResident(player.getUniqueId()); }
    public Town getTown(String name) { return universe.getTown(name); }
    public Town getTown(UUID id) { return universe.getTown(id); }
    public Town getTown(Location location) {
        TownBlock block = getTownBlock(location);
        return block == null ? null : block.getTownOrNull();
    }
    public Town getTown(Player player) { return player == null ? null : getResidentTownOrNull(getResident(player)); }
    public Nation getNation(Player player) { Town town = getTown(player); return town == null ? null : town.getNationOrNull(); }
    public Town getResidentTownOrNull(Resident resident) { return resident == null ? null : resident.getTownOrNull(); }
    public Nation getTownNationOrNull(Town town) { return town == null ? null : town.getNationOrNull(); }
    public List<Town> getTowns() { return List.copyOf(universe.getTowns()); }
    public List<Nation> getNations() { return List.copyOf(universe.getNations()); }
    public TownBlock getTownBlock(Location location) {
        return location == null ? null : universe.getTownBlockOrNull(WorldCoord.parseWorldCoord(location));
    }
    public TownBlock getTownBlock(Player player) { return player == null ? null : getTownBlock(player.getLocation()); }
    public TownBlock getTownBlock(WorldCoord coord) { return universe.getTownBlockOrNull(coord); }
    public Collection<TownBlock> getTownBlocks() { return List.copyOf(universe.getTownBlocks().values()); }
    public TownyWorld getTownyWorld(String name) { return universe.getWorld(name); }
    public TownyWorld getTownyWorld(UUID id) { return universe.getWorld(id); }
    public TownyWorld getTownyWorld(World world) { return world == null ? null : universe.getWorld(world.getUID()); }
    public boolean isTownyWorld(World world) { return getTownyWorld(world) != null; }
    public boolean isWilderness(Location location) { return getTownBlock(location) == null; }
    public boolean isWilderness(Block block) { return block == null || isWilderness(block.getLocation()); }
    public boolean isWilderness(WorldCoord coord) { return !universe.hasTownBlock(coord); }
    public Player getPlayer(Resident resident) { return resident == null ? null : resident.getPlayer(); }
    public List<Player> getOnlinePlayers(ResidentList residents) {
        return residents.getResidents().stream().map(Resident::getPlayer).filter(player -> player != null).toList();
    }
    public List<Player> getOnlinePlayersInTown(Town town) { return getOnlinePlayers(town); }
    public List<Player> getOnlinePlayersInNation(Nation nation) { return getOnlinePlayers(nation); }
    public List<Player> getOnlinePlayersAlliance(Nation nation) { return getOnlinePlayersInNation(nation); }
}
