import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.Aggregation;
import stream.nebula.operators.sinks.FileSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.operators.window.ThresholdWindow;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;
import stream.nebula.serialization.cpp.CppQueryRequestSerializer;

import java.io.IOException;

import static stream.nebula.expression.Expressions.attribute;

/**
 * Example demonstrating the threshold window operator.
 */
public class ThresholdWindowExample {

    public static void main(String[] args) throws RESTException, IOException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime = NebulaStreamRuntime.getRuntime();
        nebulaStreamRuntime.getConfig().setHost("localhost").setPort("8081");

        // Create a query reads from the wind turbines source.
        Query query = nebulaStreamRuntime.readFromSource("wind_turbines");

        // Create a streaming query with a threshold window
        query
                // TODO https://github.com/nebulastream/nebulastream/issues/3596
                // At the moment, the attributes used in the threshold window operation have to be fully
                // qualified with the source name, i.e., "wind_turbines$" in this case.
                // TODO https://github.com/nebulastream/nebulastream/issues/3607
                // The attribute that is used in the predicate, e.g., features_properties_mag, must be an integer.
                .window(ThresholdWindow.of(attribute("wind_turbines$features_properties_mag").greaterThan(1000000)))
                // TODO https://github.com/nebulastream/nebulastream/issues/3608
                // The threshold window operator computes aggregates for each group but does not output the group field.
                .byKey("wind_turbines$features_properties_time")
                .apply(
                        // TODO https://github.com/nebulastream/nebulastream/issues/3595
                        // It is necessary to specify the name of the aggregated field. Also, right now the name must
                        // be the aggregation function, e.g., "Count" for count aggregation and "Avg" for average
                        // aggregation.
                        Aggregation.count().as("wind_turbines$Count"),
                        Aggregation.sum("wind_turbines$features_properties_capacity").as("wind_turbines$Sum"),
                        Aggregation.min("wind_turbines$features_properties_capacity").as("wind_turbines$Min"),
                        Aggregation.max("wind_turbines$features_properties_capacity").as("wind_turbines$Max"),
                        // TODO https://github.com/nebulastream/nebulastream/issues/3602
                        // The output of average aggregation is an integer and not a float.
                        Aggregation.average("wind_turbines$features_properties_capacity").as("wind_turbines$Avg"));


        // Write the output of the query to a CSV file.
        query.sink(new FileSink("/tutorial/output/threshold-window-results.csv", "CSV_FORMAT",true));

        // Submit the query to the coordinator.
        // TODO https://github.com/nebulastream/nebulastream-java-client/issues/206
        // The new Protobuf serializer does not serialize all the required information for the threshold operator and
        // for count aggregation.
        nebulaStreamRuntime.setSerializer(new CppQueryRequestSerializer());
        nebulaStreamRuntime.executeQuery(query, "BottomUp");

        // Once the query has finished, the file `/output/threshold-window-results.csv` contains 340 output tuples
        // (341 lines). The first 4 lines are:
        //wind_turbines$Count:INTEGER,wind_turbines$Sum:INTEGER,wind_turbines$Min:INTEGER,wind_turbines$Max:INTEGER,wind_turbines$Avg:INTEGER
        //3,8556,2852,2852,2852
        //6,17112,2852,2852,2852
        //2,5704,2852,2852,2852
        // These 3 tuples correspond to the first 25 tuples of the input file.
    }

}
