package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.playeractions.DestroyBlock;
import com.gmail.goosius.siegewar.playeractions.PlaceBlock;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.gmail.goosius.siegewar.utils.SiegeWarBlockUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarDistanceUtil;
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
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (SiegeWarSettings.getWarSiegeEnabled()
                && SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        if (SiegeWarSettings.getWarSiegeEnabled()
                && SiegeWarSettings.getSiegeZoneWildernessForbiddenBucketMaterials().contains(event.getBucket())
                && TownyAPI.getInstance().isWilderness(target)
                && SiegeWarDistanceUtil.isLocationInActiveSiegeZone(target.getLocation())) {
            event.setCancelled(true);
            Messaging.sendErrorMsg(event.getPlayer(), com.palmergames.bukkit.towny.object.Translatable
                    .of("msg_war_siege_zone_bucket_emptying_forbidden").forLocale(event.getPlayer()));
        }
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
