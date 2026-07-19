package com.gmail.goosius.siegewar.integration.townclaim;

import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.TownOccupationController;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.gmail.goosius.siegewar.utils.SiegeWarDistanceUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarTownPeacefulnessUtil;
import com.humankindmc.claims.integration.TownOperationGuard;
import com.humankindmc.claims.model.Claim;
import com.humankindmc.claims.model.ClaimRegion;
import com.humankindmc.claims.nation.Nation;
import com.humankindmc.claims.town.Town;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

final class TownClaimOperationGuard implements TownOperationGuard {
    @Override
    public Decision canJoinTown(UUID playerId, Town town) {
        com.palmergames.bukkit.towny.object.Town siegeTown = TownClaimBridge.town(town.id());
        if (enabled() && SiegeWarSettings.getWarSiegeBesiegedTownRecruitmentDisabled()
                && siegeTown != null && SiegeController.hasActiveSiege(siegeTown)) {
            return Decision.deny("siegewar-besieged-town-cannot-recruit");
        }
        return Decision.allow();
    }

    @Override
    public Decision canClaim(UUID actorId, Town town, ClaimRegion region) {
        if (!enabled()) {
            return Decision.allow();
        }
        com.palmergames.bukkit.towny.object.Town siegeTown = TownClaimBridge.town(town.id());
        if (SiegeWarSettings.getWarSiegeBesiegedTownClaimingDisabled()
                && siegeTown != null && SiegeController.hasActiveSiege(siegeTown)) {
            return Decision.deny("siegewar-besieged-town-cannot-claim");
        }
        if (SiegeWarSettings.getWarSiegeClaimingDisabledNearSiegeZones() && isNearActiveSiege(region)) {
            return Decision.deny("siegewar-claim-too-near-siege-zone");
        }
        return Decision.allow();
    }

    @Override
    public Decision canUnclaim(UUID actorId, Claim claim) {
        com.palmergames.bukkit.towny.object.Town town = TownClaimBridge.town(claim.townId());
        if (town == null) {
            return Decision.allow();
        }
        if (SiegeWarSettings.getWarCommonOccupiedTownUnClaimingDisabled()
                && TownOccupationController.isTownOccupied(town)) {
            return Decision.deny("siegewar-occupied-town-cannot-unclaim");
        }
        if (enabled() && SiegeWarSettings.getWarSiegeBesiegedTownUnClaimingDisabled()
                && SiegeController.hasActiveSiege(town)) {
            return Decision.deny("siegewar-besieged-town-cannot-unclaim");
        }
        return Decision.allow();
    }

    @Override
    public Decision canLeaveNation(UUID actorId, Town sourceTown, Nation nation) {
        com.palmergames.bukkit.towny.object.Town town = TownClaimBridge.town(sourceTown.id());
        if (enabled() && town != null && TownOccupationController.isTownOccupied(town)) {
            return Decision.deny("siegewar-occupied-town-cannot-leave-nation");
        }
        return Decision.allow();
    }

    @Override
    public Decision canSetNationCapital(UUID actorId, Nation nation, Town oldCapital, Town newCapital) {
        com.palmergames.bukkit.towny.object.Town oldTown = oldCapital == null
                ? null : TownClaimBridge.town(oldCapital.id());
        com.palmergames.bukkit.towny.object.Town newTown = newCapital == null
                ? null : TownClaimBridge.town(newCapital.id());
        if (!enabled() || newTown == null) {
            return Decision.allow();
        }
        if ((oldTown != null && SiegeController.hasSiege(oldTown)) || SiegeController.hasSiege(newTown)) {
            return Decision.deny("siegewar-besieged-capital-cannot-change");
        }
        if (SiegeWarTownPeacefulnessUtil.isTownPeaceful(newTown)) {
            return Decision.deny("siegewar-peaceful-town-cannot-be-capital");
        }
        return Decision.allow();
    }

    private static boolean enabled() {
        return SiegeWarSettings.getWarSiegeEnabled();
    }

    private static boolean isNearActiveSiege(ClaimRegion region) {
        World world = Bukkit.getWorld(region.worldName());
        if (world == null) {
            return false;
        }
        double x = ((long) region.minX() + region.maxX()) / 2.0;
        double z = ((long) region.minZ() + region.maxZ()) / 2.0;
        return SiegeWarDistanceUtil.isLocationInActiveSiegeZone(new Location(world, x, 0, z));
    }
}
