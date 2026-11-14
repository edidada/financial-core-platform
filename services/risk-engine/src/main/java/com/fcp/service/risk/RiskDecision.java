package com.fcp.service.risk;

public record RiskDecision(String outcome, int score, String route, long latencyMillis, boolean degraded) {}