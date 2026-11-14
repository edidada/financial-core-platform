package com.fcp.common.utils;

/** 应用层 ID 生成器，屏蔽 AUTO_INCREMENT / BIGSERIAL 方言差异（ADR-0006）。 */
@FunctionalInterface
public interface IdGenerator {
    long nextId();
}