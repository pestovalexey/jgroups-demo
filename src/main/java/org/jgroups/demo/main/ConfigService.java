package org.jgroups.demo.main;

import java.util.List;

import static java.util.List.of;

public class ConfigService {

    static List<Config> getConfigs() {
        return of(new Config(1),
                new Config(2),
                new Config(3)
        );
    }

    static List<Config> getNewConfigs() {
        return of(new Config(1),
                new Config(2),
                new Config(3),
                new Config(4),
                new Config(5)
        );
    }
}
