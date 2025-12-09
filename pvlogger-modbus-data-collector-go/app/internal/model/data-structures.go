package model

const CharSet = "ISO-8859-5"

type ModbusRegisterDefinition struct {
	No              int      `json:"no"`
	Name            string   `json:"name"`
	ReadWrite       string   `json:"readWrite"`
	Type            []string `json:"type"`
	Unit            string   `json:"unit"`
	Gain            string   `json:"gain"`
	RegisterAddress int      `json:"registerAddress"`
	Quantity        int      `json:"quantity"`
	Scope           string   `json:"scope"`
	Category        string   `json:"category"`
}
