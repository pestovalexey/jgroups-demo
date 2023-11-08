package org.jgroups.demo.rpc;

import lombok.extern.slf4j.Slf4j;

import org.jgroups.*;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.demo.rpc.listener.StartListener;
import org.jgroups.demo.rpc.listener.StopListener;
import org.jgroups.demo.rpc.node.rebalance.RebalanceExecutor;
import org.jgroups.demo.rpc.node.rebalance.RebalanceFactory;
import org.jgroups.demo.rpc.node.rebalance.roundrobin.RoundRobinRebalanceFactory;
import org.jgroups.demo.rpc.node.rpc.RemoteNodeMethodDispatcher;
import org.jgroups.demo.rpc.node.rpc.RemoteNodeMethods;
import org.jgroups.protocols.raft.RAFT;
import org.jgroups.protocols.raft.RAFT.RoleChange;
import org.jgroups.protocols.raft.Role;
import org.jgroups.raft.RaftHandle;
import org.jgroups.util.Util;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A Node in a Cluster with automatic Load Balancing and High Consistency (CP)
 * @param <P>
 */
@Slf4j
public class Node<P> implements Receiver, RoleChange, Closeable {
    /**
     * Shared State
     */
    private final List<P> payload = new LinkedList<>();

    private final JChannel jChannel;
    private final RaftHandle raftHandle;
    private final RpcDispatcher rpcDispatcher;

    /**
     * RpcDispatcher's Server Obj
     */
    private final RemoteNodeMethods<P> remoteNodeMethods;
    private final RemoteNodeMethodDispatcher<P> remoteNodeMethodDispatcher;
    private final RebalanceFactory<P> rebalanceFactory;
    private final RebalanceExecutor rebalanceExecutor;
    private final int timeout;

    /**
     * @param name    Node name
     * @param config  Xml config file path
     * @param timeout All requests timeout and setState timeout
     */
    public Node(String name,
                String config,
                Integer timeout
    ) throws Exception {
        this(name, config, timeout, new RoundRobinRebalanceFactory<>());
    }

    /**
     * @param rebalanceFactory RebalanceFactory
     */
    public Node(String name,
                String config,
                Integer timeout,
                RebalanceFactory<P> rebalanceFactory
    ) throws Exception {

        this.rebalanceFactory = requireNonNull(rebalanceFactory);
        this.timeout = requireNonNull(timeout);

        jChannel = new JChannel(config);
        jChannel.setReceiver(this);
        jChannel.setName(name);

        raftHandle = new RaftHandle(jChannel, null);
        raftHandle.addRoleListener(this);
        raftHandle.raftId(name);

        remoteNodeMethods = new RemoteNodeMethods<>(jChannel);
        rpcDispatcher = new RpcDispatcher(jChannel, remoteNodeMethods);
        rpcDispatcher.setReceiver(this);

        rebalanceExecutor = new RebalanceExecutor();
        remoteNodeMethodDispatcher = new RemoteNodeMethodDispatcher<>(
                rpcDispatcher,
                timeout);
    }

    public void addStopListener(StopListener<P> listener) {
        remoteNodeMethods.getStopListeners()
                .add(listener);
    }

    public void addStartListener(StartListener<P> listener) {
        remoteNodeMethods.getStartListeners()
                .add(listener);
    }

    /**
     * API
     * Ignore state if payloads shared state is not empty
     *
     * @param cluster Name to connection
     * @param state   State (or Payload) for balancing in cluster
     */
    public void connect(String cluster, List<P> state) throws Exception {
        jChannel.connect(cluster);
        jChannel.getState(null, timeout);

        synchronized (payload) {
            if (payload.isEmpty()) {
                payload.addAll(state);
            }
        }
    }

    @Override
    public void roleChanged(Role role) {
        log.info("role {}", role);
        if (Role.Leader == role) {
            rebalance(jChannel.getView());
        }
    }

    @Override
    public void viewAccepted(View view) {
        log.info("view {}", view);
        if (payload.isEmpty()) {
            return;
        }
        if (raftHandle.isLeader()) {
            rebalance(view);
            return;
        }
        if (!canOperate(view)) {
            remoteNodeMethods.notifyStopListeners();
        }
    }

    private boolean canOperate(View view) {
        return getSize(view) >= getMinSize();
    }

    private int getSize(View view) {
        return view.getMembers()
                .size();
    }

    /**
     * To operate consistent
     */
    private int getMinSize() {
        var stack = jChannel.getProtocolStack();
        RAFT raft = stack.findProtocol(RAFT.class);
        return (raft.members().size() / 2) + 1;
    }

    /**
     * Runs load rebalance
     */
    private void rebalance(View view) {
        var nodes = view.getMembers();
        var rebalance = rebalanceFactory.create(payload, nodes, remoteNodeMethodDispatcher);
        rebalanceExecutor.submit(rebalance);
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (payload) {
            Util.objectToStream(payload, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        synchronized (payload) {
            List<P> fromStream = Util.objectFromStream(new DataInputStream(input));
            payload.clear();
            payload.addAll(fromStream);
        }
    }

    /**
     * API
     * Replicate state on all nodes, then run rebalance
     */
    public void setState(List<P> state) throws Exception {
        var view = jChannel.getView();
        if (canOperate(view)) {
            synchronized (payload) {
                payload.clear();
                payload.addAll(state);
            }
            var local = jChannel.address();
            remoteNodeMethodDispatcher.callRemoteGetState(local);
            rebalance(view);
        }
    }

    /**
     * API
     * Replicate change
     */
    private void addReplica(P p) {
        var view = jChannel.getView();
        if (canOperate(view)) {
            synchronized (payload) {
                payload.add(p);

                // TODO
            }
        }
    }

    /**
     * API
     */
    @Override
    public void close() throws IOException {
        rebalanceExecutor.shutdown();
        rpcDispatcher.close();
        jChannel.close();
    }
}