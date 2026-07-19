package com.palmergames.bukkit.towny;

import com.gmail.goosius.siegewar.Messaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.command.CommandSender;

public final class TownyMessaging {
    private TownyMessaging() {
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(message);
        }
    }

    public static void sendMsg(CommandSender sender, Translatable message) {
        Messaging.sendMsg(sender, message);
    }

    public static void sendErrorMsg(CommandSender sender, String message) {
        Messaging.sendErrorMsg(sender, message);
    }

    public static void sendPrefixedTownMessage(Town town, String message) {
        town.getResidents().stream().map(Resident::getPlayer).filter(player -> player != null)
                .forEach(player -> Messaging.sendMsg(player, message));
    }

    public static void sendPrefixedTownMessage(Town town, Translatable message) {
        town.getResidents().stream().map(Resident::getPlayer).filter(player -> player != null)
                .forEach(player -> Messaging.sendMsg(player, message));
    }

    public static void sendPrefixedNationMessage(Nation nation, String message) {
        nation.getResidents().stream().map(Resident::getPlayer).filter(player -> player != null)
                .forEach(player -> Messaging.sendMsg(player, message));
    }

    public static void sendPrefixedNationMessage(Nation nation, Translatable message) {
        nation.getResidents().stream().map(Resident::getPlayer).filter(player -> player != null)
                .forEach(player -> Messaging.sendMsg(player, message));
    }
}
