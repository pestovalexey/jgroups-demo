package org.jgroups.demo.rpc.node.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;

/**
 * Calls methods on remote Nodes
 * @param <P>
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteNodeMethodDispatcher<P> {

    private final RpcDispatcher rpcDispatcher;
    private final int timeout;

    /**
     * Stop all payloads on all nodes. Multicast
     */
    public void callRemoteNotifyStopListeners() throws Exception {
        var method = RemoteNodeMethods.class.getMethod("notifyStopListeners");
        var call = new MethodCall(method);
        var opts = RequestOptions.SYNC()
                .timeout(timeout);

        rpcDispatcher.callRemoteMethods(null, call, opts);
    }

    /**
     * Start payload on a single node. Unicast
     *
     * @param payload P
     * @param node    Address node
     */
    public void callRemoteNotifyStartListeners(P payload, Address node) throws Exception {
        var method = RemoteNodeMethods.class.getMethod("notifyStartListeners", Object.class);
        var call = new MethodCall(method, payload);
        var opts = RequestOptions.SYNC()
                .timeout(timeout);

        rpcDispatcher.callRemoteMethod(node, call, opts);
    }

    /**
     * Get (Update) state on all nodes. Multicast
     *
     * @param node From where needs to getState
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
