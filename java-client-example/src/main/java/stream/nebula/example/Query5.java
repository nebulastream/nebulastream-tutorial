package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.sinks.MQTTSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;

import java.io.IOException;

import static stream.nebula.expression.Expressions.attribute;

/**
 * Java version of the following C++ NebulaStream query:
 *
 * <pre>
 *   Query::from("windTurbines")
 *          .unionWith(Query::from("solarPanels"))
 *          .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q5-results", "user", 1000,
 *                                           MQTTSinkDescriptor::TimeUnits::milliseconds, 0,
 *                                           MQTTSinkDescriptor::ServiceQualities::atLeastOnce, true));
 * </pre>
 */
public class Query5 {

    public static void main(String[] args) throws IOException, RESTException, InterruptedException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime =
                NebulaStreamRuntime.getRuntime("localhost", 8081);

        // Use `unionWith` to combine tuples from `windTurbines` and `solarPanels` logical sources.
        Query query = nebulaStreamRuntime.readFromSource("windTurbines")
                .unionWith(nebulaStreamRuntime.readFromSource("solarPanels"));

        // Finish the query with a sink.
        Sink sink = query.sink(new MQTTSink("ws://mosquitto:9001", "q5-results", "user", 1000,
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
