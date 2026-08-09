package datetime

import (
	"fmt"
	"os"
	"sync"
	"time"
)

var (
	localZone *time.Location
	tzOnce    sync.Once
)

func initTimezone() {
	tzOnce.Do(func() {
		tz := os.Getenv("TZ")
		if tz == "" {
			panic("TZ environment variable is not set — set it to the local timezone (e.g. TZ=Europe/Zurich)")
		}
		loc, err := time.LoadLocation(tz)
		if err != nil {
			panic(fmt.Sprintf("failed to load timezone %q: %v", tz, err))
		}
		localZone = loc
	})
}

func FormatForDataRecord(pointInTime int64) string {
	initTimezone()
	return format(pointInTime, "2006-01-02 15:04:05")
}

func FormatFileNameSafe(pointInTime int64) string {
	initTimezone()
	return format(pointInTime, "2006-01-02_15-04-05.000")
}

func format(pointInTime int64, layout string) string {
	dateTime := time.UnixMilli(pointInTime).In(localZone)
	return dateTime.Format(layout)
}
