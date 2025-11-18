package main

import (
	"encoding/json"
	"log"
	"os"
)

func writeKpisToJsonFile(kpiRecord *KpiExportRecord, path string, fileName string) {
	absolutePath := path + "/" + fileName + ".json"
	log.Default().Println("Will write KPIs to file: " + absolutePath)
	err := os.MkdirAll(path, 0755)
	if err != nil {
		log.Fatal(err)
		return
	}

	file, err := os.Create(absolutePath)
	log.Default().Println("Created file: " + file.Name())
	if err != nil {
		log.Fatal(err)
		return
	}
	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			log.Fatal(err)
		}
	}(file)

	encoder := json.NewEncoder(file)
	encoder.SetIndent("", "  ")
	err = encoder.Encode(kpiRecord)
	log.Default().Println("Encoded KPIs to file: " + file.Name())
	if err != nil {
		log.Fatal(err)
	}
}
