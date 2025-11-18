package main

import (
	"encoding/json"
	"io"
	"log"
	"net/http"
)

func fetchKpis() *KpiValues {
	host := getEnv("REST_HOST", "localhost")
	port := getEnv("REST_PORT", "8083")
	path := getEnv("REST_PATH", "/v1/read-latest-kpi")
	protocol := getEnv("REST_PROTOCOL", "http")
	url := protocol + "://" + host + ":" + port + path

	resp, err := http.Get(url)
	if err != nil {
		log.Fatal(err)
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			log.Default().Println(err.Error())
		}
	}(resp.Body)
	log.Default().Println("Response status: " + resp.Status)
	body, readErr := io.ReadAll(resp.Body)
	log.Default().Println("Received message: " + string(body))
	if readErr != nil {
		log.Fatal(readErr)
	}
	kpiValues := KpiValues{}
	err = json.Unmarshal(body, &kpiValues)
	if err != nil {
		log.Fatal(err)
	}
	return &kpiValues
}
