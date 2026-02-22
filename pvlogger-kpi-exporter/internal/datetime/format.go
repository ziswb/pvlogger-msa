package datetime

import (
	"time"

	"pv-logger/logging"
)

func FormatForDataRecord(pointInTime int64) string {
	return format(pointInTime, "2006-01-02 15:04:05")
}

func FormatFileNameSafe(pointInTime int64) string {
	return format(pointInTime, "2006-01-02_15-04-05.000")
}

func format(pointInTime int64, layout string) string {
	loc, err := time.LoadLocation("Europe/Zurich")
	if err != nil {
		logging.Fatal("failed to load timezone", "location", "Europe/Zurich", "error", err)
	}
	dateTime := time.Date(1970, 1, 1, 0, 0, 0, 0, loc).Add(time.Duration(pointInTime) * time.Millisecond)
	return dateTime.Format(layout)
}
