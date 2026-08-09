package config

import (
	"net/url"

	env "pv-logger/config"
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
			Username: env.GetEnvChecked("USERNAME"),
			Password: env.GetEnvChecked("FUSIONSOLAR_PASSWORD"),
			Host:     env.GetEnv("FS_HOST", "eu5.fusionsolar.huawei.com"),
			Path:     env.GetEnv("FS_PATH", "thirdData"),
			Protocol: env.GetEnv("FS_PROTOCOL", "https"),
		},
		K8s: K8sConfig{
			Namespace:  env.GetEnv("K8S_NAMESPACE", "pvlogger"),
			SecretName: env.GetEnv("K8S_SECRET_NAME", "fusionsolar-token-secret"),
			TokenKey:   env.GetEnv("K8S_TOKEN_KEY", "FUSIONSOLAR_TOKEN"),
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

