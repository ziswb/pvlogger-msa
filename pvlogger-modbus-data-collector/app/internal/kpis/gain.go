package kpis

type Gain int

const (
	ONE Gain = iota + 1
	TEN
	HUNDRED
	THOUSAND
	ASTERISK
)

var gainName = map[string]Gain{
	"ONE":      ONE,
	"TEN":      TEN,
	"HUNDRED":  HUNDRED,
	"THOUSAND": THOUSAND,
	"ASTERISK": ASTERISK,
}

var gainValue = map[Gain]string{
	ONE:      "1",
	TEN:      "10",
	HUNDRED:  "100",
	THOUSAND: "1000",
	ASTERISK: "1",
}

func (g Gain) String() string {
	return gainValue[g]
}

func NewGainFromString(text string) Gain {
	gain := gainName[text]
	if gain != 0 {
		return gain
	}
	return ASTERISK
}
