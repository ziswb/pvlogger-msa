package response

import (
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"time"
)

func NewReadDeviceIdentifiersResponseFromBytes(
	jobId model.JobId,
	requestId model.RequestId,
	requestTime time.Time,
	header model.MbapHeader,
	bytes []byte,
) *ReadDeviceIdentifiersResponse {
	functionCode := model.NewFunctionCodeFromByte(bytes[0])
	meiType := bytes[1]
	readDevIdCode := bytes[2]
	consistencyLevel := bytes[3]
	more := bytes[4]
	nextObjectId := bytes[5]
	numberOfObjects := bytes[6]
	objectList := readObjectList(numberOfObjects, bytes[7:])
	return &ReadDeviceIdentifiersResponse{
		JobId:            jobId,
		RequestId:        requestId,
		RequestTime:      requestTime,
		ResponseTime:     time.Now(),
		Header:           header,
		FunctionCode:     functionCode,
		NumberOfBytes:    model.UByte(header.DataLength - 3),
		MeiType:          meiType,
		ReadDevIdCode:    readDevIdCode,
		ConsistencyLevel: consistencyLevel,
		More:             more,
		NextObjectId:     nextObjectId,
		NumberOfObjects:  numberOfObjects,
		ObjectList:       objectList,
	}
}

func readObjectList(numberOfObjects model.UByte, bytes []byte) []*ModbusObject {
	var objects []*ModbusObject
	if len(bytes) < 2 {
		logging.Fatal("not enough bytes for first pair of (objectId, objectLength) in object list")
	}
	pos := 0
	for range numberOfObjects {
		if len(bytes[pos:]) <= 3 {
			logging.Fatal("not enough bytes for object id, length and value in object list")
		}
		objectId := bytes[pos]
		pos++
		objectLength := bytes[pos]
		pos++
		if len(bytes[pos:]) < int(objectLength) {
			logging.Fatal("not enough bytes for object value in object list")
		} else {
			objects = append(objects, NewModbusObject(objectId, objectLength, bytes[pos:pos+int(objectLength)]))
			pos += int(objectLength)
		}
	}
	return objects
}
