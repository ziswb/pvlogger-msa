package config

import (
	"net/url"
	"os"
)

type Config struct {
	FusionSolar FusionSolarConfig
	K8s         K8sConfig
}

type FusionSolarConfig struct {
	Username string
	Password string
	Host     string
	Path     string
	Protocol string
}

type K8sConfig struct {
	Namespace  string
	SecretName string
	TokenKey   string
}

func Load() *Config {
	return &Config{
		FusionSolar: FusionSolarConfig{
			Username: getEnvChecked("USERNAME"),
			Password: getEnvChecked("FUSIONSOLAR_PASSWORD"),
			Host:     getEnv("FS_HOST", "eu5.fusionsolar.huawei.com"),
			Path:     getEnv("FS_PATH", "thirdData"),
			Protocol: getEnv("FS_PROTOCOL", "https"),
		},
		K8s: K8sConfig{
			Namespace:  getEnv("K8S_NAMESPACE", "pvlogger"),
			SecretName: getEnv("K8S_SECRET_NAME", "fusionsolar-token-secret"),
			TokenKey:   getEnv("K8S_TOKEN_KEY", "FUSIONSOLAR_TOKEN"),
		},
	}
}

func (c *Config) FsUrl() string {
	u := &url.URL{
		Scheme: c.FusionSolar.Protocol,
		Host:   c.FusionSolar.Host,
		Path:   c.FusionSolar.Path,
	}
	return u.String()
}

func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}

func getEnvChecked(key string) string {
	value, exists := os.LookupEnv(key)
	if !exists || value == "" {
		panic("required environment variable " + key + " is not set")
	}
	return value
}
