package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.Aggregation;
import stream.nebula.operators.sinks.FileSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.operators.window.TumblingWindow;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;
import stream.nebula.serialization.cpp.CppQueryRequestSerializer;
import stream.nebula.udf.MapFunction;

import java.io.IOException;

import static stream.nebula.operators.window.EventTime.eventTime;
import static stream.nebula.operators.window.TimeMeasure.minutes;

public class JavaUdfExample {

    static class Point {
        long x;
        long y;
    }

    static class MyMapFunction implements MapFunction<Point, Point> {
        public Point map(final Point value) {
            Point output =  new Point();
            output.x = 2 * value.x;
            output.y = 2 * value.y;
            return output;
        }
    }

    public static void main(String[] args) throws RESTException, IOException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime = NebulaStreamRuntime.getRuntime();
        nebulaStreamRuntime.getConfig().setHost("localhost").setPort("8081");

        // Create a streaming query
        Query query = nebulaStreamRuntime
                .readFromSource("points")
                .map(new MyMapFunction());

        // Finish the query with a sink
        Sink sink = query.sink(new FileSink("/tutorial/output/java-udf-results.csv", "CSV_FORMAT", true));

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query, "BottomUp");
    }

}
