package com.fcp.benchmark;

import com.fcp.engine.accounting.EntryLine;
import com.fcp.engine.accounting.PostingCommand;
import com.fcp.engine.accounting.JournalValidator;
import com.fcp.common.domain.Currency;
import com.fcp.common.domain.Direction;
import com.fcp.common.domain.JournalType;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** 分录引擎微基准：验证借贷平衡校验与命令构造的开销（阶段三 JMH）。 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class PostingBenchmark {

    private PostingCommand command;

    @Setup(Level.Iteration)
    public void setup() {
        command = new PostingCommand("BENCH-1", JournalType.TRANSFER, Currency.CNY, "DEFAULT", null,
                List.of(new EntryLine(1L, Direction.DEBIT, 10000L),
                        new EntryLine(2L, Direction.CREDIT, 10000L)));
    }

    @Benchmark
    public void validateBalancedJournal() {
        JournalValidator.validate(command);
    }
}