package org.jgroups.demo.norpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.ObjectMessage;
import org.jgroups.View;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Slf4j
@RequiredArgsConstructor
public class Rebalance<P> implements Runnable {

    private final View view;
    private final JChannel jChannel;
    private final SharedState<P> sharedState;

    private final Condition allStop;
    private final Lock lock;

    @Override
    public void run() {
        log.info("Rebalance...");
        try {
            List<Address> nodes = view.getMembers();
            for (Address node : nodes) {
                sendStopAll(node);
            }
            waitStopAllConfirmations();
            sharedState.getStopConfirms().clear();

            int n = 0;
            for (P payload : sharedState.getPayloadToBalance()) {
                Address node = nodes.get(n++);
                sendStart(node, payload);
                if (n == nodes.size()) {
                    n = 0;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void waitStopAllConfirmations() throws InterruptedException {
        while (!allStopped()) {
            lock.lock();
            try {
                allStop.await();
            } finally {
                lock.unlock();
            }
        }
    }

    private void sendStopAll(Address node) throws Exception {
        jChannel.send(new ObjectMessage(node, "stop_all"));
        sharedState.getStopConfirms().put(node, false);
    }

    private boolean allStopped() {
        Map<Address, Boolean> confirmations = sharedState.getStopConfirms();
        for (boolean stopped : confirmations.values()) {
            if (!stopped) {
                return false;
            }
        }
        return true;
    }

    private void sendStart(Address node, P payload) throws Exception {
        StartObject<P> object = new StartObject<>(payload);
        ObjectMessage message = new ObjectMessage(node, object);
        message.setSrc(jChannel.getAddress());

        jChannel.send(message);
    }
}
