package org.jgroups.demo.rpc.main;

import java.util.List;

import static java.util.List.of;

public class ConfigService {

    static List<Config> getConfigs() {
        return of(new Config(1),
                new Config(2),
                new Config(3)
        );
    }
}
