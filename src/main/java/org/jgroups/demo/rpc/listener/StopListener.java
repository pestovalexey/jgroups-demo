package org.jgroups.demo.rpc.listener;

import java.util.List;

@FunctionalInterface
public interface StopListener<P> {

    /**
     *
     * @param payloads List<P>
     */
    void onStopAll(List<P> payloads);

}
