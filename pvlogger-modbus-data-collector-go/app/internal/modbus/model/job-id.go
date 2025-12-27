package model

type JobId struct {
	Id string `json:"id"`
}

func NewJobId(id string) JobId {
	return JobId{Id: id}
}
