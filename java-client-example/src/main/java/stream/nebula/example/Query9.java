package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.sinks.MQTTSink;
import stream.nebula.operators.window.TumblingWindow;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;

import java.io.IOException;

import static stream.nebula.expression.Expressions.attribute;
import static stream.nebula.expression.Expressions.literal;
import static stream.nebula.operators.Aggregation.sum;
import static stream.nebula.operators.window.EventTime.eventTime;
import static stream.nebula.operators.window.Duration.hours;

/**
 * Java version of the following C++ NebulaStream query:
 *
 * <pre>
 *   Query::from("windTurbines")
 *   .unionWith(Query::from("solarPanels"))
 *   .window(TumblingWindow::of(EventTime(Attribute("timestamp")), Hours(1)))
 *   .apply(Sum(Attribute("producedPower")))
 *   .map(Attribute("JoinKey") = 1)
 *   .joinWith(Query::from("consumers")
 *            .window(TumblingWindow::of(EventTime(Attribute("timestamp")), Hours(1)))
 *            .apply(Sum(Attribute("consumedPower")))
 *            .map(Attribute("JoinKey") = 1))
 *   .where(Attribute("JoinKey") == Attribute("JoinKey"))
 *   .window(TumblingWindow::of(EventTime(Attribute("start")), Hours(1)))
 *   .map(Attribute("DifferenceProducedConsumedPower") = Attribute("producedPower") - Attribute("consumedPower"))
 *   .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q9-results"));
 * </pre>
 */
public class Query9 {

    public static void main(String[] args) throws IOException, RESTException, InterruptedException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime =
                NebulaStreamRuntime.getRuntime("localhost", 8081);

        // Create a query from the consumers logical source and filter the tuples.
        Query query = nebulaStreamRuntime.readFromSource("windTurbines")
                .unionWith(nebulaStreamRuntime.readFromSource("solarPanels"))
                .window(TumblingWindow.of(eventTime("timestamp"), hours(1)))
                .apply(sum("producedPower"))
                .map("JoinKey", literal(1))
                .joinWith(nebulaStreamRuntime.readFromSource("consumers")
                        .window(TumblingWindow.of(eventTime("timestamp"), hours(1)))
                        .apply(sum("consumedPower"))
                        .map("JoinKey", literal(1)))
                .where(attribute("JoinKey").equalTo(attribute("JoinKey")))
                .window(TumblingWindow.of(eventTime("start"), hours(1)))
                .map("DifferenceProducedConsumedPower",
                        attribute("producedPower").subtract(attribute("consumedPower")));

        // Finish the query with a sink.
        query.sink(new MQTTSink("ws://mosquitto:9001", "q9-results", "user", 1000,
                MQTTSink.TimeUnits.milliseconds, 0, MQTTSink.ServiceQualities.atLeastOnce, true));

        // Submit the query to the coordinator.
        nebulaStreamRuntime.executeQuery(query, "BottomUp");
    }

}
