package stream.nebula.example;

import stream.nebula.exceptions.RESTException;
import stream.nebula.operators.sinks.MQTTSink;
import stream.nebula.runtime.NebulaStreamRuntime;
import stream.nebula.runtime.Query;
import stream.nebula.udf.MapFunction;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import static stream.nebula.expression.Expressions.attribute;

/**
 * NebulaStream query that parses the UINT64 timestamp of the consumers source into a human-readable form.
 *
 * The program waits until the status of the query has changed to RUNNING, then waits 10 seconds, and stops the query.
 */
public class JavaUdfExampleQuery {

    // The input class of the Java UDF must match the input schema of the Java map operator,
    // in this case the consumers data source.
    static class ConsumersInput {
        byte consumerId;
        byte sectorId;
        int consumedPower;
        String consumerType;
        long timestamp;
    }

    // The output schema of the Java map operator is derived from the output class
    static class Output {
        byte consumerId;
        byte sectorId;
        int consumedPower;
        String consumerType;
        long timestamp;
        String datetime;
    }

    // The Map UDf class implements the interface MapFunction<IN, OUT>, similarly to a Flink UDF.
    static class ConvertTimestampToDateTime implements MapFunction<ConsumersInput, Output> {

        @Override
        public Output map(final ConsumersInput consumersInput) {
            // Copy fields of the input stream into the output
            Output output = new Output();
            output.consumerId = consumersInput.consumerId;
            output.sectorId = consumersInput.sectorId;
            output.consumedPower = consumersInput.consumedPower;
            output.consumerType = consumersInput.consumerType;
            output.timestamp = consumersInput.timestamp;
            // Convert the UINT64 input timestamp into a human-readable form
            Date date = new Date(consumersInput.timestamp);
            output.datetime = date.toString();
            // Return the output object
            return output;
        }
    }

    public static void main(String[] args) throws IOException, RESTException, InterruptedException {
        // Create a NebulaStream runtime and connect it to the NebulaStream coordinator.
        NebulaStreamRuntime nebulaStreamRuntime =
                NebulaStreamRuntime.getRuntime("localhost", 8081);

        // Process only those tuples from the `consumers` logical source where `consumedPower` is greater than 10000.
        Query query = nebulaStreamRuntime.readFromSource("consumers")

        // Apply UDF to convert UINT64 timestamp to human-readable datetime
                .map(new ConvertTimestampToDateTime());

        // Finish the query with a sink.
        query.sink(new MQTTSink("ws://mosquitto:9001", "q10-java-udf-query", "user", 1000,
                MQTTSink.TimeUnits.milliseconds, 0, MQTTSink.ServiceQualities.atLeastOnce, true));

        // Submit the query to the coordinator.
        int queryId = nebulaStreamRuntime.executeQuery(query, "BottomUp");

        // Wait until the query status changes to running
        for (String status = null;
             !Objects.equals(status, "RUNNING");
             status = nebulaStreamRuntime.getQueryStatus(queryId)) {
            System.out.printf("Query id: %d, status: %s\n", queryId, status);
            Thread.sleep(1000);
        };
    }
}
