package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.Aggregation;
import stream.nebula.operators.sinks.MQTTSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.operators.window.TimeMeasure;
import stream.nebula.operators.window.TumblingWindow;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;

import java.io.IOException;
import java.util.Objects;

import static stream.nebula.expression.Expressions.attribute;
import static stream.nebula.expression.Expressions.literal;
import static stream.nebula.operators.window.EventTime.eventTime;

/**
 * Java version of the following C++ NebulaStream query:
 *
 * <pre>
 *   Query::from("consumers")
 *          .filter(Attribute("consumedPower") > 10000)
 *          .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q1-results"));
 * </pre>
 *
 * The program waits until the status of the query has changed to RUNNING, then waits 10 seconds, and stops the query.
 */
public class Query1 {

    public static void main(String[] args) throws IOException, RESTException, InterruptedException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime =
                NebulaStreamRuntime.getRuntime("localhost", 8081);

        // Process only those tuples from the `consumers` logical source where `consumedPower` is greater than 10000.
        Query query = nebulaStreamRuntime.readFromSource("consumers")
                .filter(attribute("consumedPower").greaterThan(10000));

        // Finish the query with a sink.
        query.sink(new MQTTSink("ws://mosquitto:9001", "q1-results", "user", 1000,
                MQTTSink.TimeUnits.milliseconds, 0, MQTTSink.ServiceQualities.atLeastOnce, true));

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query, "BottomUp");

        // Wait until the query status changes to running
        for (String status = null;
             !Objects.equals(status, "RUNNING");
             status = nebulaStreamRuntime.getQueryStatus(queryId)) {
            System.out.printf("Query id: %d, status: %s\n", queryId, status);
            Thread.sleep(1000);
        };

        // Let the query run for 10 seconds
        for (int i = 0; i < 10; ++i) {
            String status = nebulaStreamRuntime.getQueryStatus(queryId);
            System.out.printf("Query id: %d, status: %s\n", queryId, status);
            Thread.sleep(1000);
        }

        // Stop the query
        nebulaStreamRuntime.stopQuery(queryId);

        // Wait until the query has stopped
        for (String status = null;
             !Objects.equals(status, "STOPPED");
             status = nebulaStreamRuntime.getQueryStatus(queryId)) {
            System.out.printf("Query id: %d, status: %s\n", queryId, status);
            Thread.sleep(1000);
        };
    }
}
