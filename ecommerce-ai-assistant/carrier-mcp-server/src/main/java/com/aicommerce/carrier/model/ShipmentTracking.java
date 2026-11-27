package com.aicommerce.carrier.model;

import java.util.List;

public record ShipmentTracking(String trackingId, String carrier, String status, String currentLocation,
                               boolean delayed, String delayReason, String revisedEta, List<ScanEvent> events) {}
