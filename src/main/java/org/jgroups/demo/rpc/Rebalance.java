package org.jgroups.demo.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;

import java.lang.reflect.Method;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class Rebalance<P> implements Runnable {

    private final List<P> payload;
    private final List<Address> nodes;
    private final RpcDispatcher dispatcher;
    private final Node<P> nodeApp;

    @Override
    public void run() {
        log.info("Rebalance...");
        try {
            stopAll();
            startRoundRobin();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void stopAll() throws Exception {
        dispatcher.callRemoteMethods(null, "stopAll", null, null,
                RequestOptions.SYNC()
        );
    }

    private void startRoundRobin() throws Exception {
        int n = 0;
        for (P payload : payload) {
            Address node = nodes.get(n);
            start(node, payload);
            if (++n >= nodes.size()) {
                n = 0;
            }
        }
    }

    private void start(Address node, P payload) throws Exception {
        Method method = Node.class.getMethod("start", Object.class);
        MethodCall call = new MethodCall(method, payload);
        dispatcher.callRemoteMethod(node, call,
                RequestOptions.SYNC());

    }
}
