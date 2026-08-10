package config

import "pv-logger/config"

// REST path of the masterdata API — API contract, not deployment config
// (ADR-0012). Source of truth are the Kotlin route constants in
// masterdata-server (MasterDataPaths.kt); paths_drift_test.go fails on drift.
const MasterDataPath = "master-data"

type MasterDataConfig struct {
	Host     string
	Protocol string
	Port     int
}

func MdConfig() *MasterDataConfig {
	return &MasterDataConfig{
		Host:     config.GetEnv("MD_HOST", "localhost"),
		Port:     config.GetIntEnv("MD_PORT", "8081"),
		Protocol: config.GetEnv("MD_PROTOCOL", "http"),
	}
}
