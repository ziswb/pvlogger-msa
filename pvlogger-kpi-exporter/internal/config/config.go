package config

type Config struct {
	ExportPath   string
	FilePrefix   string
	RestHost     string
	RestPort     string
	RestPath     string
	RestProtocol string
}

func Load() *Config {
	return &Config{
		ExportPath:   GetEnvChecked("KPI_EXPORT_PATH"),
		FilePrefix:   GetEnvChecked("KPI_FILE_PREFIX"),
		RestHost:     GetEnv("REST_HOST", "localhost"),
		RestPort:     GetEnv("REST_PORT", "8083"),
		RestPath:     GetEnv("REST_PATH", "/v1/read-latest-kpi"),
		RestProtocol: GetEnv("REST_PROTOCOL", "http"),
	}
}
