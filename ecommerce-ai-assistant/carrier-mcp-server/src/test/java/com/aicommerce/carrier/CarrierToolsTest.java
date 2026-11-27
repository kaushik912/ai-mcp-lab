package com.aicommerce.carrier;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CarrierToolsTest {

    private final CarrierTools tools = new CarrierTools();

    @Test
    void delayedShipmentReportsWeatherDelayAtBangaloreHub() {
        var t = tools.trackShipment("TRK-BLR-77412");
        assertThat(t.delayed()).isTrue();
        assertThat(t.currentLocation()).contains("Bangalore");
        assertThat(t.delayReason()).containsIgnoringCase("weather");
        assertThat(t.events()).isNotEmpty();
    }

    @Test
    void unknownTrackingIdIsNotFound() {
        assertThat(tools.trackShipment("NOPE").status()).isEqualTo("NOT_FOUND");
    }

    @Test
    void labelIsIdempotentPerRma() {
        var a = tools.generateReturnShippingLabel("RMA-ABC123", "Alex", "560100", "FOOTWEAR");
        var b = tools.generateReturnShippingLabel("RMA-ABC123", "Alex", "560100", "FOOTWEAR");
        assertThat(a.awb()).isEqualTo(b.awb()).startsWith("AWB");
        assertThat(a.packagingInstructions()).containsIgnoringCase("shoebox");
    }

    @Test
    void returnsThreeDropoffPoints() {
        assertThat(tools.findNearbyDropoffPoints("560100")).hasSize(3);
        assertThat(tools.findNearbyDropoffPoints("999999")).hasSize(3);
    }
}
