package com.gmail.goosius.siegewar.integration.townclaim;

import com.palmergames.bukkit.towny.object.Resident;

import java.util.UUID;

final class TownClaimResident extends Resident {
    TownClaimResident(String name, UUID id) {
        super(name, id);
        TownClaimMetadataStore.hydrate("residents", id, this);
    }

    @Override
    public void save() {
        TownClaimMetadataStore.save("residents", getUUID(), this);
    }
}
