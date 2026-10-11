#!/bin/sh
set -x
curl -s -X 'GET' 'http://localhost:8080/health' -H 'accept: */*' | jq
sleep 1
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-01", "instruction": "Pick up a tube from the red rack."}'
sleep 2
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-01' -H 'accept: */*' | jq
sleep 4
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-01' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-02", "instruction": "Pick up a tube from the red rack."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-02' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-03", "instruction": "Scan the barcode of the tube you are holding."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-03' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-04", "instruction": "Place the tube you are holding in an open spot in the blue rack."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-04' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-05", "instruction": "Pick up a tube from the red rack."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-05' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-06", "instruction": "Scan the barcode of the tube you are holding."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-06' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-07", "instruction": "Place the tube you are holding in an open spot in the blue rack."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-07' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-08", "instruction": "Pick up a tube from the red rack."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-08' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-09", "instruction": "Scan the barcode of the tube you are holding."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-09' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-10", "instruction": "Place the tube you are holding in an open spot in the blue rack."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-10' -H 'accept: */*' | jq
sleep 6
curl -w '\n' -s -X 'POST' 'http://localhost:8080/run/run-20261010/steps' -H 'accept: */*' -H 'Content-Type: application/json' -d '{"stepId": "step-11", "instruction": "Pick up a tube from the red rack."}'
sleep 6
curl -s -X 'GET' 'http://localhost:8080/run/run-20261010/steps/step-11' -H 'accept: */*' | jq
