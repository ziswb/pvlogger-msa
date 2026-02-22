package model

type RequestId struct {
	Id UShort `json:"id"`
}

func NewRequestId(id UShort) RequestId {
	return RequestId{Id: id}
}
