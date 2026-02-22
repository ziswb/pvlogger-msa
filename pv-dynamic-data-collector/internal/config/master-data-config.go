package config

type MasterDataConfig struct {
	Host           string
	Protocol       string
	Port           int
	MasterDataPath string
}

func MdConfig() *MasterDataConfig {
	return &MasterDataConfig{
		Host:           getEnv("MD_HOST", "localhost"),
		Port:           getIntEnv("MD_PORT", "8081"),
		Protocol:       getEnv("MD_PROTOCOL", "http"),
		MasterDataPath: getEnv("MD_PATH", "master-data"),
	}
}
