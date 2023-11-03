package org.jgroups.demo.norpc.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
@RequiredArgsConstructor
class Config implements Serializable {
    private final static long serialVersionUID = 3672034987577017282L;

    private final long id;

    public void start() {
        log.info("start: " + id);
    }

    public void stop() {
        log.error("stop : " + id);
    }

}
