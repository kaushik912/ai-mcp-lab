package com.aicommerce.carrier.model;

public record ReturnLabel(String rmaId, String awb, String barcode, String carrier, String customerName,
                          String pickupPincode, String serviceLevel, String packagingInstructions,
                          String instructions) {}
