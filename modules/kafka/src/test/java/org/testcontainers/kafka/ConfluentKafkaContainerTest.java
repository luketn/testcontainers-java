package org.testcontainers.kafka;

import org.junit.Test;
import org.testcontainers.AbstractKafka;
import org.testcontainers.KCatContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SocatContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfluentKafkaContainerTest extends AbstractKafka {

    @Test
    public void testUsage() throws Exception {
        try ( // constructorWithVersion {
            ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")
            // }
        ) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testUsageWithListener() throws Exception {
        try (
            Network network = Network.newNetwork();
            // registerListener {
            ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")
                .withListener("kafka:19092")
                .withNetwork(network);
            // }
            KCatContainer kcat = new KCatContainer().withNetwork(network)
        ) {
            kafka.start();
            kcat.start();

            kcat.execInContainer("kcat", "-b", "kafka:19092", "-t", "msgs", "-P", "-l", "/data/msgs.txt");
            String stdout = kcat
                .execInContainer("kcat", "-b", "kafka:19092", "-C", "-t", "msgs", "-c", "1")
                .getStdout();

            assertThat(stdout).contains("Message produced by kcat");
        }
    }

    @Test
    public void testUsageWithListenerFromProxy() throws Exception {
        try (
            Network network = Network.newNetwork();
            // registerListenerFromProxy {
            SocatContainer socat = new SocatContainer().withNetwork(network).withTarget(2000, "kafka", 19092);
            ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")
                .withListener("kafka:19092", () -> socat.getHost() + ":" + socat.getMappedPort(2000))
                .withNetwork(network)
            // }
        ) {
            socat.start();
            kafka.start();

            String bootstrapServers = String.format("%s:%s", socat.getHost(), socat.getMappedPort(2000));
            testKafkaFunctionality(bootstrapServers);
        }
    }
}