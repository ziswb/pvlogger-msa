package ch.bziswiler.pvlogger.crepuscular.database.schema

import kotlinx.serialization.Serializable

/*
Example response for url https://api.sunrise-sunset.org/json?lat=<site-lat>&lng=<site-lon>&formatted=0&date=2025-02-12
{
  "results": {
    "sunrise": "2025-02-12T06:32:32+00:00",
    "sunset": "2025-02-12T16:44:32+00:00",
    "solar_noon": "2025-02-12T11:38:32+00:00",
    "day_length": 36720,
    "civil_twilight_begin": "2025-02-12T06:02:18+00:00",
    "civil_twilight_end": "2025-02-12T17:14:46+00:00",
    "nautical_twilight_begin": "2025-02-12T05:26:09+00:00",
    "nautical_twilight_end": "2025-02-12T17:50:55+00:00",
    "astronomical_twilight_begin": "2025-02-12T04:50:30+00:00",
    "astronomical_twilight_end": "2025-02-12T18:26:34+00:00"
  },
  "status": "OK",
  "tzid": "UTC"
}
 */
@Serializable
data class SunriseSunsetData(val results: SunriseSunsetDataResult, val status: String, val tzid: String)