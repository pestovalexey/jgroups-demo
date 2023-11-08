package org.jgroups.demo.rpc.node.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.demo.rpc.listener.StartListener;
import org.jgroups.demo.rpc.listener.StopListener;

import java.util.LinkedList;
import java.util.List;

/**
 * Node methods that might be invoked remotely with RPC
 * @param <P>
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class RemoteNodeMethods<P> {

    private final List<StartListener<P>> startListeners = new LinkedList<>();
    private final List<StopListener<P>> stopListeners = new LinkedList<>();

    private final JChannel jChannel;
    /**
     * Payload for one Node
     */
    private final List<P> payloadNode = new LinkedList<>();

    /**
     * Notifies Stop listeners, i.e. executes callbacks
     */
    public void notifyStartListeners(P payload) {
        startListeners.forEach(listener -> listener.onStart(payload));
        payloadNode.add(payload);
    }

    /**
     * Notifies Start listeners, executes callbacks
     */
    public void notifyStopListeners() {
        if (payloadNode.isEmpty()) {
            return;
        }
        stopListeners.forEach(listener -> listener.onStopAll(payloadNode));
        payloadNode.clear();
    }

    /**
     * A way to replicate new state to all nodes
     */
    public void getState(Address source, int timeout) throws Exception {
        jChannel.getState(source, timeout);
    }
}
