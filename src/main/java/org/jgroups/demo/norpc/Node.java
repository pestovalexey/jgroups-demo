package org.jgroups.demo.norpc;

import lombok.extern.slf4j.Slf4j;
import org.jgroups.*;
import org.jgroups.protocols.raft.Role;
import org.jgroups.raft.RaftHandle;
import org.jgroups.util.Util;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class Node<P> implements Receiver, Closeable {

    private final SharedState<P> sharedState = new SharedState<>();
    private final List<P> localPayload = new LinkedList<>();

    private final List<StopListener<P>> stopListeners = new LinkedList<>();
    private final List<StartListener<P>> startListeners = new LinkedList<>();

    private final JChannel jChannel;
    private final RaftHandle raftHandle;

    private Lock lock;
    private Condition allStop;

    /**
     * @param name    Node name
     * @param payload List<P> to balance in a Cluster
     */
    public Node(String name, List<P> payload) {
        this(name, payload, "raft.xml");
    }

    /**
     * @param name         Node name
     * @param payload      List<P> to balance in a Cluster
     * @param jGroupConfig JGroups config Xml file
     */
    public Node(String name, List<P> payload, String jGroupConfig) {
        List<P> list = sharedState.getPayloadToBalance();
        list.addAll(payload);
        try {
            jChannel = new JChannel(jGroupConfig);
            jChannel.setReceiver(this);
            jChannel.name(name);

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

    private void notifyStopListeners() {
        for (StopListener<P> listener : stopListeners) {
            listener.onStopAll(localPayload);
        }
    }

    private void rebalance(View view) {
        lock = new ReentrantLock();
        allStop = lock.newCondition();

        Rebalance<P> r = new Rebalance<>(view,
                jChannel,
                sharedState,
                allStop,
                lock
        );
        Thread t = new Thread(r);
        t.setDaemon(false);
        t.start();
    }

    @Override
    public void receive(Message message) {
        if (message.getObject().equals("stop_all")) {
            receiveStopAll(message);
            return;
        }
        if (message.getObject().equals("confirm_all_stopped")) {
            receiveStopAllConfirm(message);
            return;
        }
        if (message.getObject() instanceof StartObject) {
            receiveStartObject(message);
        }
    }

    private void receiveStopAll(Message message) {
        if (!localPayload.isEmpty()) {
            notifyStopListeners();
        }
        localPayload.clear();

        Address leader = message.getSrc();
        sendStopAllConfirm(leader);
    }

    private void sendStopAllConfirm(Address node) {
        try {
            jChannel.send(new ObjectMessage(node, "confirm_all_stopped"));
        } catch (Exception e) {
            log.error("Error", e);
            throw new NodeException(e);
        }
    }

    private void receiveStopAllConfirm(Message message) {
        Address node = message.getSrc();
        sharedState.getStopConfirms().put(node, true);

        if (sharedState.allStopped()) {
            lock.lock();
            try {
                allStop.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    private void receiveStartObject(Message message) {
        StartObject<P> object = message.getObject();
        notifyStartListeners(object);

        localPayload.add(object.getPayload());
    }

    /**
     *
     * @param object StartObject<P>
     */
    private void notifyStartListeners(StartObject<P> object) {
        for (StartListener<P> listener : startListeners) {
            listener.onStart(object.getPayload());
        }
    }

    /**
     * @param payload Updated Payload to balance in a Cluster
     */
    public void setPayload(List<P> payload) {
        List<P> list = sharedState.getPayloadToBalance();
        list.clear();
        list.addAll(payload);

        rebalance(jChannel.getView());
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (sharedState) {
            Util.objectToStream(sharedState, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        synchronized (sharedState) {
            DataInputStream inputStream = new DataInputStream(input);
            SharedState<P> fromStream = Util.objectFromStream(inputStream);

            sharedState.getStopConfirms().clear();
            sharedState.getStopConfirms().putAll(fromStream.getStopConfirms());

            sharedState.getPayloadToBalance().clear();
            sharedState.getPayloadToBalance().addAll(fromStream.getPayloadToBalance());
        }
    }

    @Override
    public void close() {
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