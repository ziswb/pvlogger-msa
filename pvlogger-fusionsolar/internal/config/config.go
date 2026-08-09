package config

import (
	"net/url"

	env "pv-logger/config"
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
	useTokenSecret := env.GetEnv("USE_TOKEN_SECRET", "true") == "true"
	return &Config{
		Job: JobConfig{
			Name: env.GetEnv("JOB_NAME", "STATION_REAL_KPI"),
		},
		Fs: FusionSolarConfig{
			Username:    env.GetEnvChecked("USERNAME"),
			Password:    env.GetEnvOptional("FUSIONSOLAR_PASSWORD", useTokenSecret),
			Host:        env.GetEnv("FS_HOST", "eu5.fusionsolar.huawei.com"),
			Path:        env.GetEnv("FS_PATH", "thirdData"),
			Protocol:    env.GetEnv("FS_PROTOCOL", "https"),
			StationCode: env.GetEnvChecked("FS_STATION_CODE"),
			DevIds:      env.GetEnvChecked("FS_DEV_TYPE_ID"),
		},
		Database: DatabaseConfig{
			Username: env.GetEnv("POSTGRESQL_USERNAME", "postgres"),
			Password: env.GetEnvChecked("POSTGRES_PASSWORD"),
			Host:     env.GetEnvChecked("POSTGRESQL_HOST"),
			Port:     env.GetIntEnv("POSTGRESQL_PORT", "5434"),
			DbName:   env.GetEnv("DB_NAME", "fsdata"),
		},
		K8s: K8sConfig{
			Namespace:       env.GetEnv("K8S_NAMESPACE", "pvlogger"),
			TokenSecretName: env.GetEnv("K8S_TOKEN_SECRET_NAME", "fusionsolar-token-secret"),
			TokenKey:        env.GetEnv("K8S_TOKEN_KEY", "FUSIONSOLAR_TOKEN"),
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

