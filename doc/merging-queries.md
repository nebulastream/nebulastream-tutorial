# Merging query examples

These queries are sematically equivalent but syntactically different.

The first query uses `filter` before `map`.

```c++
/* Create a query from the windTurbines source. */
Query::from("windTurbines")

      /* Filter tuples where produced power is less than 80000 */
      .filter(Attribute("producedPower") < 80000)

      /* Divide produced Power by 1000 */
      .map(Attribute("producedPower") = Attribute("producedPower") / 1000)

      /* Send to a MQTT sink with topic "q1-merged-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q1-merged-results"));
```

The second query uses `filter` after `map`.
The filter expression is adjust accordingly.

```c++
/* Create a query from the windTurbines source. */
Query::from("windTurbines")

      /* Divide produced Power by 1000 */
      .map(Attribute("producedPower") = Attribute("producedPower") / 1000)

      /* Filter tuples where produced power is less than 80, to take into account the previous division. */
      .filter(Attribute("producedPower") < 80)

      /* Send to a MQTT sink with topic "q2-merged-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q2-merged-results"));
```