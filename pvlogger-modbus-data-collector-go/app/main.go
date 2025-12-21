package main

import (
	"log"
	"pvlogger-modbus-data-collector-go/app/internal/services"
)

func main() {
	log.Default().Println("Starting ...")
	//host := config.MdConfig().Host
	//port := config.MdConfig().Port
	//proto := config.MdConfig().Protocol
	//definitions, err := services.NewMasterDataService(services.MasterDataConfig{Protocol: proto, Host: host, Port: port}).FetchRegisterDefinitions([]string{"METER"}, []string{"DYNAMIC"})
	//if err != nil {
	//	return
	//}
	//log.Default().Println(definitions)
	services.Execute()
}
