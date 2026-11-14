package com.fcp.engine.routing;

import com.fcp.engine.rule.RuleFact;

public record RoutingContext(String currency, long amountMinor, String channelHint, RuleFact facts) {}