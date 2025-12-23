package kpis

type KW struct {
	kw float64
}

type W struct {
	w float64
}

type KWh struct {
	kwh float64
}

type Wh struct {
	wh float64
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
	return W{o.w}
}

func (o W) ToKW() KW {
	return KW{o.w / 1000}
}

func (o KW) ToW() W {
	return W{o.kw * 1000}
}

func (o KW) ToKW() KW {
	return KW{o.kw}
}

func (o Wh) ToWh() Wh {
	return Wh{o.wh}
}

func (o Wh) ToKWh() KWh {
	return KWh{o.wh / 1000}
}

func (k KWh) ToWh() Wh {
	return Wh{k.kwh * 1000}
}

func (k KWh) ToKWh() KWh {
	return KWh{k.kwh}
}

func (k W) minus(o W) W {
	return W{k.w - o.w}
}

func (k W) plus(o W) W {
	return W{k.w + o.w}
}

func (k KWh) plus(o KWh) KWh {
	return KWh{k.kwh + o.kwh}
}

func (k KWh) minus(o KWh) KWh {
	return KWh{k.kwh - o.kwh}
}
