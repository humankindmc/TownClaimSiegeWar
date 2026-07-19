package com.gmail.goosius.siegewar.integration.townclaim;

import com.palmergames.bukkit.towny.object.Town;

import java.util.UUID;

final class TownClaimTown extends Town {
    TownClaimTown(String name, UUID id) {
        super(name, id);
        TownClaimMetadataStore.hydrate("towns", id, this);
    }

    @Override
    public void save() {
        TownClaimMetadataStore.save("towns", getUUID(), this);
    }
}
