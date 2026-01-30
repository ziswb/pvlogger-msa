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
	return &Config{
		Job: JobConfig{
			Name: getEnv("JOB_NAME", "STATION_REAL_KPI"),
		},
		Fs: FusionSolarConfig{
			Username:    getEnv("USERNAME", "<fusionsolar-user>"),
			Password:    getEnvChecked("FUSIONSOLAR_PASSWORD"),
			Host:        getEnv("FS_HOST", "eu5.fusionsolar.huawei.com"),
			Path:        getEnv("FS_PATH", "thirdData"),
			Protocol:    getEnv("FS_PROTOCOL", "https"),
			StationCode: getEnv("FS_STATION_CODE", "NE=<station-code>"),
			DevIds:      getEnv("FS_DEV_TYPE_ID", "<dev-id>,<dev-id>"),
		},
		Database: DatabaseConfig{
			Username: getEnv("POSTGRESQL_USERNAME", "postgres"),
			Password: getEnvChecked("POSTGRES_PASSWORD"),
			Host:     getEnv("POSTGRESQL_HOST", "<central-host>"),
			Port:     getIntEnv("POSTGRESQL_PORT", "5433"),
			DbName:   getEnv("DB_NAME", "fsdata"),
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
