package file

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"

	"pv-logger/logging"
)

func WriteJSON(data any, dir string, fileName string) error {
	absolutePath := filepath.Join(dir, fileName+".json")
	logging.Info("writing KPIs to file", "path", absolutePath)
	err := os.MkdirAll(dir, 0755)
	if err != nil {
		return fmt.Errorf("failed to create directory %s: %w", dir, err)
	}

	file, err := os.Create(absolutePath)
	if err != nil {
		return fmt.Errorf("failed to create file %s: %w", absolutePath, err)
	}
	logging.Debug("created file", "path", file.Name())

	encoder := json.NewEncoder(file)
	encoder.SetIndent("", "  ")
	if err := encoder.Encode(data); err != nil {
		if closeErr := file.Close(); closeErr != nil {
			logging.Error("failed to close file", "path", file.Name(), "error", closeErr)
		}
		return fmt.Errorf("failed to encode KPIs: %w", err)
	}
	logging.Debug("encoded KPIs to file", "path", file.Name())
	if err := file.Close(); err != nil {
		return fmt.Errorf("failed to close file %s: %w", file.Name(), err)
	}
	return nil
}
