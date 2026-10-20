package file

import (
	"bytes"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"

	"pv-logger/logging"
)

// Encode renders data exactly as the export files have always been written:
// two-space indent and a trailing newline.
func Encode(data any) ([]byte, error) {
	var buf bytes.Buffer
	encoder := json.NewEncoder(&buf)
	encoder.SetIndent("", "  ")
	if err := encoder.Encode(data); err != nil {
		return nil, fmt.Errorf("failed to encode KPIs: %w", err)
	}
	return buf.Bytes(), nil
}

// WriteJSON writes data as <dir>/<fileName>.json.
func WriteJSON(data any, dir string, fileName string) error {
	content, err := Encode(data)
	if err != nil {
		return err
	}
	return WriteBytes(content, dir, fileName+".json")
}

// WriteBytes writes content to <dir>/<fileName> through a temporary file and a
// rename, so a reader never sees a half-written file.
func WriteBytes(content []byte, dir string, fileName string) error {
	absolutePath := filepath.Join(dir, fileName)
	logging.Info("writing KPIs to file", "path", absolutePath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return fmt.Errorf("failed to create directory %s: %w", dir, err)
	}
	tmp, err := os.CreateTemp(dir, ".tmp-"+fileName+"-*")
	if err != nil {
		return fmt.Errorf("failed to create temporary file in %s: %w", dir, err)
	}
	tmpName := tmp.Name()
	defer func() { _ = os.Remove(tmpName) }() // no-op once renamed
	if _, err := tmp.Write(content); err != nil {
		_ = tmp.Close()
		return fmt.Errorf("failed to write %s: %w", tmpName, err)
	}
	if err := tmp.Close(); err != nil {
		return fmt.Errorf("failed to close %s: %w", tmpName, err)
	}
	if err := os.Chmod(tmpName, 0644); err != nil {
		return fmt.Errorf("failed to set permissions on %s: %w", tmpName, err)
	}
	if err := os.Rename(tmpName, absolutePath); err != nil {
		return fmt.Errorf("failed to move %s to %s: %w", tmpName, absolutePath, err)
	}
	return nil
}
