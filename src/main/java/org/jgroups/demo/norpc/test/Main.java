package org.jgroups.demo.norpc.test;


import org.jgroups.demo.norpc.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        String name = args[0];
        String cluster = args[1];

        Node<Config> node = new Node<>(name, getConfigs());
        node.addStopListener(configs -> configs.forEach(Config::stop));
        node.addStartListener(Config::start);
        node.connect(cluster);

        waitQuit();

        List<Config> list = new LinkedList<>(getConfigs());
        list.add(new Config(4));
        node.setPayload(list);

        node.close();
    }

    private static List<Config> getConfigs() {
        return List.of(new Config(1),
                new Config(2),
                new Config(3)
        );
    }

    private static void waitQuit() throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String line = in.readLine().toLowerCase();
                if (line.startsWith("quit") || line.startsWith("q")) {
                    break;
                }
            }
        }
    }
}
