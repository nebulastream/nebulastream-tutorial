# Show wind turbines produced power

Compute the sum of the produced power per hour by the wind turbines, group the results by the solar panel group ID, 
and update the computation every 10 minutes.
Send the result to the `windTurbineDashboards` MQTT topic.
This is similar to query 8 in `example-queries.md`.

```c++
/* Create a query from the windTurbines source. */
Query::from("windTurbines")

      /* Create a sliding window of size 1 hour and slide 10 minutes, using the timestamp attribute as the event time of the tuples. */
      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10)))
       
      /* Group the contents of the window on the groupId attribute */
      .byKey(Attribute("groupId"))
       
      /* Compute the sum of the attribute producedPower */ 
      .apply(Sum(Attribute("producedPower")))

      /* Send to a MQTT sink with topic "windTurbineDashboards". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "windTurbineDashboards"));
```

# Show solar panels produced power

Compute the sum of the produced power per hour by the wind turbines, group the results by the solar panel group ID,
and update the computation every 10 minutes.
Send the result to the `solarPanelDashboards` MQTT topic.
This is similar to query 8 in `example-queries.md`.

```c++
/* Create a query from the solarPanels source. */
Query::from("solarPanels")

      /* Create a sliding window of size 1 hour and slide 10 minutes, using the timestamp attribute as the event time of the tuples. */
      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10)))
       
      /* Group the contents of the window on the groupId attribute */
      .byKey(Attribute("groupId"))
       
      /* Compute the sum of the attribute producedPower */ 
      .apply(Sum(Attribute("producedPower")))

      /* Send to a MQTT sink with topic "solarPanelDashboards". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "solarPanelDashboards"));
```

# Trigger streetlights

Compute the difference between produced and consumed power.
Send the result to the `differenceProducedConsumedPower` MQTT topic.
This is similar to query 9 in `example-queries.md`.

```c++
/* Combine both wind turbine and solar panel producers. */
Query::from("windTurbines")
      .unionWith(Query::from("solarPanels"))
      
      /* Compute the sum of produced power in the last hour, update every 10 minutes.
      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10))
      .apply(Sum(Attribute("producedPower")))

      /* Add a join key. */
      .map(Attribute("JoinKey") = 1)

      /* Join with consumers */
      .joinWith(Query::from("consumers")
                      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10))
                      .apply(Sum(Attribute("consumedPower")))
                      .map(Attribute("JoinKey") = 1))

      // Cross-join all tuples from both input streams                
      .where(Attribute("JoinKey") == Attribute("JoinKey"))

      // Join sliding inside a tumbling window with size 1 hour, slide 10 minutes
      .window(SlidingWindow::of(EventTime(Attribute("start")), Hours(1), Minutes(10))

       /* Compute the difference between produced and consumed power. */
      .map(Attribute("DifferenceProducedConsumedPower") = Attribute("producedPower") - Attribute("consumedPower"))
      
      /* Send to a MQTT sink with topic "differenceProducedConsumedPower". */
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "differenceProducedConsumedPower"));
```