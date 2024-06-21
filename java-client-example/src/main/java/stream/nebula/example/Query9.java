package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.sinks.MQTTSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.operators.window.SlidingWindow;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;

import java.io.IOException;

import static stream.nebula.operators.Aggregation.sum;
import static stream.nebula.operators.window.EventTime.eventTime;
import static stream.nebula.operators.window.TimeMeasure.hours;
import static stream.nebula.operators.window.TimeMeasure.minutes;

/**
 * Java version of the following C++ NebulaStream query:
 *
 * <pre>
 *   Query::from("windTurbines")
 *          .unionWith(Query::from("solarPanels"))
 *          .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10))
 *          .apply(Sum(Attribute("producedPower")))
 *          .map(Attribute("JoinKey") = 1)
 *          .joinWith(Query::from("consumers"))
 *                          .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10))
 *                          .apply(Sum(Attribute("consumedPower")))
 *                          .map(Attribute("JoinKey") = 1))
 *          .where(Attribute("JoinKey") == Attribute("JoinKey"))
 *          .window(SlidingWindow::of(EventTime(Attribute("start")), Hours(1), Minutes(10))
 *          .map(Attribute("DifferenceProducedConsumedPower") = Attribute("producedPower") - Attribute("consumedPower"))
 *          .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q9-results", "user", 1000,
 *                                           MQTTSinkDescriptor::TimeUnits::milliseconds, 0,
 *                                           MQTTSinkDescriptor::ServiceQualities::atLeastOnce, true));
 * </pre>
 *
 * The program prints the status of the submitted query for 10 seconds.
 * The program does not stop the query.
 */
public class Query9 {

    public static void main(String[] args) throws IOException, RESTException, InterruptedException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime =
                NebulaStreamRuntime.getRuntime("localhost", 8081);

        // Create a query from the consumers logical source and filter the tuples.
        Query query = nebulaStreamRuntime.readFromSource("windTurbines");
        // TODO https://github.com/nebulastream/nebulastream-java-client/pull/315

        // Finish the query with a sink.
        Sink sink = query.sink(new MQTTSink("ws://mosquitto:9001", "q9-results", "user", 1000,
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
