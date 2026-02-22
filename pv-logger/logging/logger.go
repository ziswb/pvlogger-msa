package logging

import (
	"context"
	"log/slog"
	"os"
	"strings"
)

var logger *slog.Logger

func init() {
	level := parseLogLevel(os.Getenv("LOG_LEVEL"))
	handler := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: level,
	})
	logger = slog.New(handler)
	slog.SetDefault(logger)
}

func parseLogLevel(levelStr string) slog.Level {
	switch strings.ToUpper(levelStr) {
	case "DEBUG":
		return slog.LevelDebug
	case "INFO":
		return slog.LevelInfo
	case "WARN", "WARNING":
		return slog.LevelWarn
	case "ERROR":
		return slog.LevelError
	default:
		return slog.LevelInfo
	}
}

func DebugEnabled() bool {
	return Logger().Enabled(context.Background(), slog.LevelDebug)
}

func Logger() *slog.Logger {
	return logger
}

func Debug(msg string, args ...any) {
	logger.Debug(msg, args...)
}

func Info(msg string, args ...any) {
	logger.Info(msg, args...)
}

func Warn(msg string, args ...any) {
	logger.Warn(msg, args...)
}

func Error(msg string, args ...any) {
	logger.Error(msg, args...)
}

func Fatal(msg string, args ...any) {
	logger.Error(msg, args...)
	os.Exit(1)
}
