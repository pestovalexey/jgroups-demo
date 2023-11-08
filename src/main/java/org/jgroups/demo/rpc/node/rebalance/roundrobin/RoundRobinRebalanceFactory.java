package org.jgroups.demo.rpc.node.rebalance.roundrobin;

import org.jgroups.Address;
import org.jgroups.demo.rpc.node.rebalance.Rebalance;
import org.jgroups.demo.rpc.node.rebalance.RebalanceFactory;
import org.jgroups.demo.rpc.node.remote.RemoteNodeMethodDispatcher;

import java.util.List;

public class RoundRobinRebalanceFactory<P> implements RebalanceFactory<P> {

    @Override
    public Rebalance create(List<P> payloads,
                            List<Address> nodes,
                            RemoteNodeMethodDispatcher<P> remoteNodeMethodDispatcher
    ) {
        return new RoundRobinRebalance<>(payloads, nodes,
                remoteNodeMethodDispatcher);
    }
}
