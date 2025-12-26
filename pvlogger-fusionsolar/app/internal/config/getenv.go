package config

import (
	"os"
	"strconv"
)

type FsJobConfig struct {
	JobName string
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

func JobConfig() FsJobConfig {
	return FsJobConfig{
		JobName: getEnv("JOB_NAME", "STATION_REAL_KPI"),
	}
}

func FsConfig() FusionSolarConfig {
	return FusionSolarConfig{
		Username:    getEnv("USERNAME", "<fusionsolar-user>"),
		Password:    getEnvChecked("FUSIONSOLAR_PASSWORD"),
		Host:        getEnv("FS_HOST", "eu5.fusionsolar.huawei.com"),
		Path:        getEnv("FS_PATH", "/thirdData"),
		Protocol:    getEnv("FS_PROTOCOL", "https"),
		StationCode: getEnv("FS_STATION_CODE", "NE=<station-code>"),
		DevIds:      getEnv("FS_DEV_TYPE_ID", "<dev-id>,<dev-id>"),
	}
}

func DbConfig() DatabaseConfig {
	return DatabaseConfig{
		Username: getEnv("DB_USERNAME", "postgres"),
		Password: getEnvChecked("DB_PASSWORD"),
		Host:     getEnv("POSTGRESQL_HOST", "<central-host>"),
		Port:     getIntEnv("POSTGRESQL_PORT", "5433"),
		DbName:   getEnv("DB_NAME", "fsdata"),
	}
}

func FsUrl() string {
	return FsConfig().Protocol + "://" + FsConfig().Host + FsConfig().Path
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
