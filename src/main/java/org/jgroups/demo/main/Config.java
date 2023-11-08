package org.jgroups.demo.main;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
@ToString
@RequiredArgsConstructor
public class Config implements Serializable {
    private final static long serialVersionUID = 3672034987577016212L;

    private final long id;

    public void start() {
        log.info("start: " + id);
    }

    public void stop() {
        log.error("stop : " + id);
    }

}
