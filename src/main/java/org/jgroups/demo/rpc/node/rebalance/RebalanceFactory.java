package org.jgroups.demo.rpc.node.rebalance;

import org.jgroups.Address;
import org.jgroups.demo.rpc.node.remote.RemoteNodeMethodDispatcher;

import java.util.List;

public interface RebalanceFactory<P> {

    Rebalance create(List<P> payloads,
                     List<Address> nodes,
                     RemoteNodeMethodDispatcher<P> remoteNodeMethodDispatcher);
}

