package com.aicommerce.carrier;

import com.aicommerce.carrier.model.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Simulated third-party logistics provider. Deterministic mock data so demos are repeatable.
 * No user identity appears here: the carrier only knows tracking ids, RMA ids and pincodes.
 */
@Component
public class CarrierTools {

    private static final Logger log = LoggerFactory.getLogger("mcp.carrier");
    private static final String CARRIER = "SwiftShip Logistics";

    @McpTool(name = "trackShipment",
            description = "Live carrier tracking for a shipment: scan events, current hub, delay flag and delay reason.")
    public ShipmentTracking trackShipment(
            @McpToolParam(description = "Carrier tracking id, e.g. TRK-BLR-77412", required = true) String trackingId) {
        log.info("[MCP TOOL] trackShipment trackingId={}", trackingId);
        Instant now = Instant.now();
        if ("TRK-BLR-77412".equals(trackingId)) {
            return new ShipmentTracking(trackingId, CARRIER, "DELAYED_AT_HUB", "Bangalore Whitefield Sort Hub",
                    true, "Weather delay: heavy rain and flooding on the Hosur Road corridor halted line-haul trucks",
                    now.plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS).toString(),
                    List.of(
                            new ScanEvent(now.minus(8, ChronoUnit.DAYS).toString(), "Mumbai Origin Warehouse", "PICKED_UP", "Shipment picked up from seller"),
                            new ScanEvent(now.minus(6, ChronoUnit.DAYS).toString(), "Mumbai Gateway Hub", "IN_TRANSIT", "Departed origin hub"),
                            new ScanEvent(now.minus(4, ChronoUnit.DAYS).toString(), "Bangalore Whitefield Sort Hub", "ARRIVED_AT_HUB", "Arrived at destination sort hub"),
                            new ScanEvent(now.minus(3, ChronoUnit.DAYS).toString(), "Bangalore Whitefield Sort Hub", "DELAYED", "Held at hub: weather delay, last-mile vehicles grounded"),
                            new ScanEvent(now.minus(6, ChronoUnit.HOURS).toString(), "Bangalore Whitefield Sort Hub", "DELAYED", "Awaiting road clearance; rescheduled for dispatch")));
        }
        if (trackingId != null && trackingId.startsWith("TRK-MUM-")) {
            return new ShipmentTracking(trackingId, CARRIER, "IN_TRANSIT", "Mumbai Andheri Hub", false, null,
                    now.plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS).toString(),
                    List.of(new ScanEvent(now.minus(1, ChronoUnit.DAYS).toString(), "Mumbai Origin Warehouse", "PICKED_UP", "Shipment picked up"),
                            new ScanEvent(now.minus(6, ChronoUnit.HOURS).toString(), "Mumbai Andheri Hub", "IN_TRANSIT", "Arrived at local hub")));
        }
        if (trackingId != null && trackingId.startsWith("TRK-")) {
            return new ShipmentTracking(trackingId, CARRIER, "DELIVERED", "Customer address", false, null, null,
                    List.of(new ScanEvent(now.minus(5, ChronoUnit.DAYS).toString(), "Local delivery hub", "OUT_FOR_DELIVERY", "Out for delivery"),
                            new ScanEvent(now.minus(4, ChronoUnit.DAYS).toString(), "Customer address", "DELIVERED", "Delivered, signed by customer")));
        }
        return new ShipmentTracking(String.valueOf(trackingId), CARRIER, "NOT_FOUND", "-", false,
                "No shipment found for this tracking id", null, List.of());
    }

    @McpTool(name = "generateReturnShippingLabel",
            description = "Generate a prepaid reverse-logistics shipping label (AWB, barcode, instructions) for an approved RMA.")
    public ReturnLabel generateReturnShippingLabel(
            @McpToolParam(description = "RMA id of the approved return, e.g. RMA-1A2B3C", required = true) String rmaId,
            @McpToolParam(description = "Customer display name for the label", required = true) String customerName,
            @McpToolParam(description = "Pickup pincode (6 digits)", required = true) String pickupPincode,
            @McpToolParam(description = "Product category: ELECTRONICS, APPAREL, FOOTWEAR or HYGIENE", required = true) String category) {
        log.info("[MCP TOOL] generateReturnShippingLabel rmaId={} pincode={} category={}", rmaId, pickupPincode, category);
        // Deterministic from rmaId so repeated calls return the same label (idempotent).
        String awb = "AWB" + String.format("%09d", Math.abs((rmaId == null ? "" : rmaId).hashCode()) % 1_000_000_000);
        String cat = category == null ? "" : category.toUpperCase();
        String packaging = switch (cat) {
            case "ELECTRONICS" -> "Use original box with cushioning. Include all accessories and charging cable. Fragile: do not stack.";
            case "FOOTWEAR" -> "Pack in original shoebox, unworn and with tags attached. Do not tape directly on the shoebox.";
            case "APPAREL" -> "Fold neatly in a sealed polybag with tags attached.";
            case "HYGIENE" -> "Keep sealed in original hygiene packaging.";
            default -> "Pack securely in a sturdy box.";
        };
        return new ReturnLabel(rmaId, awb, "||| " + awb + " |||| " + pickupPincode + " ||||", CARRIER, customerName,
                pickupPincode, "DROP_OFF_OR_PICKUP", packaging,
                "Attach label on the outside of the parcel. Drop at a nearby point or hand over to the pickup agent within 5 days.");
    }

    @McpTool(name = "findNearbyDropoffPoints",
            description = "Find the 3 nearest return drop-off lockers/hubs for a pincode, with distance and operating hours.")
    public List<DropoffPoint> findNearbyDropoffPoints(
            @McpToolParam(description = "6-digit pincode", required = true) String pincode) {
        log.info("[MCP TOOL] findNearbyDropoffPoints pincode={}", pincode);
        String pc = pincode == null ? "" : pincode.trim();
        return switch (pc) {
            case "560100" -> List.of(
                    new DropoffPoint("SwiftLocker - Electronic City Phase 1", "SMART_LOCKER", "Ground Floor, Infinity Mall, Electronic City, Bangalore", pc, 0.8, "Mon-Sun 08:00-22:00", true),
                    new DropoffPoint("SwiftShip Hub - Hosur Road", "CARRIER_HUB", "42, Hosur Main Road, Bangalore", pc, 2.1, "Mon-Sat 09:00-19:00", true),
                    new DropoffPoint("Partner Store - Kirana Plus Konappana Agrahara", "PARTNER_STORE", "17, Konappana Agrahara, Bangalore", pc, 3.4, "Mon-Sun 10:00-21:00", true));
            case "400001" -> List.of(
                    new DropoffPoint("SwiftLocker - Fort Station", "SMART_LOCKER", "CSMT Concourse, Fort, Mumbai", pc, 0.5, "Mon-Sun 06:00-23:00", true),
                    new DropoffPoint("SwiftShip Hub - Ballard Estate", "CARRIER_HUB", "11, Ballard Estate, Mumbai", pc, 1.2, "Mon-Sat 09:00-18:00", true),
                    new DropoffPoint("Partner Store - Colaba Causeway Mart", "PARTNER_STORE", "8, Colaba Causeway, Mumbai", pc, 2.7, "Mon-Sun 10:00-21:00", true));
            default -> List.of(
                    new DropoffPoint("SwiftLocker - City Centre", "SMART_LOCKER", "City Centre Mall, pincode " + pc, pc, 1.0, "Mon-Sun 09:00-21:00", true),
                    new DropoffPoint("SwiftShip Hub - Central", "CARRIER_HUB", "Central Depot, pincode " + pc, pc, 2.5, "Mon-Sat 09:00-18:00", true),
                    new DropoffPoint("Partner Store - Local Mart", "PARTNER_STORE", "Main Market, pincode " + pc, pc, 3.8, "Mon-Sun 10:00-20:00", true));
        };
    }
}
