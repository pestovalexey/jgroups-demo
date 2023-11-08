package org.jgroups.demo.rpc.listener;

@FunctionalInterface
public interface StartListener<P> {

    /**
     *
     * @param payload P
     */
    void onStart(P payload);
}
