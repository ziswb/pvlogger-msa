package file

import (
	"fmt"
	"os"
	"path/filepath"
	"time"

	"pv-logger/logging"
)

const FortyEightHours = 48 * time.Hour

func shouldDelete(entry os.DirEntry) (bool, error) {
	info, err := entry.Info()
	if err != nil {
		return false, fmt.Errorf("failed to get file info: %w", err)
	}
	return info.Mode().IsRegular() && (time.Since(info.ModTime()) > FortyEightHours), nil
}

func Cleanup(path string) error {
	dir, err := os.ReadDir(path)
	if err != nil {
		return fmt.Errorf("failed to read directory %s: %w", path, err)
	}
	for _, f := range dir {
		delete, err := shouldDelete(f)
		if err != nil {
			return err
		}
		if delete {
			fp := filepath.Join(path, f.Name())
			logging.Info("deleting file", "path", fp)
			err := os.Remove(fp)
			if err != nil {
				logging.Error("failed to delete file", "path", fp, "error", err)
			}
		}
	}
	return nil
}
