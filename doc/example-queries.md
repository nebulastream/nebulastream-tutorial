# Query 1: Filter tuples

Process only those tuples from the `consumers` logical source where `consumedPower` is greater than 10000.

```c++
/* Create a query from the consumers source. */
Query::from("consumers")

      /* Filter tuples with an expression on the consumedPower attribute */
      .filter(Attribute("consumedPower") > 10000)

      /* Send to a MQTT sink with topic "q1-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q1-results"));
```

# Query 2: Filter over multiple attributes

Process only those tuples from the `consumers` logical source where `consumedPower` is greater than 10000
and where `sectorId` equals 1.

```c++
/* Create a query from the consumers source. */
Query::from("consumers")

      /* Combine filters over multiple attributes with && or || */
      .filter(Attribute("consumedPower") > 10000 && Attribute("sectorId") == 1)

      /* Send to a MQTT sink with topic "q2-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q2-results"));
```

# Query 3: Filter with complex expressions

Process only those tuples from the `consumers` logical source where `consumedPower` is between the
range 1 (inclusive) and 1000 + 1 (exclusive).

```c++
/* Create a query from the consumers source. */
Query::from("consumers")
      
      /* Filter with a complex expression over a single attribute using logical and arithmetic operations. */
      .filter(Attribute("consumedPower") >= 1 && Attribute("consumedPower") < 1000 + 1) 

      /* Send to a MQTT sink with topic "q3-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q3-results"));
```

# Query 4: Transform data

Transform the attribute `consumedPower` of the `consumers` logical source by dividing it by 1000.

```c++
/* Create a query from the consumers source. */
Query::from("consumers")

      /* Assign the result of a complex expression to an existing attribute. */
      .map(Attribute("consumedPower") = Attribute("consumedPower") / 1000)

      /* Send to a MQTT sink with topic "q4-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q4-results"));
```

# Query 5: Combine multiple data sources

Use `unionWith` to combine tuples from `windTurbines` and `solarPanels` logical sources.

````c++
/* Create a query from the windTurbines source. */
Query::from("windTurbines")

      /* Combine with the tuples from the solarPanels source. */
      .unionWith(Query::from("solarPanels"))
      
      /* Send to a MQTT sink with topic "q5-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q5-results"));
````

# Query 6: Enrich tuples with data

Create the attribute `source` to identify the original logical source before combining them with `unionWith`.

```c++
/* Create a query from the windTurbines source. */
Query::from("windTurbines")     

       /* Assign the result of an expression (here: a constant) to a new attribute of the windTurbines source. */ 
      .map(Attribute("source") = 1)
      
      /* Combine with the tuples from the solarPanels source. */
      .unionWith(Query::from("solarPanels")
      
                        /* Assign a different constant to the tuples of the solarPanels source. */
                       .map(Attribute("Source") = 2))

      /* Send to a MQTT sink with topic "q6-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q6-results"));
```

# Query 7: Window aggregations with tumbling windows

Compute the sum of the produced power per hour by the solar panels and group the results by the solar panel group ID. 

```c++
/* Create a query from the solarPanels source. */
Query::from("solarPanels")

      /* Create a tumbling window of size 1 hour, using the timestamp attribute as the event time of the tuples. */
      .window(TumblingWindow::of(EventTime(Attribute("timestamp")), Hours(1)))
       
      /* Group the contents of the window on the groupId attribute */
      .byKey(Attribute("groupId"))
       
      /* Compute the sum of the attribute producedPower */ 
      .apply(Sum(Attribute("producedPower")))

      /* Send to a MQTT sink with topic "q7-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q7-results"));
```

# Query 8: Window aggregations with sliding windows

Compute the sum of the produced power per hour by the solar panels, group the results by the solar panel group ID, 
and update the computation every 10 minutes.

```c++
/* Create a query from the solarPanels source. */
Query::from("solarPanels")

      /* Create a sliding window of size 1 hour and slide 10 minutes, using the timestamp attribute as the event time of the tuples. */
      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10)))
      
      /* Group the contents of the window on the groupId attribute */
      .byKey(Attribute("groupId"))

      /* Compute the sum of the attribute producedPower */ 
      .apply(Sum(Attribute("producedPower")))

      /* Send to a MQTT sink with topic "q8-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q8-results"));
```

# Query 9: Window join

- Compute the sum of produced power per hour by wind turbines and solar panels.
- Compute the sum of consumed power by all consumers.
- Join these sums for each hour.
- Compute the difference between produced and consumed power.
- Update results using a sliding window of size 1 hour and slide 10 minutes.

```c++
/* Combine both wind turbine and solar panel producers. */
Query::from("windTurbines")
      .unionWith(Query::from("solarPanels"))
      
      /* Compute the sum of produced power in the last hour, update every 10 minutes.
      .window(TumblingWindow::of(EventTime(Attribute("timestamp")), Hours(1)))
      .apply(Sum(Attribute("producedPower")))
      
      /* Add a join key. */
      .map(Attribute("JoinKey") = 1)
      
      /* Join with consumers */
      .joinWith(Query::from("consumers")
                      .window(TumblingWindow::of(EventTime(Attribute("timestamp")), Hours(1)))
                      .apply(Sum(Attribute("consumedPower")))
                      .map(Attribute("JoinKey") = 1))

      // Cross-join all tuples from both input streams                
      .where(Attribute("JoinKey") == Attribute("JoinKey"))

      // Join tuples inside a tumbling window with size 1 hour
      .window(TumblingWindow::of(EventTime(Attribute("start")), Hours(1)))

       /* Compute the difference between produced and consumed power. */
      .map(Attribute("DifferenceProducedConsumedPower") = Attribute("producedPower") - Attribute("consumedPower"))
      
      /* Send to a MQTT sink with topic "q9-results". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "q9-results"));
```