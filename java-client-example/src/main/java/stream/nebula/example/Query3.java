package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.sinks.MQTTSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;

import java.io.IOException;

import static stream.nebula.expression.Expressions.attribute;
import static stream.nebula.expression.Expressions.literal;

/**
 * Java version of the following C++ NebulaStream query:
 *
 * <pre>
 *   Query::from("consumers")
 *          .filter(Attribute("consumedPower") >= 1 && Attribute("consumedPower") < 1000 + 1)
 *          .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q3-results"));
 * </pre>
 */
public class Query3 {

    public static void main(String[] args) throws IOException, RESTException, InterruptedException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime =
                NebulaStreamRuntime.getRuntime("localhost", 8081);

        // Process only those tuples from the `consumers` logical source where `consumedPower` is between the
        // range 1 (inclusive) and 1000 + 1 (exclusive).
        Query query = nebulaStreamRuntime.readFromSource("consumers")
                .filter(attribute("consumedPower").greaterThan(10000)
                        .and(attribute("consumedPower").lessThan(literal(1000).add(1))));

        // Finish the query with a sink.
        Sink sink = query.sink(new MQTTSink("ws://mosquitto:9001", "q3-results", "user", 1000,
                MQTTSink.TimeUnits.milliseconds, 0, MQTTSink.ServiceQualities.atLeastOnce, true));

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query, "BottomUp");

        // Print the status of the query to the console for 10 seconds.
        for (int i = 0; i < 10; ++i) {
            String status = nebulaStreamRuntime.getQueryStatus(queryId);
            System.out.printf("Query id: %d, status: %s\n", queryId, status);
            Thread.sleep(1000);
        }
    }

}
