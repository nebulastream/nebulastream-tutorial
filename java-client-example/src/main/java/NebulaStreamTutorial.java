import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.Aggregation;
import stream.nebula.operators.sinks.FileSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.operators.window.TumblingWindow;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;
import stream.nebula.serialization.cpp.CppQueryRequestSerializer;

import java.io.IOException;

import static stream.nebula.operators.window.EventTime.eventTime;
import static stream.nebula.operators.window.TimeMeasure.minutes;

public class NebulaStreamTutorial {

    public static void main(String[] args) throws RESTException, IOException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime = NebulaStreamRuntime.getRuntime();
        nebulaStreamRuntime.getConfig().setHost("localhost").setPort("8081");

        // Create a streaming query
        Query query = nebulaStreamRuntime
                .readFromSource("wind_turbines")
                .window(TumblingWindow.of(eventTime("features_properties_updated"), minutes(10)))
                .byKey("metadata_id")
                .apply(Aggregation.sum("features_properties_mag"));

        // Finish the query with a sink
        Sink sink = query.sink(new FileSink("/tutorial/java-query-results.csv", "CSV_FORMAT", true));

        // The line below is necessary because of a bug when submitting queries with aggregations over Protobuf.
        // TODO https://github.com/nebulastream/nebulastream/issues/3429
        nebulaStreamRuntime.setSerializer(new CppQueryRequestSerializer());

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query, "BottomUp");
    }

}
