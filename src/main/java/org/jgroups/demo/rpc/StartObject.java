package org.jgroups.demo.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@RequiredArgsConstructor
public class StartObject<P> implements Serializable {
    private final static long serialVersionUID = 3672034987577017232L;

    @Getter
    private final P payload;
}
