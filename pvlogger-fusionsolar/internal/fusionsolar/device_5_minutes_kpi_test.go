package fusionsolar

import (
	"testing"
	"time"
)

func TestDetermineNextCollectionDate_EmptyMap(t *testing.T) {
	result := determineNextCollectionDate(map[time.Time]int{})
	if result == nil {
		t.Fatal("expected non-nil result for empty map")
	}
	expected := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	if !result.Equal(expected) {
		t.Errorf("expected %v, got %v", expected, *result)
	}
}

func TestDetermineNextCollectionDate_AllDaysComplete(t *testing.T) {
	records := make(map[time.Time]int)
	walker := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	for walker.Before(time.Now()) {
		records[walker] = 200
		walker = walker.Add(time.Hour * 24)
	}
	result := determineNextCollectionDate(records)
	if result != nil {
		t.Errorf("expected nil when all days are complete, got %v", *result)
	}
}

func TestDetermineNextCollectionDate_FirstDayIncomplete(t *testing.T) {
	records := make(map[time.Time]int)
	walker := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	for walker.Before(time.Now()) {
		records[walker] = 200
		walker = walker.Add(time.Hour * 24)
	}
	// Make the first day incomplete
	firstDay := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	records[firstDay] = 100

	result := determineNextCollectionDate(records)
	if result == nil {
		t.Fatal("expected non-nil result")
	}
	if !result.Equal(firstDay) {
		t.Errorf("expected %v, got %v", firstDay, *result)
	}
}

func TestDetermineNextCollectionDate_GapInMiddle(t *testing.T) {
	records := make(map[time.Time]int)
	walker := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	gapDate := time.Date(2023, 3, 15, 0, 0, 0, 0, time.UTC)

	for walker.Before(time.Now()) {
		if walker.Equal(gapDate) {
			records[walker] = 50
		} else {
			records[walker] = 200
		}
		walker = walker.Add(time.Hour * 24)
	}

	result := determineNextCollectionDate(records)
	if result == nil {
		t.Fatal("expected non-nil result")
	}
	if !result.Equal(gapDate) {
		t.Errorf("expected gap date %v, got %v", gapDate, *result)
	}
}

func TestDetermineNextCollectionDate_Exactly200IsComplete(t *testing.T) {
	records := make(map[time.Time]int)
	day := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	records[day] = 200

	nextDay := day.Add(time.Hour * 24)

	result := determineNextCollectionDate(records)
	if result == nil {
		t.Fatal("expected non-nil result")
	}
	// First day has exactly 200 (not < 200), so it should move to the next day
	if !result.Equal(nextDay) {
		t.Errorf("expected %v, got %v", nextDay, *result)
	}
}

func TestDetermineNextCollectionDate_199IsIncomplete(t *testing.T) {
	records := make(map[time.Time]int)
	day := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	records[day] = 199

	result := determineNextCollectionDate(records)
	if result == nil {
		t.Fatal("expected non-nil result")
	}
	if !result.Equal(day) {
		t.Errorf("expected %v, got %v", day, *result)
	}
}

func TestDetermineNextCollectionDate_NilMap(t *testing.T) {
	result := determineNextCollectionDate(nil)
	if result == nil {
		t.Fatal("expected non-nil result for nil map")
	}
	expected := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC)
	if !result.Equal(expected) {
		t.Errorf("expected %v, got %v", expected, *result)
	}
}

func TestDetermineNextCollectionDate_ReturnsUTCTime(t *testing.T) {
	result := determineNextCollectionDate(map[time.Time]int{})
	if result == nil {
		t.Fatal("expected non-nil result")
	}
	if result.Location() != time.UTC {
		t.Errorf("expected UTC timezone, got %v", result.Location())
	}
}
