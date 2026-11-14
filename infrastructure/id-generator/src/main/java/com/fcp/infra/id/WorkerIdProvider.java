package com.fcp.infra.id;

/** workerId 来源：多实例部署时每实例必须唯一（环境变量 / K8s StatefulSet 序号）。 */
public interface WorkerIdProvider {
    long workerId();
}