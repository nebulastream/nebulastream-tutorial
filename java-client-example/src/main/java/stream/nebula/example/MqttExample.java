package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.Aggregation;
import stream.nebula.operators.sinks.FileSink;
import stream.nebula.operators.sinks.MQTTSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.operators.window.TumblingWindow;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;

import java.io.IOException;

import static stream.nebula.expression.Expressions.attribute;
import static stream.nebula.operators.window.EventTime.eventTime;
import static stream.nebula.operators.window.TimeMeasure.minutes;

public class MqttExample {

    public static void main(String[] args) throws RESTException, IOException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime = NebulaStreamRuntime.getRuntime();
        nebulaStreamRuntime.getConfig().setHost("localhost").setPort("8081");

        // Create a streaming query
        Query query = nebulaStreamRuntime
                        .readFromSource("mqtt-source")
                        .map("v", attribute("v").add(1));

        // Finish the query with a sink
//        Sink sink = query.sink(new FileSink("/tutorial/output/mqtt-query-results.csv", "CSV_FORMAT", false));
        Sink sink = query.sink(new MQTTSink("ws://172.31.0.21:9001", "test-mqtt-out", "xyz", 1,
                MQTTSink.TimeUnits.milliseconds, 10, MQTTSink.ServiceQualities.exactlyOnce, true));

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query, "BottomUp");
    }

}
