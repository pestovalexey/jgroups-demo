package org.jgroups.demo.rpc.node.rebalance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.util.DefaultThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
public class RebalanceExecutor {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(
            new DefaultThreadFactory("rebalance-thread-", false));

    public void submit(Rebalance rebalance) {
        executorService.submit(rebalance);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
