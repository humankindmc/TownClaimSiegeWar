package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.playeractions.DestroyBlock;
import com.gmail.goosius.siegewar.playeractions.PlaceBlock;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class TownClaimActionListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!SiegeWarSettings.getWarSiegeEnabled()) {
            return;
        }
        Block block = event.getBlockPlaced();
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(block.getLocation());
        TownyBuildEvent action = new TownyBuildEvent(event.getPlayer(), block.getLocation(), block.getType(), block,
                townBlock, townBlock == null);
        PlaceBlock.evaluateSiegeWarPlaceBlockRequest(event.getPlayer(), block, action);
        copyCancellation(action, event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!SiegeWarSettings.getWarSiegeEnabled()) {
            return;
        }
        Block block = event.getBlock();
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(block.getLocation());
        TownyDestroyEvent action = new TownyDestroyEvent(event.getPlayer(), block.getLocation(), block.getType(), block,
                townBlock, townBlock == null);
        try {
            DestroyBlock.evaluateSiegeWarDestroyBlockRequest(action);
        } catch (TownyException exception) {
            action.setCancelled(true);
            action.setCancelMessage(exception.getMessage(event.getPlayer()));
        }
        copyCancellation(action, event.getPlayer(), event);
    }

    private void copyCancellation(CancellableTownyEvent source, Player player, org.bukkit.event.Cancellable target) {
        if (!source.isCancelled()) {
            return;
        }
        target.setCancelled(true);
        if (source.getCancelMessage() != null && !source.getCancelMessage().isBlank()) {
            Messaging.sendErrorMsg(player, source.getCancelMessage());
        }
    }
}
