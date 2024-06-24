package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.sinks.MQTTSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.operators.window.SlidingWindow;
import stream.nebula.operators.window.TumblingWindow;
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
 *   Query::from("solarPanels")
 *          .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10)))
 *          .byKey(Attribute("groupId"))
 *          .apply(Sum(Attribute("producedPower")))
 *          .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q8-results"));
 * </pre>
 */
public class Query8 {

    public static void main(String[] args) throws IOException, RESTException, InterruptedException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime =
                NebulaStreamRuntime.getRuntime("localhost", 8081);

        // Compute the sum of the produced power per hour by the solar panels, group the results by the solar panel
        // group ID, and update the computation every 10 minutes.
        Query query = nebulaStreamRuntime.readFromSource("solarPanels")
                .window(SlidingWindow.of(eventTime("timestamp"), hours(1), minutes(10)))
                .byKey("groupId")
                .apply(sum("producedPower"));

        // Finish the query with a sink.
        query.sink(new MQTTSink("ws://mosquitto:9001", "q8-results", "user", 1000,
                MQTTSink.TimeUnits.milliseconds, 0, MQTTSink.ServiceQualities.atLeastOnce, true));
        // Submit the query to the coordinator.
        nebulaStreamRuntime.executeQuery(query, "BottomUp");
    }

}
