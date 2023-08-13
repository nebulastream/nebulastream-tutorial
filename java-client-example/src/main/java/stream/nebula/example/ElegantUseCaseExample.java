package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.sinks.FileSink;
import stream.nebula.operators.sinks.Sink;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;
import stream.nebula.udf.MapFunction;

import java.io.IOException;

import static stream.nebula.expression.Expressions.attribute;

/**
 * Example demonstrating the execution of a Java UDF.
 */
public class ElegantUseCaseExample {

    // This is the input type of the UDF.
    // The names of the fields `x` and `y` must correspond to the schema of the input stream.
    static class CartesianCoordinate {
        double x;
        double y;
    }

    // This is he output type of the UDF.
    // The schema of the output stream is derived from the names of the fields `angle` and `radius`.
    static class PolarCoordinate {
        double angle;
        double radius;
    }

    // This UDF converts cartesian coordinates to polar coordinates.
    static class CartesianToPolar implements MapFunction<CartesianCoordinate, PolarCoordinate> {
        public PolarCoordinate map(final CartesianCoordinate value) {
            PolarCoordinate output =  new PolarCoordinate();
            output.radius = Math.sqrt(value.x * value.x + value.y * value.y);
            output.angle = Math.atan2(value.x, value.y);
            return output;
        }
    }
    public static void main(String[] args) throws RESTException, IOException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime = NebulaStreamRuntime.getRuntime();
        nebulaStreamRuntime.getConfig().setHost("localhost").setPort("8081");

        // Create a streaming query. The `points` source contains two attributes `x` and `y`.
        Query query = nebulaStreamRuntime.readFromSource("cartesian-points");

        // Project the input attributes for the UDF. This operation is not strictly necessary in this example,
        // because the input stream only contains the two attributes.
        query.project(attribute("x"), attribute("y"));

        // Execute the UDF on the stream.
        query.map(new CartesianToPolar());

        // Finish the query with a sink
        Sink sink = query.sink(new FileSink("/tutorial/output/polar-points.csv", "CSV_FORMAT", true));

        // Submit the query to the coordinator.
        nebulaStreamRuntime.executeQuery(query, "ELEGANT-PERFORMANCE");
    }

}
