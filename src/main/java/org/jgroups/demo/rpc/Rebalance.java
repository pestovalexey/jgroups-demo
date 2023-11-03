package org.jgroups.demo.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class Rebalance<P> implements Runnable {

    private final List<P> payload;
    private final List<Address> nodes;
    private final RpcDispatcher dispatcher;

    @Override
    public void run() {
        log.info("Rebalance...");
        try {
            stopAll();
            roundRobinStart();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void stopAll() throws Exception {
        dispatcher.callRemoteMethods(null, "stopAll", null, null, RequestOptions.SYNC());

        //Method method = Node.class.getMethod(name);
        //MethodCall call = new MethodCall(method);
        //RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 0);
    }

    private void roundRobinStart() throws Exception {
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
        StartObject<P> arg = new StartObject<>(payload);
        dispatcher.callRemoteMethod(node, "start", new Object[]{arg},
                new Class[]{StartObject.class},
                RequestOptions.SYNC()
        );
        //Method method = Node.class.getMethod(name, StartObject.class);
        //MethodCall call = new MethodCall(method, arg);
        //RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 0);

    }
}
