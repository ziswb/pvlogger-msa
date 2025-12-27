package model

type RegisterDefinition struct {
	No              int             `json:"no"`
	Name            string          `json:"name"`
	ReadWrite       string          `json:"readWrite"`
	Type            string          `json:"type"`
	Unit            string          `json:"unit"`
	Gain            string          `json:"gain"`
	RegisterAddress RegisterAddress `json:"registerAddress"`
	Quantity        Quantity        `json:"quantity"`
	Scope           Scope           `json:"scope"`
	Category        string          `json:"category"`
}
