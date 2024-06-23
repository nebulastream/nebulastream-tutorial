# Show wind turbines produced power

Compute the sum of the produced power per hour by the wind turbines, group the results by the solar panel group ID, 
and update the computation every 10 minutes.
Send the result to the `windTurbineDashboards` MQTT topic.
This is similar to query 8 in `example-queries.md`.

```c++
Query::from("windTurbines")
      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10)))
      .byKey(Attribute("groupId"))
      .apply(Sum(Attribute("producedPower")))
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "windTurbineDashboards"));

```

# Show solar panels produced power

Compute the sum of the produced power per hour by the wind turbines, group the results by the solar panel group ID,
and update the computation every 10 minutes.
Send the result to the `solarPanelDashboards` MQTT topic.
This is similar to query 8 in `example-queries.md`.

```c++
Query::from("solarPanels")
      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10)))
      .byKey(Attribute("groupId"))
      .apply(Sum(Attribute("producedPower")))
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "solarPanelDashboards"));

```

# Trigger streetlights

Compute the difference between produced and consumed power.
Send the result to the `differenceProducedConsumedPower` MQTT topic.
This is similar to query 9 in `example-queries.md`.

```c++
Query::from("windTurbines")
      .unionWith(Query::from("solarPanels"))
      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10))
      .apply(Sum(Attribute("producedPower")))
      .map(Attribute("JoinKey") = 1)
      .joinWith(Query::from("consumers")
                      .window(SlidingWindow::of(EventTime(Attribute("timestamp")), Hours(1), Minutes(10))
                      .apply(Sum(Attribute("consumedPower")))
                      .map(Attribute("JoinKey") = 1))
                      .where(Attribute("JoinKey") == Attribute("JoinKey"))
                      .window(SlidingWindow::of(EventTime(Attribute("start")), Hours(1), Minutes(10))
      .map(Attribute("DifferenceProducedConsumedPower") = Attribute("producedPower") - Attribute("consumedPower"))
      .sink(MQTTSinkDescriptor::create("ws://mosquitto:9001", "differenceProducedConsumedPower"));
```