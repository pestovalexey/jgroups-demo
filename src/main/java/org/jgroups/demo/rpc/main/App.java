package org.jgroups.demo.rpc.main;

import org.jgroups.demo.rpc.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

class App {
    static final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws Exception {
        String name = args[0];
        String cluster = args[1];

        Node<Config> node = new Node<>(name, ConfigService.getConfigs());
        node.addStopListener(configs -> configs.forEach(Config::stop));
        node.addStartListener(Config::start);
        node.connect(cluster);

        waitQuit();

        // Добавилась конфигурация
        List<Config> list = new LinkedList<>(ConfigService.getConfigs());
        list.add(new Config(4));
        node.setPayload(list);

        waitQuit();

        node.close();
    }

    static void waitQuit() throws IOException {
        while (true) {
            String line = App.in.readLine().toLowerCase();
            if (line.startsWith("quit") || line.startsWith("q")) {
                break;
            }
        }
    }
}
