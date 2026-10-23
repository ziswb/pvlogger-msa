package services

import (
	"errors"
	"pvlogger-shared-modbus/model"
	"slices"
	"testing"
)

func TestRunSweepAggregatesTheRunItRead(t *testing.T) {
	var got []model.JobId
	err := runSweep(
		func() (*model.JobId, error) {
			id := model.NewJobId("run-1")
			return &id, nil
		},
		func(jobId model.JobId) error {
			got = append(got, jobId)
			return nil
		},
	)
	if err != nil {
		t.Fatalf("runSweep() error = %v", err)
	}
	if want := []model.JobId{model.NewJobId("run-1")}; !slices.Equal(got, want) {
		t.Errorf("aggregated %v, want %v", got, want)
	}
}

func TestRunSweepSkipsAggregationWhenTheReadFails(t *testing.T) {
	readErr := errors.New("modbus unreachable")
	aggregated := false
	err := runSweep(
		func() (*model.JobId, error) { return nil, readErr },
		func(model.JobId) error {
			aggregated = true
			return nil
		},
	)
	if !errors.Is(err, readErr) {
		t.Errorf("runSweep() error = %v, want %v", err, readErr)
	}
	if aggregated {
		t.Error("aggregated after a failed read")
	}
}

func TestRunSweepReturnsTheAggregationError(t *testing.T) {
	// Today's normal case until masterdata has a DONGLE device type (backlog 35):
	// the run must exit non-zero, not swallow it.
	aggErr := errors.New("no dongle block in run")
	err := runSweep(
		func() (*model.JobId, error) {
			id := model.NewJobId("run-2")
			return &id, nil
		},
		func(model.JobId) error { return aggErr },
	)
	if !errors.Is(err, aggErr) {
		t.Errorf("runSweep() error = %v, want %v", err, aggErr)
	}
}

func TestSweepReadsDynamicAndAlarmRegisters(t *testing.T) {
	want := []model.Category{model.DynamicCategory, model.AlarmCategory}
	if !slices.Equal(sweepCategories, want) {
		t.Errorf("sweepCategories = %v, want %v", sweepCategories, want)
	}
}
