package datetime

import (
	"os"
	"time"

	"pv-logger/logging"
)

var localZone *time.Location

func init() {
	initTimezone()
}

func initTimezone() {
	tz := os.Getenv("TZ")
	if tz == "" {
		logging.Fatal("TZ environment variable is not set — set it to the local timezone (e.g. TZ=Europe/Zurich)")
	}
	loc, err := time.LoadLocation(tz)
	if err != nil {
		logging.Fatal("failed to load timezone", "tz", tz, "error", err)
	}
	localZone = loc
}

func FormatForDataRecord(pointInTime int64) string {
	return format(pointInTime, "2006-01-02 15:04:05")
}

func FormatFileNameSafe(pointInTime int64) string {
	return format(pointInTime, "2006-01-02_15-04-05.000")
}

func format(pointInTime int64, layout string) string {
	dateTime := time.UnixMilli(pointInTime).In(localZone)
	return dateTime.Format(layout)
}
