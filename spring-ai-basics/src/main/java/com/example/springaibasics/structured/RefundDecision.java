package com.example.springaibasics.structured;

/** What we ask the model to return. Spring AI maps the model's JSON onto this record. */
public record RefundDecision(boolean eligible, int amount, String reason) {}
