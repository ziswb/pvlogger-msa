package main

import (
	"log"
	"pvlogger-modbus-data-collector-go/app/internal/services"
)

func main() {
	log.Default().Println("Starting ...")
	services.Execute()
	log.Default().Println("Finished")
}
