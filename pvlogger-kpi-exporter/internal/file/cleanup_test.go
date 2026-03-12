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
	os.Chtimes(oldFile, oldTime, oldTime)

	Cleanup(dir)

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

	Cleanup(dir)

	if _, err := os.Stat(recentFile); os.IsNotExist(err) {
		t.Error("expected recent file to be kept")
	}
}

func TestCleanup_KeepsDirectories(t *testing.T) {
	dir := t.TempDir()

	// Create a subdirectory with old mtime
	subDir := filepath.Join(dir, "subdir")
	os.Mkdir(subDir, 0755)
	oldTime := time.Now().Add(-49 * time.Hour)
	os.Chtimes(subDir, oldTime, oldTime)

	Cleanup(dir)

	if _, err := os.Stat(subDir); os.IsNotExist(err) {
		t.Error("expected directory to be kept (only regular files deleted)")
	}
}

func TestCleanup_MixedFiles(t *testing.T) {
	dir := t.TempDir()

	// Old file
	oldFile := filepath.Join(dir, "old.json")
	os.WriteFile(oldFile, []byte("old"), 0644)
	oldTime := time.Now().Add(-72 * time.Hour)
	os.Chtimes(oldFile, oldTime, oldTime)

	// Recent file
	recentFile := filepath.Join(dir, "recent.json")
	os.WriteFile(recentFile, []byte("recent"), 0644)

	// File right at boundary (47h - should be kept)
	boundaryFile := filepath.Join(dir, "boundary.json")
	os.WriteFile(boundaryFile, []byte("boundary"), 0644)
	boundaryTime := time.Now().Add(-47 * time.Hour)
	os.Chtimes(boundaryFile, boundaryTime, boundaryTime)

	Cleanup(dir)

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
	Cleanup(dir)
}
