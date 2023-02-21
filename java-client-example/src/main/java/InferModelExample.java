import stream.nebula.exceptions.RESTException;
import stream.nebula.expression.BasicType;
import stream.nebula.operators.Aggregation;
import stream.nebula.operators.sinks.FileSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.operators.window.TumblingWindow;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;
import stream.nebula.serialization.cpp.CppQueryRequestSerializer;

import java.io.IOException;
import java.util.Objects;

import static stream.nebula.expression.Expressions.attribute;
import static stream.nebula.operators.window.EventTime.eventTime;
import static stream.nebula.operators.window.TimeMeasure.minutes;

/**
 * Example demonstrating the {@code inferModel} operation.
 * It then maps the attributes {@code f1}, {@code f2}, {@code f3}, and {@code f4} as inputs for the TensorFlow model.
 * The model computes three output fields which are stored as the attributes {@code iris0}, {@code iris1}, and {@code
 * iris2} in the output tuple.
 */
public class InferModelExample {

    public static void main(String[] args) throws RESTException, IOException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime = NebulaStreamRuntime.getRuntime();
        nebulaStreamRuntime.getConfig().setHost("localhost").setPort("8081");

        // Create a query reads from the iris logical source.
        // This logical source has one physical source which is the CSV file `/resources/iris_short.csv`.
        // This CSV file has 10 input tuples.
        Query query = nebulaStreamRuntime.readFromSource("iris");

        // Run a TensorFlow model on each tuple.
        query
                .inferModel(
                    // Read the model `iris_95acc.tflite` as a resource from the classpath.
                    Objects.requireNonNull(InferModelExample.class.getResourceAsStream("/iris_95acc.tflite")),
                    // Give the model a name that is a valid file name on the coordinator and worker.
                    "/tmp/iris_95acc.tflite")
                // Map the attributes `f1`, `f2`, `f3`, and `f4` as inputs for the TensorFlow model.
                .on(
                        attribute("f1"),
                        attribute("f2"),
                        attribute("f3"),
                        attribute("f4"))
                // The model computes three output fields which are stored as the attributes `iris0`, `iris1`, and
                // `iris2` in the output tuple.
                .as(
                        attribute("iris0", BasicType.FLOAT32),
                        attribute("iris1", BasicType.FLOAT32),
                        attribute("iris2", BasicType.FLOAT32));

        // Write the output of the query to a CSV file.
        Sink sink = query.sink(new FileSink("/tutorial/output/infer-model-results.csv", "CSV_FORMAT",
                true));

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query, "BottomUp");

        // Once the query has finished, the file `/output/infer-model-results.csv` contains 10 output tuples.
        // Each output tuple contains the fields of the input tuple (f1, f2, ...) + the output fields of the
        // TensorFlow Lite model (iris0, iris1, iris2).
    }

}
