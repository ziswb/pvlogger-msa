package config

import (
	"net/url"
	"os"
	"strconv"
)

type Config struct {
	Job      JobConfig
	Fs       FusionSolarConfig
	Database DatabaseConfig
	K8s      K8sConfig
}

type K8sConfig struct {
	Namespace       string
	TokenSecretName string
	TokenKey        string
	UseTokenSecret  bool
}

type JobConfig struct {
	Name string
}

type FusionSolarConfig struct {
	Username    string
	Password    string
	Host        string
	Path        string
	Protocol    string
	StationCode string
	DevIds      string
}

type DatabaseConfig struct {
	Username string
	Password string
	Host     string
	Port     int
	DbName   string
}

func Load() *Config {
	useTokenSecret := getEnv("USE_TOKEN_SECRET", "true") == "true"
	return &Config{
		Job: JobConfig{
			Name: getEnv("JOB_NAME", "STATION_REAL_KPI"),
		},
		Fs: FusionSolarConfig{
			Username:    getEnvChecked("USERNAME"),
			Password:    getEnvOptional("FUSIONSOLAR_PASSWORD", useTokenSecret),
			Host:        getEnv("FS_HOST", "eu5.fusionsolar.huawei.com"),
			Path:        getEnv("FS_PATH", "thirdData"),
			Protocol:    getEnv("FS_PROTOCOL", "https"),
			StationCode: getEnvChecked("FS_STATION_CODE"),
			DevIds:      getEnvChecked("FS_DEV_TYPE_ID"),
		},
		Database: DatabaseConfig{
			Username: getEnv("POSTGRESQL_USERNAME", "postgres"),
			Password: getEnvChecked("POSTGRES_PASSWORD"),
			Host:     getEnvChecked("POSTGRESQL_HOST"),
			Port:     getIntEnv("POSTGRESQL_PORT", "5434"),
			DbName:   getEnv("DB_NAME", "fsdata"),
		},
		K8s: K8sConfig{
			Namespace:       getEnv("K8S_NAMESPACE", "pvlogger"),
			TokenSecretName: getEnv("K8S_TOKEN_SECRET_NAME", "fusionsolar-token-secret"),
			TokenKey:        getEnv("K8S_TOKEN_KEY", "FUSIONSOLAR_TOKEN"),
			UseTokenSecret:  useTokenSecret,
		},
	}
}

func (c *Config) FsUrl() string {
	u := &url.URL{
		Scheme: c.Fs.Protocol,
		Host:   c.Fs.Host,
		Path:   c.Fs.Path,
	}
	return u.String()
}

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if len(value) == 0 {
		return fallback
	}
	return value
}

func getIntEnv(key, fallback string) int {
	value, err := strconv.Atoi(getEnv(key, fallback))
	if err != nil {
		panic(err)
	}
	return value
}

func getEnvChecked(key string) string {
	value := os.Getenv(key)
	if len(value) == 0 {
		panic("Missing environment variable: " + key)
	}
	return value
}

func getEnvOptional(key string, optional bool) string {
	value := os.Getenv(key)
	if len(value) == 0 && !optional {
		panic("Missing environment variable: " + key)
	}
	return value
}
