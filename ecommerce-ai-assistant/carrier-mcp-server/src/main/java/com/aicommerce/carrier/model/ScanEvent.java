package com.aicommerce.carrier.model;

public record ScanEvent(String timestamp, String location, String status, String detail) {}
