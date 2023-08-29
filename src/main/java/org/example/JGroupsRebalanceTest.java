package org.example;


import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jgroups.*;
import org.jgroups.util.Util;

import java.io.*;
import java.util.*;

@RequiredArgsConstructor
public class JGroupsRebalanceTest implements Receiver {
    // Shared State
    private final Map<String, Boolean> stopConfirmationMap = new HashMap<>();
    // Local State
    private final List<Integration> started = new LinkedList<>();

    private Address localhost;
    private JChannel jChannel;

    public void start() throws Exception {
        jChannel = new JChannel().setReceiver(this);
        jChannel.connect("DbCleanCluster");
        jChannel.getState(null, 10000);

        localhost = jChannel.getAddress();
        waitQuit();

        jChannel.close();
    }

    @Override
    public void viewAccepted(View updated) {
        final List<Address> nodes = updated.getMembers();

        // Перераспределить нагрузку среди нод
        Address coordinator = updated.getCoord();
        if (localhost == null) {
            return;
        }

        if (localhost.equals(coordinator)) {
            System.out.println("> I'm Coordinator now");

            Thread coordinatorThread = new Thread(() -> {
                try {
                    for (Address node : nodes) {
                        stopIntegrations(node);

                        stopConfirmationMap.put(node.toString(), false); // состояние обновляет координатор
                    }

                    while (!allStopped()) {
                        synchronized (this) {
                            wait();
                        }
                    }
                    stopConfirmationMap.clear();

                    // Round Robin
                    int n = 0;
                    for (Integration i : getIntegrations()) {
                        startIntegration(i, nodes.get(n));

                        if (++n >= nodes.size()) {
                            n = 0;
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            coordinatorThread.start();
        }
    }

    private void stopIntegrations(Address node) throws Exception {
        Message stopRpc = new ObjectMessage(node, "stop_all_integs");
        jChannel.send(stopRpc);
    }

    private void startIntegration(Integration integration, Address node) throws Exception {
        Message startRpc = new ObjectMessage(node, "start_integ(" + integration.getId() + ")");
        jChannel.send(startRpc);
    }

    private boolean allStopped() {
        for (boolean stopped : stopConfirmationMap.values()) {
            if (!stopped) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void receive(Message msg) {
        String procedure = msg.getObject();

        if (procedure.equals("stop_all_integs")) {
            // IntegrationModule.stop();
            started.forEach(Integration::stop);
            started.clear();

            System.out.println();
            try {
                confirmAllStopped(msg.getSrc());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (procedure.equals("confirm_all_stopped")) {
            stopConfirmationMap.put(msg.getSrc().toString(), true);

            if (allStopped()) {
                synchronized (this) {
                    notify();
                }
            }
        }
        if (procedure.startsWith("start_integ")) {
            // IntegrationModule.start();

            String id = procedure.substring(procedure.indexOf("(") + 1, procedure.indexOf(")"));
            started.add(Integration.builder()
                    .id(Long.parseLong(id))
                    .build()
            );
            System.out.println("start: " + id);
        }
    }

    private void confirmAllStopped(Address node) throws Exception {
        Message confirm = new ObjectMessage(node, "confirm_all_stopped");
        jChannel.send(confirm);
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (stopConfirmationMap) {
            Util.objectToStream(stopConfirmationMap, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        synchronized (stopConfirmationMap) {
            stopConfirmationMap.clear();
            stopConfirmationMap.putAll(Util.objectFromStream(new DataInputStream(input)));
        }
    }

    public static void main(String[] args) throws Exception {
        new JGroupsRebalanceTest().start();
    }

    private List<Integration> getIntegrations() {
        return List.of(
                Integration.builder()
                        .id(1)
                        .build(),
                Integration.builder()
                        .id(2)
                        .build(),
                Integration.builder()
                        .id(3)
                        .build()
        );
    }

    @Getter
    @Builder
    static class Integration {
        long id;

        void stop() {
            System.err.println("stop : " + id);
        }
    }

    private void waitQuit() throws IOException {
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