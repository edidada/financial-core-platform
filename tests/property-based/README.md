# 属性测试

由 `tests/platform-tests/src/test/java/com/fcp/tests/HotspotEngineTest.java` 等承载：

- 随机生成 5,000 笔交易序列，断言热点缓冲净额引擎永远不超扣、借贷恒平衡、无丢账。
- `common-domain` MoneyTest：金额运算严格整数、无浮点误差。
- `engine-accounting` JournalValidatorTest：借贷不平衡/单分录/溢出一律拒绝。

阶段五扩展：引入 jqwik，对「任意交易序列 + 任意故障注入点」生成状态空间搜索，
证明 ADR-0003 的单事务边界在并发下保持 I-1~I-6。