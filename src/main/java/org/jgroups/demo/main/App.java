package org.jgroups.demo.main;

import lombok.extern.slf4j.Slf4j;
import org.jgroups.demo.rpc.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
class App {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    final int timeout = 10000;

    public static void main(String[] args) {
        var name = args[0];
        var cluster = args[1];

        new App().demo(name, cluster);
    }

    void demo(String name, String cluster) {
        try {
            Node<Config> node = new Node<>(name, "raft.xml", timeout);

            // Register listeners
            node.addStopListener(configs -> configs.forEach(Config::stop));
            node.addStartListener(Config::start);

            // Connection
            List<Config> configs = ConfigService.getConfigs();
            node.connect(cluster, configs);

            waitQuit();

            // State Change
            node.setState(ConfigService.getNewConfigs());
            waitQuit();

            // State Change
            node.setState(ConfigService.getConfigs());
            waitQuit();

            node.close();
            reader.close();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    void waitQuit() throws IOException {
        while (true) {
            String line = reader.readLine().toLowerCase();
            if (line.startsWith("quit") || line.startsWith("q")) {
                break;
            }
        }
    }
}
