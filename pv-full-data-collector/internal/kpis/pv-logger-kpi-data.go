package kpis

import "fmt"

type PvLoggerKpiData struct {
	JobId         string `json:"jobId"`
	DateCreated   int64  `json:"dateCreated"`
	Producing     W      `json:"producing"`
	Exporting     W      `json:"exporting"`
	Importing     W      `json:"importing"`
	Consuming     W      `json:"consuming"`
	SelfConsuming W      `json:"selfConsuming"`
	Produced      Wh     `json:"produced"`
	Exported      Wh     `json:"exported"`
	Imported      Wh     `json:"imported"`
	Consumed      Wh     `json:"consumed"`
	SelfConsumed  Wh     `json:"selfConsumed"`
}

func (p *PvLoggerKpiData) String() string {
	return fmt.Sprintf("JobId: %s, DateCreated: %d, Producing: %v, Exporting: %v, Importing: %v, Consuming: %v, SelfConsuming: %v, Produced: %v, Exported: %v, Imported: %v, Consumed: %v, SelfConsumed: %v", p.JobId, p.DateCreated, p.Producing, p.Exporting, p.Importing, p.Consuming, p.SelfConsuming, p.Produced, p.Exported, p.Imported, p.Consumed, p.SelfConsumed)
}
