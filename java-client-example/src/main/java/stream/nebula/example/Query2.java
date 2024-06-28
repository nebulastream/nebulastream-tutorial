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
 *   Query::from("consumers")
 *   .filter(Attribute("consumedPower") >= 400 && Attribute("sectorId") == 1)
 *   .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q2-results"));
 * </pre>
 */
public class Query2 {

    public static void main(String[] args) throws IOException, RESTException, InterruptedException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime =
                NebulaStreamRuntime.getRuntime("localhost", 8081);

        // Process only those tuples from the `consumers` logical source where `consumedPower` is greater than 10000
        // and where `sectorId` equals 1.
        Query query = nebulaStreamRuntime.readFromSource("consumers")
                .filter(attribute("consumedPower").greaterThanOrEqual(400)
                        .and(attribute("sectorId").equalTo(1)));

        // Finish the query with a sink.
        Sink sink = query.sink(new MQTTSink("ws://mosquitto:9001", "q2-results", "user", 1000,
                MQTTSink.TimeUnits.milliseconds, 0, MQTTSink.ServiceQualities.atLeastOnce, true));

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query, "BottomUp");
    }
}
