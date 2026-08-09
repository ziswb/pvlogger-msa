package file

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
)

func TestWriteJSON_CreatesFile(t *testing.T) {
	dir := t.TempDir()
	data := map[string]string{"key": "value"}

	if err := WriteJSON(&data, dir, "test-output"); err != nil {
		t.Fatalf("WriteJSON returned error: %v", err)
	}

	path := filepath.Join(dir, "test-output.json")
	if _, err := os.Stat(path); os.IsNotExist(err) {
		t.Fatal("expected file to be created")
	}
}

func TestWriteJSON_ValidJSON(t *testing.T) {
	dir := t.TempDir()
	data := map[string]string{"hello": "world"}

	if err := WriteJSON(&data, dir, "test-json"); err != nil {
		t.Fatalf("WriteJSON returned error: %v", err)
	}

	path := filepath.Join(dir, "test-json.json")
	content, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("failed to read file: %v", err)
	}

	var result map[string]string
	if err := json.Unmarshal(content, &result); err != nil {
		t.Fatalf("output is not valid JSON: %v", err)
	}
	if result["hello"] != "world" {
		t.Errorf("expected 'world', got '%s'", result["hello"])
	}
}

func TestWriteJSON_IndentedOutput(t *testing.T) {
	dir := t.TempDir()
	data := map[string]string{"key": "value"}

	if err := WriteJSON(&data, dir, "test-indent"); err != nil {
		t.Fatalf("WriteJSON returned error: %v", err)
	}

	path := filepath.Join(dir, "test-indent.json")
	content, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}

	// Should contain 2-space indentation
	expected := "{\n  \"key\": \"value\"\n}\n"
	if string(content) != expected {
		t.Errorf("expected indented JSON:\n%s\ngot:\n%s", expected, string(content))
	}
}

func TestWriteJSON_CreatesDirectory(t *testing.T) {
	dir := filepath.Join(t.TempDir(), "nested", "dir")

	data := map[string]int{"n": 42}
	if err := WriteJSON(&data, dir, "test-mkdir"); err != nil {
		t.Fatalf("WriteJSON returned error: %v", err)
	}

	path := filepath.Join(dir, "test-mkdir.json")
	if _, err := os.Stat(path); os.IsNotExist(err) {
		t.Fatal("expected nested directory and file to be created")
	}
}

func TestWriteJSON_StructData(t *testing.T) {
	dir := t.TempDir()

	type TestStruct struct {
		Name  string  `json:"name"`
		Value float64 `json:"value"`
	}
	data := TestStruct{Name: "test", Value: 3.14}

	if err := WriteJSON(&data, dir, "test-struct"); err != nil {
		t.Fatalf("WriteJSON returned error: %v", err)
	}

	path := filepath.Join(dir, "test-struct.json")
	content, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}

	var result TestStruct
	if err := json.Unmarshal(content, &result); err != nil {
		t.Fatal(err)
	}
	if result.Name != "test" || result.Value != 3.14 {
		t.Errorf("unexpected result: %+v", result)
	}
}
