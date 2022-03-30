import stream.nebula.API.Query;
import stream.nebula.exceptions.EmptyFieldException;
import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.Aggregation;
import stream.nebula.operators.EventTime;
import stream.nebula.operators.TimeMeasure;
import stream.nebula.operators.sink.FileSink;
import stream.nebula.operators.windowdefinition.TumblingWindow;
import stream.nebula.runtime.NebulaStreamRuntime;

import java.io.IOException;

public class NebulaStreamTutorial {

    public static void main(String[] args) throws EmptyFieldException, RESTException, IOException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime = NebulaStreamRuntime.getRuntime();
        nebulaStreamRuntime.getConfig().setHost("localhost").setPort("8081");

        // Create a streaming query
        Query query = new Query()
                .from("wind_turbines")
                .window(TumblingWindow.of(new EventTime("features_properties_updated"), TimeMeasure.minutes(10)))
                .byKey("metadata_id")
                .apply(Aggregation.sum("features_properties_mag"))
                .sink(new FileSink("/tutorial/java-query-results.csv", "CSV_FORMAT", "OVERRIDE"));

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query.generateCppCode(), "BottomUp");
    }

}
