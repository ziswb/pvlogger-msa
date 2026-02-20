package model

type RegisterDefinition struct {
	Name            string          `json:"name"`
	Type            string          `json:"type"`
	Unit            string          `json:"unit"`
	Gain            string          `json:"gain"`
	RegisterAddress RegisterAddress `json:"registerAddress"`
	Quantity        Quantity        `json:"quantity"`
}
