package org.jgroups.demo.norpc;

@FunctionalInterface
public interface StartListener<P> {

    /**
     *
     * @param payload P
     */
    void onStart(P payload);
}
