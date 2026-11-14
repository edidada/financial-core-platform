package com.fcp.engine.posting;

import com.fcp.engine.accounting.Journal;
import com.fcp.engine.accounting.PostingCommand;

/** 账务引擎契约；实现必须运行在调用方本地事务内 (ADR-0003)。 */
public interface PostingEngine {

    Journal post(PostingCommand command);

    /** 生成反向凭证，原凭证保持不变 (ADR-0004)。 */
    Journal reverse(long originalJournalId, String newBizId, String reason);
}