package com.gmail.goosius.siegewar.integration.townclaim;

import java.util.UUID;

final class TownClaimTerritory extends TownClaimTown {
    private final UUID anchorClaimId;

    TownClaimTerritory(String name, UUID id, UUID anchorClaimId) {
        super(name, id);
        this.anchorClaimId = anchorClaimId;
    }

    UUID anchorClaimId() {
        return anchorClaimId;
    }
}
