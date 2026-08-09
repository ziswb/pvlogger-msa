package file

import (
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestCleanup_DeletesOldFiles(t *testing.T) {
	dir := t.TempDir()

	// Create an old file (> 48h)
	oldFile := filepath.Join(dir, "old.json")
	if err := os.WriteFile(oldFile, []byte("old"), 0644); err != nil {
		t.Fatal(err)
	}
	oldTime := time.Now().Add(-49 * time.Hour)
	if err := os.Chtimes(oldFile, oldTime, oldTime); err != nil {
		t.Fatal(err)
	}

	if err := Cleanup(dir); err != nil {
		t.Fatalf("Cleanup returned error: %v", err)
	}

	if _, err := os.Stat(oldFile); !os.IsNotExist(err) {
		t.Error("expected old file to be deleted")
	}
}

func TestCleanup_KeepsRecentFiles(t *testing.T) {
	dir := t.TempDir()

	// Create a recent file (< 48h)
	recentFile := filepath.Join(dir, "recent.json")
	if err := os.WriteFile(recentFile, []byte("recent"), 0644); err != nil {
		t.Fatal(err)
	}

	if err := Cleanup(dir); err != nil {
		t.Fatalf("Cleanup returned error: %v", err)
	}

	if _, err := os.Stat(recentFile); os.IsNotExist(err) {
		t.Error("expected recent file to be kept")
	}
}

func TestCleanup_KeepsDirectories(t *testing.T) {
	dir := t.TempDir()

	// Create a subdirectory with old mtime
	subDir := filepath.Join(dir, "subdir")
	if err := os.Mkdir(subDir, 0755); err != nil {
		t.Fatal(err)
	}
	oldTime := time.Now().Add(-49 * time.Hour)
	if err := os.Chtimes(subDir, oldTime, oldTime); err != nil {
		t.Fatal(err)
	}

	if err := Cleanup(dir); err != nil {
		t.Fatalf("Cleanup returned error: %v", err)
	}

	if _, err := os.Stat(subDir); os.IsNotExist(err) {
		t.Error("expected directory to be kept (only regular files deleted)")
	}
}

func TestCleanup_MixedFiles(t *testing.T) {
	dir := t.TempDir()

	// Old file
	oldFile := filepath.Join(dir, "old.json")
	if err := os.WriteFile(oldFile, []byte("old"), 0644); err != nil {
		t.Fatal(err)
	}
	oldTime := time.Now().Add(-72 * time.Hour)
	if err := os.Chtimes(oldFile, oldTime, oldTime); err != nil {
		t.Fatal(err)
	}

	// Recent file
	recentFile := filepath.Join(dir, "recent.json")
	if err := os.WriteFile(recentFile, []byte("recent"), 0644); err != nil {
		t.Fatal(err)
	}

	// File right at boundary (47h - should be kept)
	boundaryFile := filepath.Join(dir, "boundary.json")
	if err := os.WriteFile(boundaryFile, []byte("boundary"), 0644); err != nil {
		t.Fatal(err)
	}
	boundaryTime := time.Now().Add(-47 * time.Hour)
	if err := os.Chtimes(boundaryFile, boundaryTime, boundaryTime); err != nil {
		t.Fatal(err)
	}

	if err := Cleanup(dir); err != nil {
		t.Fatalf("Cleanup returned error: %v", err)
	}

	if _, err := os.Stat(oldFile); !os.IsNotExist(err) {
		t.Error("expected old file to be deleted")
	}
	if _, err := os.Stat(recentFile); os.IsNotExist(err) {
		t.Error("expected recent file to be kept")
	}
	if _, err := os.Stat(boundaryFile); os.IsNotExist(err) {
		t.Error("expected boundary file (47h) to be kept")
	}
}

func TestCleanup_EmptyDirectory(t *testing.T) {
	dir := t.TempDir()
	// Should not panic or error on empty directory
	if err := Cleanup(dir); err != nil {
		t.Fatalf("Cleanup returned error: %v", err)
	}
}
