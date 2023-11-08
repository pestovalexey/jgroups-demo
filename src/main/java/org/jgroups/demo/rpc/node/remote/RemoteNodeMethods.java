package org.jgroups.demo.rpc.node.remote;

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

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class RemoteNodeMethods<P> {

    private final List<StartListener<P>> startListeners = new LinkedList<>();
    private final List<StopListener<P>> stopListeners = new LinkedList<>();

    private final JChannel jChannel;
    /**
     * Local Node's State
     */
    private final List<P> payloadsNode = new LinkedList<>();

    public void notifyStartListeners(P payload) {
        startListeners.forEach(listener -> listener.onStart(payload));
        payloadsNode.add(payload);
    }

    public void notifyStopListeners() {
        if (payloadsNode.isEmpty()) {
            return;
        }
        stopListeners.forEach(listener -> listener.onStopAll(payloadsNode));
        payloadsNode.clear();
    }

    public void getState(Address source, int timeout) throws Exception {
        jChannel.getState(source, timeout);
    }
}
