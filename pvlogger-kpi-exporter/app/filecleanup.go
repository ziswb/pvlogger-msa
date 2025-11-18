package main

import (
	"log"
	"os"
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
			log.Default().Println("Deleting file: " + f.Name())
			err := os.Remove(f.Name())
			if err != nil {
				log.Println(err)
			}
		}
	}
}
