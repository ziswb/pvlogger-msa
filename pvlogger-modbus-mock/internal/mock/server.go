// Package mock serves the synthetic plant over Modbus TCP the way the
// SmartDongle does (ADR-0025): the dongle's own unit answers the aggregate
// live-power block, every other unit is the inverter with its meter.
package mock

import (
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net"
	"time"

	"pvlogger-modbus-mock/internal/plant"
)

const (
	fcReadHoldingRegisters = 0x03
	fcReadDeviceID         = 0x2B

	meiReadDeviceID = 0x0E

	exIllegalFunction  = 0x01
	exIllegalDataValue = 0x03

	// maxReadCount is the Modbus limit of registers per read request.
	maxReadCount = 125
)

// Server answers Modbus TCP requests with the plant's state at now().
type Server struct {
	plant      *plant.Plant
	dongleUnit uint8
	now        func() time.Time
}

// NewServer returns a server for p; dongleUnit is the unit ID of the dongle's
// aggregate block (DONGLE_DEVICE_ID of the collectors), now is its clock.
func NewServer(p *plant.Plant, dongleUnit uint8, now func() time.Time) *Server {
	return &Server{plant: p, dongleUnit: dongleUnit, now: now}
}

// Serve accepts connections on ln until it is closed.
func (s *Server) Serve(ln net.Listener) error {
	for {
		conn, err := ln.Accept()
		if err != nil {
			if errors.Is(err, net.ErrClosed) {
				return nil
			}
			return err
		}
		go s.handle(conn)
	}
}

// handle answers the requests of one connection in order.
func (s *Server) handle(conn net.Conn) {
	defer func() { _ = conn.Close() }()
	for {
		header := make([]byte, 7)
		if _, err := io.ReadFull(conn, header); err != nil {
			return
		}
		length := int(binary.BigEndian.Uint16(header[4:6]))
		if length < 2 {
			slog.Warn("dropping connection after malformed MBAP header", "length", length)
			return
		}
		pdu := make([]byte, length-1)
		if _, err := io.ReadFull(conn, pdu); err != nil {
			return
		}
		answer := s.answer(header[6], pdu)
		frame := make([]byte, 7, 7+len(answer))
		copy(frame, header[:4]) // transaction and protocol identifier
		binary.BigEndian.PutUint16(frame[4:], uint16(len(answer)+1))
		frame[6] = header[6]
		if _, err := conn.Write(append(frame, answer...)); err != nil {
			return
		}
	}
}

// answer returns the response PDU for a request PDU to unit.
func (s *Server) answer(unit uint8, pdu []byte) []byte {
	switch pdu[0] {
	case fcReadHoldingRegisters:
		if len(pdu) != 5 {
			return exception(pdu[0], exIllegalDataValue)
		}
		start := binary.BigEndian.Uint16(pdu[1:])
		count := binary.BigEndian.Uint16(pdu[3:])
		if count == 0 || count > maxReadCount {
			return exception(pdu[0], exIllegalDataValue)
		}
		words := s.registers(unit, s.plant.At(s.now()))
		out := []byte{fcReadHoldingRegisters, byte(2 * count)}
		for a := range count {
			out = binary.BigEndian.AppendUint16(out, words[start+a])
		}
		return out
	case fcReadDeviceID:
		if len(pdu) != 4 || pdu[1] != meiReadDeviceID {
			return exception(pdu[0], exIllegalDataValue)
		}
		objects := identification(pdu[2], pdu[3], unit)
		if objects == nil {
			return exception(pdu[0], exIllegalDataValue)
		}
		// conformity level = the requested code, no more objects follow
		out := []byte{fcReadDeviceID, meiReadDeviceID, pdu[2], pdu[2], 0x00, 0x00, byte(len(objects))}
		for _, o := range objects {
			out = append(out, o.id, byte(len(o.value)))
			out = append(out, o.value...)
		}
		return out
	default:
		slog.Info("unsupported function code", "function", fmt.Sprintf("0x%02x", pdu[0]))
		return exception(pdu[0], exIllegalFunction)
	}
}

func exception(function, code byte) []byte {
	return []byte{function | 0x80, code}
}
