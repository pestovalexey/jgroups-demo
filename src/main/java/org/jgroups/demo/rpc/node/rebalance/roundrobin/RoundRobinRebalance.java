package org.jgroups.demo.rpc.node.rebalance.roundrobin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.demo.rpc.node.rebalance.Rebalance;
import org.jgroups.demo.rpc.node.remote.RemoteNodeMethodDispatcher;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class RoundRobinRebalance<P> implements Rebalance {

    private final List<P> payloads;
    private final List<Address> nodes;
    private final RemoteNodeMethodDispatcher<P> remoteNodeMethodDispatcher;

    @Override
    public void run() {
        try {
            remoteNodeMethodDispatcher.callRemoteNotifyStopListeners();
            int n = 0;
            for (P p : payloads) {
                Address node = nodes.get(n++);
                remoteNodeMethodDispatcher.callRemoteNotifyStartListeners(p, node);
                if (n == nodes.size()) {
                    n = 0;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
