package config

import (
	env "pv-logger/config"
)

// Modes of the one exporter binary (KPI_EXPORTER_MODE).
const (
	ModeExport = "export" // CronJob: fetch, write file, clean up (default)
	ModeServe  = "serve"  // cluster: token-protected export endpoint
	ModeRelay  = "relay"  // receiver's server: fetch from serve over Tailscale, write file
)

type Config struct {
	ExportPath   string
	FilePrefix   string
	KeyPrefix    string // the receiver's JSON key prefix (ADR-0021)
	RestHost     string
	RestPort     string
	RestPath     string
	RestProtocol string

	// serve and relay
	HTTPPort string
	// serve
	TokenFile string
	// relay
	UpstreamURL   string
	UpstreamToken string
	TSAuthKey     string
	TSHostname    string
	TSStateDir    string
}

func Mode() string {
	return env.GetEnv("KPI_EXPORTER_MODE", ModeExport)
}

func dataAccessor(c *Config) {
	c.RestHost = env.GetEnv("REST_HOST", "localhost")
	c.RestPort = env.GetEnv("REST_PORT", "8083")
	c.RestPath = env.GetEnv("REST_PATH", "/v1/read-latest-kpi")
	c.RestProtocol = env.GetEnv("REST_PROTOCOL", "http")
}

// Load is the configuration of the export (CronJob) mode.
func Load() *Config {
	c := &Config{
		ExportPath: env.GetEnvChecked("KPI_EXPORT_PATH"),
		FilePrefix: env.GetEnvChecked("KPI_FILE_PREFIX"),
		KeyPrefix:  env.GetEnvChecked("KPI_KEY_PREFIX"),
	}
	dataAccessor(c)
	return c
}

// LoadServe is the configuration of the cluster-side export endpoint.
func LoadServe() *Config {
	c := &Config{
		FilePrefix: env.GetEnvChecked("KPI_FILE_PREFIX"),
		KeyPrefix:  env.GetEnvChecked("KPI_KEY_PREFIX"),
		HTTPPort:   env.GetEnv("HTTP_PORT", "8080"),
		TokenFile:  env.GetEnv("KPI_EXPORT_TOKEN_FILE", "/etc/pvlogger-export/token"),
	}
	dataAccessor(c)
	return c
}

// LoadRelay is the configuration of the container on the receiver's server.
// Without TS_AUTHKEY it reaches UPSTREAM_URL over the host network (local
// testing); with it, over its own Tailscale node.
func LoadRelay() *Config {
	return &Config{
		ExportPath:    env.GetEnvChecked("KPI_EXPORT_PATH"),
		HTTPPort:      env.GetEnv("HTTP_PORT", "8080"),
		UpstreamURL:   env.GetEnvChecked("UPSTREAM_URL"),
		UpstreamToken: env.GetEnvChecked("UPSTREAM_TOKEN"),
		TSAuthKey:     env.GetEnv("TS_AUTHKEY", ""),
		TSHostname:    env.GetEnv("TS_HOSTNAME", "pvlogger-relay"),
		TSStateDir:    env.GetEnv("TS_STATE_DIR", "/var/lib/pvlogger-relay"),
	}
}
