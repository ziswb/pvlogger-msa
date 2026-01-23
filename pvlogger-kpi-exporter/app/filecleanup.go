package main

import (
	"os"
	"path/filepath"
	"time"

	"pvlogger-go-shared/logging"
)

const FortyEightHours = 48 * time.Hour

func shouldDelete(entry os.DirEntry) bool {
	info, err := entry.Info()
	if err != nil {
		logging.Fatal("failed to get file info", "error", err)
	}
	return info.Mode().IsRegular() && (time.Now().Sub(info.ModTime()) > FortyEightHours)
}

func cleanupFiles(path string) {
	dir, err := os.ReadDir(path)
	if err != nil {
		logging.Fatal("failed to read directory", "path", path, "error", err)
	}
	for _, f := range dir {
		if shouldDelete(f) {
			fp := filepath.Join(path, f.Name())
			logging.Info("deleting file", "path", fp)
			err := os.Remove(fp)
			if err != nil {
				logging.Error("failed to delete file", "path", fp, "error", err)
			}
		}
	}
}
