package org.jgroups.demo.norpc.listener;

@FunctionalInterface
public interface StartListener<P> {

    /**
     *
     * @param payload P
     */
    void onStart(P payload);
}
