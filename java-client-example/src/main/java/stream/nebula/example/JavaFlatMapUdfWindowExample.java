package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.sinks.FileSink;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;
import stream.nebula.udf.FlatMapFunction;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static stream.nebula.expression.Expressions.attribute;

/**
 * Example demonstrating how to implement arbitrary window logic with a flatMap UDF that retains state.
 */
public class JavaFlatMapUdfWindowExample {

    // This is the input type of the UDF.
    // The names of the fields `timestamp` and `age` must correspond to the schema of the input stream.
    static class Input {
        String timestamp;
        int age;
    }

    // This is he output type of the UDF.
    // The output stream will contain fields that correspond to the field names.
    static class Output {
        String start;
        String end;
        double meanAge;
        double medianAge;
        String sorted;
    }

    // This UDF tracks a configurable sliding window and computes the median and mean age of the input.
    static class SlidingWindow implements FlatMapFunction<Input, Output> {

        // These fields hold the immutable properties of this particular window instance, i.e., the size and the slide.
        private final int size;
        private final int slide;

        // These fields hold the mutable state of the window, i.e., the elements contained in the window and a
        // variable to track the size of the current window slide.
        LinkedList<Input> window = new LinkedList<>();
        private int index = 0;

        public SlidingWindow(int size, int slide) {
            this.size = size;
            this.slide = slide;
        }

        // The flatMap method is called for every tuple.
        // It can access state retained inside the UDF across invocations.
        // If the current tuple does not trigger a new window, it returns an empty result.
        // Otherwise, if the current tuple triggers a new window, it aggregates the window contents and emits a
        // single output tuple.
        // FlatMap can also return a collection with multiple output tuples, e.g., it could emit every tuple in the
        // window and enrich it with additional data.
        public Collection<Output> flatMap(Input value) {
            // Print the input for debugging on the console of the worker.
            System.out.println("timestamp = " + value.timestamp + "; age = " + value.age);
            // Keep the last `size` objects in the window.
            if (window.size() == size) {
                window.removeFirst();
            }
            window.addLast(value);
            // Emit empty result if the current window slide is not completed or if the first window is not full.
            index += 1;
            if (index < slide || window.size() != size) {
                return Collections.emptyList();
            }
            // Reset the window slide, compute window aggregation, and emit result.
            index = 0;
            Output output = new Output();
            output.start = window.getFirst().timestamp;
            output.end = window.getLast().timestamp;
            List<Double> sortedAges = window.stream().map(i -> (double) i.age).sorted().collect(Collectors.toList());
            output.sorted = sortedAges.toString();
            output.meanAge = sortedAges.stream().reduce(0.0, Double::sum) / size;
            output.medianAge = size % 2 == 0
                    ? (sortedAges.get(size / 2 - 1) + sortedAges.get(size / 2)) / 2
                    : sortedAges.get(size / 2);
            return Collections.singletonList(output);
        }
    }

    public static void main(String[] args) throws RESTException, IOException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime = NebulaStreamRuntime.getRuntime();
        nebulaStreamRuntime.getConfig().setHost("localhost").setPort("8081");

        // Create a streaming query. The `ages` source contains two attributes `timestamp` and `age`.
        Query query = nebulaStreamRuntime.readFromSource("ages");

        // Project the input attributes for the UDF.
        // This operation is not strictly necessary in this example, because the input stream only contains the two
        // attributes.
        query.project(attribute("timestamp"), attribute("age"));

        // Execute the UDF on the stream.
        query.flatMap(new SlidingWindow(6, 2));

        // Finish the query with a sink
        query.sink(new FileSink("/tutorial/output/ages-results.csv", "CSV_FORMAT", true));

        // Submit the query to the coordinator.
        nebulaStreamRuntime.executeQuery(query, "BottomUp");
    }

}
