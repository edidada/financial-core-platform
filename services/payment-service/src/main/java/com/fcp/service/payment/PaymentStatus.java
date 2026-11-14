package com.fcp.service.payment;

/** 支付指令生命周期：与账务分离（README 第三节「支付指令与资金结算分离」）。 */
public enum PaymentStatus { CREATED, SUBMITTED, ACCEPTED, REJECTED, UNKNOWN, SETTLED, FAILED }