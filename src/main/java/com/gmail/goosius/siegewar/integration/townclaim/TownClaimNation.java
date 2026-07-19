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
        // ponytail: TownClaim has no diplomacy system yet; treat every foreign nation as an opponent.
        return nation != null && !getUUID().equals(nation.getUUID());
    }
}
