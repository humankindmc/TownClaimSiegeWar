package com.gmail.goosius.siegewar.integration.townclaim;

public final class TownClaimMetadataStoreCheck {
    public static void main(String[] args) {
        assert Boolean.TRUE.equals(TownClaimMetadataStore.field("flag", "BooleanDataField", "true").getValue());
        assert Double.valueOf(2.5).equals(TownClaimMetadataStore.field("decimal", "DecimalDataField", "2.5").getValue());
        assert Integer.valueOf(4).equals(TownClaimMetadataStore.field("integer", "IntegerDataField", "4").getValue());
        assert Long.valueOf(8).equals(TownClaimMetadataStore.field("long", "LongDataField", "8").getValue());
        assert "value".equals(TownClaimMetadataStore.field("string", "StringDataField", "value").getValue());
        assert TownClaimMetadataStore.field("unknown", "Unknown", "value") == null;
    }
}
