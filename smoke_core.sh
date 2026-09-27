#!/bin/bash
set -u
BASE='http://localhost:8080/VRM-1.0-SNAPSHOT'
run() {
  label="$1"; method="$2"; url="$3"; shift 3
  printf '\n%-42s ' "$label"
  curl --max-time 10 -sS -o /tmp/smoke_body -w '%{http_code}' -X "$method" "$url" "$@" || printf 'ERR'
  printf '  '; head -c 220 /tmp/smoke_body | tr '\n' ' '; printf '\n'
}
run 'GET garages' GET "$BASE/garages"
run 'GET garage 1' GET "$BASE/garages/1"
run 'GET garage variants' GET "$BASE/garages/1/variants"
run 'GET garage services' GET "$BASE/garages/1/services"
run 'GET garage service 3' GET "$BASE/garages/1/services/3"
run 'GET service requests' GET "$BASE/service-requests" -H 'X-User-Id: 1'
run 'GET service request 2' GET "$BASE/service-requests/2" -H 'X-User-Id: 1'
run 'GET garage requests' GET "$BASE/garages/1/service-requests"
run 'GET garage request 2' GET "$BASE/garages/1/service-requests/2"
run 'GET garage request count' GET "$BASE/garages/1/service-requests/count"
run 'GET garage jobs' GET "$BASE/garages/1/jobs"
run 'GET garage job 1' GET "$BASE/garages/1/jobs/1"
run 'GET jobs' GET "$BASE/jobs"
run 'GET job 1' GET "$BASE/jobs/1"
run 'GET job services' GET "$BASE/jobs/1/services"
run 'GET job service 1' GET "$BASE/jobs/1/services/1"
run 'GET job-services' GET "$BASE/job-services"
run 'GET invoices' GET "$BASE/invoices/2"
run 'GET invoice payments' GET "$BASE/invoices/2/payments"
run 'GET payment 2' GET "$BASE/payments/2"
run 'GET payment summary' GET "$BASE/payments/summary"
