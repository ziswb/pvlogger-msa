package kpis

type KW struct {
	Kw float64 `json:"kw"`
}

type W struct {
	W float64 `json:"w"`
}

type KWh struct {
	Kwh float64 `json:"kwh"`
}

type Wh struct {
	Wh float64 `json:"wh"`
}

func NewW(w float64) W {
	return W{w}
}

func NewWh(wh float64) Wh {
	return Wh{wh}
}

func NewKW(kw float64) KW {
	return KW{kw}
}

func NewKWh(kwh float64) KWh {
	return KWh{kwh}
}

func (o W) ToW() W {
	return W{o.W}
}

func (o W) ToKW() KW {
	return KW{o.W / 1000}
}

func (o KW) ToW() W {
	return W{o.Kw * 1000}
}

func (o KW) ToKW() KW {
	return KW{o.Kw}
}

func (o Wh) ToWh() Wh {
	return Wh{o.Wh}
}

func (o Wh) ToKWh() KWh {
	return KWh{o.Wh / 1000}
}

func (k KWh) ToWh() Wh {
	return Wh{k.Kwh * 1000}
}

func (k KWh) ToKWh() KWh {
	return KWh{k.Kwh}
}

func (k W) minus(o W) W {
	return W{k.W - o.W}
}

func (k W) plus(o W) W {
	return W{k.W + o.W}
}

func (k KWh) plus(o KWh) KWh {
	return KWh{k.Kwh + o.Kwh}
}

func (k KWh) minus(o KWh) KWh {
	return KWh{k.Kwh - o.Kwh}
}
