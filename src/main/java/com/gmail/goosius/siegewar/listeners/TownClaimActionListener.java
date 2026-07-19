package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.playeractions.DestroyBlock;
import com.gmail.goosius.siegewar.playeractions.PlaceBlock;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.humankindmc.claims.event.TownClaimBlockBreakEvent;
import com.humankindmc.claims.event.TownClaimBlockPlaceEvent;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class TownClaimActionListener implements Listener {
    @EventHandler
    public void onBlockPlace(TownClaimBlockPlaceEvent event) {
        if (!SiegeWarSettings.getWarSiegeEnabled()) {
            return;
        }
        Block block = event.block();
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(block.getLocation());
        TownyBuildEvent action = new TownyBuildEvent(event.player(), block.getLocation(), block.getType(), block,
                townBlock, townBlock == null);
        PlaceBlock.evaluateSiegeWarPlaceBlockRequest(event.player(), block, action);
        copyCancellation(action, event.player(), event.originalEvent());
    }

    @EventHandler
    public void onBlockBreak(TownClaimBlockBreakEvent event) {
        if (!SiegeWarSettings.getWarSiegeEnabled()) {
            return;
        }
        Block block = event.block();
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(block.getLocation());
        TownyDestroyEvent action = new TownyDestroyEvent(event.player(), block.getLocation(), block.getType(), block,
                townBlock, townBlock == null);
        try {
            DestroyBlock.evaluateSiegeWarDestroyBlockRequest(action);
        } catch (TownyException exception) {
            action.setCancelled(true);
            action.setCancelMessage(exception.getMessage(event.player()));
        }
        copyCancellation(action, event.player(), event.originalEvent());
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
