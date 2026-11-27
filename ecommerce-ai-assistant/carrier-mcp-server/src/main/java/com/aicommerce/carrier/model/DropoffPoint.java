package com.aicommerce.carrier.model;

public record DropoffPoint(String name, String type, String address, String pincode, double distanceKm,
                           String hours, boolean acceptsReturns) {}
