package org.jgroups.demo.rpc.node.remote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;

@Slf4j
@RequiredArgsConstructor
public class RemoteNodeMethodDispatcher<P> {

    private final RpcDispatcher rpcDispatcher;
    private final int timeout;

    /**
     * Multicast stop all payloads on all the nodes
     */
    public void callRemoteNotifyStopListeners() throws Exception {
        var method = RemoteNodeMethods.class.getMethod("notifyStopListeners");
        var call = new MethodCall(method);
        var opts = RequestOptions.SYNC()
                .timeout(timeout);

        rpcDispatcher.callRemoteMethods(null, call, opts);
    }

    /**
     * Unicast start payload on the nodes
     *
     * @param payload P
     * @param node    Address
     */
    public void callRemoteNotifyStartListeners(P payload, Address node) throws Exception {
        var method = RemoteNodeMethods.class.getMethod("notifyStartListeners", Object.class);
        var call = new MethodCall(method, payload);
        var opts = RequestOptions.SYNC()
                .timeout(timeout);

        rpcDispatcher.callRemoteMethod(node, call, opts);
    }

    /**
     * Multicast get state (update) on all the nodes
     *
     * @param node From
     * @throws Exception throw
     */
    public void callRemoteGetState(Address node) throws Exception {
        var method = RemoteNodeMethods.class.getMethod("getState", Address.class, int.class);
        var call = new MethodCall(method, node, timeout);
        var opts = RequestOptions.SYNC()
                .setTransientFlags(Message.TransientFlag.DONT_LOOPBACK)
                .timeout(timeout);

        rpcDispatcher.callRemoteMethods(null, call, opts);
    }

}
