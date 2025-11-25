package main

import (
	"log"
	"pvlogger/internal/auth"
	"pvlogger/internal/fusionsolar"
)

func main() {
	token, err := auth.Authenticate()
	if err != nil {
		log.Fatal(err)
	}
	rtd, err := fusionsolar.FetchStationRealTimeData(*token)
	if err != nil {
		log.Fatal(err)
	}
	err = fusionsolar.SaveResponse(rtd)
	if err != nil {
		log.Fatal(err)
	}
}
