package config

type Config struct {
	ExportPath string
	FilePrefix string
	RestHost   string
	RestPort   string
	RestPath   string
	Protocol   string
}

func Load() *Config {
	return &Config{
		ExportPath: GetEnv("KPI_EXPORT_PATH", "/Users/bziswiler/Work/Private/HuaweiFusionSolar/export"),
		FilePrefix: GetEnv("KPI_FILE_PREFIX", "pv_export_<site>"),
		RestHost:   GetEnv("REST_HOST", "localhost"),
		RestPort:   GetEnv("REST_PORT", "8083"),
		RestPath:   GetEnv("REST_PATH", "/v1/read-latest-kpi"),
		Protocol:   GetEnv("REST_PROTOCOL", "http"),
	}
}
