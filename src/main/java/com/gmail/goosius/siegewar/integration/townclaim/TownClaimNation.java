package com.gmail.goosius.siegewar.integration.townclaim;

import com.palmergames.bukkit.towny.object.Nation;

import java.util.UUID;

final class TownClaimNation extends Nation {
    TownClaimNation(String name, UUID id) {
        super(name, id);
        TownClaimMetadataStore.hydrate("nations", id, this);
    }

    @Override
    public void save() {
        TownClaimMetadataStore.save("nations", getUUID(), this);
    }

    @Override
    public boolean hasEnemy(Nation nation) {
        return nation != null && TownClaimBridge.areAtWar(getUUID(), nation.getUUID());
    }
}
