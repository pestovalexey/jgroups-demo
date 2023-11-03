package org.jgroups.demo.norpc.listener;

import java.util.List;

@FunctionalInterface
public interface StopListener<P> {

    /**
     *
     * @param payloadList List<P>
     */
    void onStopAll(List<P> payloadList);

}
