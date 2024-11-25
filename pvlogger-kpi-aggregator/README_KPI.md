# KPIs

KPIs are computed in dimensions "live" and "today so far":

* "live" is in kW
* "today so far" is in kWh

The sources are described in the subsequent paragraphs.

# Computation for "today so far"

| KPI         |         Device         | Data Type | Unit |  Gain   | Register Address |  Source Type  |
|:------------|:----------------------:|:---------:|:----:|:-------:|-----------------:|:-------------:|
| Produced    |        INVERTER        |    U32    | KWH  | HUNDRED |            32114 |    "daily"    |
| Export/Sell |         METER          |    I32    | KWH  | HUNDRED |            37119 | "accumulated" |
| Import/Buy  |         METER          |    I32    | KWH  | HUNDRED |            37121 | "accumulated" |
| Consumption | Produced-Export+Import |           |      |         |                  |               |
| Onw Use     |    Produced-Export     |           |      |         |                  |               |

# Computation for "live"

| KPI               |         Device         | Data Type | Unit |   Gain   | Register Address | Remark        |
|:------------------|:----------------------:|:---------:|:----:|:--------:|-----------------:|---------------|
| Producing         |        INVERTER        |    I32    |  KW  | THOUSAND |            32080 | Solar Power   |
| Exporting/Selling |         METER          |    I32    |  W   |   ONE    |            37113 | If value >= 0 |
| Importing/Buying  |         METER          |    I32    |  W   |   ONE    |            37113 | If value < 0  |
| Consuming         | Produced-Export+Import |           |      |          |                  |               |
| Own Use           |    Produced-Export     |           |      |          |                  |               |
