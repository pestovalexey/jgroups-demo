package org.jgroups.demo.rpc;

import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.demo.rpc.exception.NodeException;
import org.jgroups.demo.rpc.listener.StartListener;
import org.jgroups.demo.rpc.listener.StopListener;
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

@Slf4j
public class Node<P> implements Receiver, Closeable {
    // Shared state
    private final List<P> payload = new LinkedList<>();
    private final List<P> payloadLocal = new LinkedList<>();

    private final List<StopListener<P>> stopListeners = new LinkedList<>();
    private final List<StartListener<P>> startListeners = new LinkedList<>();

    private final JChannel jChannel;
    private final RaftHandle raftHandle;
    private final RpcDispatcher dispatcher;

    /**
     * @param name    Node name
     * @param payload Payload list to balance in a Cluster
     */
    public Node(String name, List<P> payload) {
        this(name, payload, "raft.xml");
    }

    /**
     * @param name         Node name
     * @param payload      Payload list to balance in a Cluster
     * @param jGroupConfig JGroups config Xml file
     */
    public Node(String name, List<P> payload, String jGroupConfig) {
        this.payload.addAll(payload);
        try {
            jChannel = new JChannel(jGroupConfig);
            jChannel.setReceiver(this);
            jChannel.name(name);

            dispatcher = new RpcDispatcher(jChannel, this);
            dispatcher.setReceiver(this);

            raftHandle = new RaftHandle(jChannel, null);
            raftHandle.raftId(name);
            raftHandle.addRoleListener(role -> {
                log.info("Role: {}", role);

                if (Role.Leader == role) {
                    rebalance(jChannel.getView());
                }
            });

        } catch (Exception e) {
            log.error("Error", e);
            throw new NodeException(e);
        }
    }

    public void addStopListener(StopListener<P> listener) {
        stopListeners.add(listener);
    }

    public void addStartListener(StartListener<P> listener) {
        startListeners.add(listener);
    }

    /**
     *
     * @param cluster to connect
     * @throws Exception e
     */
    public void connect(String cluster) throws Exception {
        jChannel.connect(cluster);
        jChannel.getState(null,
                10000);
    }

    @Override
    public void viewAccepted(View view) {
        log.info("Cluster change {}", view);

        if (raftHandle.isLeader()) {
            rebalance(view);
            return;
        }
        if (getClusterSize(view) == 1) {
            notifyStopListeners();
        }
    }

    private int getClusterSize(View view) {
        return view.getMembers()
                .size();
    }

    private void rebalance(View view) {
        List<Address> nodes = view.getMembers();
        Rebalance<P> rebalance = new Rebalance<>(
                payload, nodes,
                dispatcher,
                this
        );
        Thread t = new Thread(rebalance);
        t.setDaemon(false);
        t.start();
    }

    public void stopAll() {
        if (!payloadLocal.isEmpty()) {
            notifyStopListeners();
        }
        payloadLocal.clear();
    }

    private void notifyStopListeners() {
        for (StopListener<P> listener : stopListeners) {
            listener.onStopAll(payloadLocal);
        }
    }

    public void start(P payload) {
        notifyStartListeners(payload);
        payloadLocal.add(payload);
    }

    private void notifyStartListeners(P payload) {
        for (StartListener<P> listener : startListeners) {
            listener.onStart(payload);
        }
    }

    /**
     * @param payload Updated Payload to balance in a Cluster
     */
    public void setPayload(List<P> payload) {
        this.payload.clear();
        this.payload.addAll(payload);

        rebalance(jChannel.getView());
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

    @Override
    public void close() throws IOException {
        dispatcher.close();
        jChannel.close();
    }

    /**
     * @param node Node to add to a Cluster
     * @throws Exception e
     */
    public void addNode(String node) throws Exception {
        raftHandle.addServer(node);
    }

    /**
     * @param node Node to remove from a Cluster
     * @throws Exception e
     */
    public void removeNode(String node) throws Exception {
        raftHandle.removeServer(node);
    }

}