package org.jgroups.demo.norpc;

import lombok.Getter;
import org.jgroups.Address;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
public class SharedState<P> implements Serializable {
    private final static long serialVersionUID = 3672034987577017232L;

    /**
     *
     */
    private final Map<Address, Boolean> stopConfirms = new HashMap<>();

    /**
     *
     */
    private final List<P> payloadToBalance = new LinkedList<>();

    /**
     *
     * @return true if all stopped
     */
    public boolean allStopped() {
        for (boolean stopped : stopConfirms.values()) {
            if (!stopped) {
                return false;
            }
        }
        return true;
    }
}
