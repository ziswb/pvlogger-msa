package main

import (
	"log"
	"os"
	"path/filepath"
	"time"
)

const FortyEightHours = 48 * time.Hour

func shouldDelete(entry os.DirEntry) bool {
	info, err := entry.Info()
	if err != nil {
		log.Fatal(err)
	}
	return info.Mode().IsRegular() && (time.Now().Sub(info.ModTime()) > FortyEightHours)
}

func cleanupFiles(path string) {
	dir, err := os.ReadDir(path)
	if err != nil {
		log.Fatal(err)
	}
	for _, f := range dir {
		if shouldDelete(f) {
			fp := filepath.Join(path, f.Name())
			log.Default().Println("Deleting file: " + fp)
			err := os.Remove(fp)
			if err != nil {
				log.Println(err)
			}
		}
	}
}
