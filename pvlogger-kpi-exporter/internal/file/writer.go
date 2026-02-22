package file

import (
	"encoding/json"
	"os"
	"path/filepath"

	"pv-logger/logging"
)

func WriteJSON(data any, dir string, fileName string) {
	absolutePath := filepath.Join(dir, fileName+".json")
	logging.Info("writing KPIs to file", "path", absolutePath)
	err := os.MkdirAll(dir, 0755)
	if err != nil {
		logging.Fatal("failed to create directory", "path", dir, "error", err)
		return
	}

	file, err := os.Create(absolutePath)
	if err != nil {
		logging.Fatal("failed to create file", "path", absolutePath, "error", err)
		return
	}
	logging.Debug("created file", "path", file.Name())
	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			logging.Fatal("failed to close file", "path", file.Name(), "error", err)
		}
	}(file)

	encoder := json.NewEncoder(file)
	encoder.SetIndent("", "  ")
	err = encoder.Encode(data)
	if err != nil {
		logging.Fatal("failed to encode KPIs", "error", err)
	}
	logging.Debug("encoded KPIs to file", "path", file.Name())
}
