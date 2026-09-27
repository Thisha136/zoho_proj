# VRM API Test Reference — All 18 Tables

Base URL: `http://localhost:8080/VRM-1.0-SNAPSHOT`

> Auth headers (until JWT is wired):
> - `X-User-Id: {uid}` for USER role calls
> - `X-Garage-Id: {gid}` for GARAGE role calls

---

## Testing Order (recommended)

```
brands → models → variants → garages → garage_variants
→ users → addresses → bikes
→ garage_services
→ service_requests (need user + bike + garage)
→ jobs (need accepted service_request)
→ garage_bays → bay_allocations (need job)
→ job_services (need job)
→ invoices → payments (need completed job)
→ feedback (need completed job)
```

---

## 1. USERS

### POST /users — Create account
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/users
Content-Type: application/json

{
  "username": "thisha",
  "email": "thisha@example.com",
  "password": "Pass@1234",
  "ph_no": "9876543210"
}
```
**Expected:** `201 Created`
```json
{ "message": "User created successfully", "uid": 1 }
```

---

### GET /users/{uid} — View own profile
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/1
```
**Expected:** `200 OK`
```json
{
  "uid": 1,
  "username": "thisha",
  "email": "thisha@example.com",
  "ph_no": "9876543210",
  "status": "ACTIVE",
  "created_at": "2026-09-20T10:00:00",
  "updated_at": "2026-09-20T10:00:00",
  "version": 0
}
```

---

### PUT /users/{uid} — Full profile update
```
PUT http://localhost:8080/VRM-1.0-SNAPSHOT/users/1
Content-Type: application/json

{
  "username": "thisha_updated",
  "email": "thisha@example.com",
  "ph_no": "9999999999"
}
```
**Expected:** `200 OK`
```json
{ "message": "User updated successfully" }
```

---

### PATCH /users/{uid} — Partial profile update
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/users/1
Content-Type: application/json

{ "ph_no": "8888888888" }
```
**Expected:** `200 OK`
```json
{ "message": "User partially updated successfully" }
```

---

### PATCH /users/{uid}/password — Change password
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/password
Content-Type: application/json

{ "password": "NewPass@5678" }
```
**Expected:** `200 OK`
```json
{ "message": "Password changed successfully" }
```

---

### PATCH /users/{uid}/status — Change status (ADMIN)
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/status
Content-Type: application/json

{ "status": "INACTIVE" }
```
**Expected:** `200 OK`
```json
{ "message": "User status updated successfully" }
```

**Negative — invalid status:**
```json
{ "status": "DELETED" }
```
**Expected:** `400 Bad Request`
```json
{ "error": "Status must be ACTIVE, INACTIVE or BLOCKED" }
```

---

### GET /users — Admin: list / search / filter / sort
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users?search=thisha
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users?status=ACTIVE
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users?sort=username&order=asc
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users?search=thisha&status=ACTIVE&sort=created_at&order=desc
```
**Expected:** `200 OK` — array of user objects (no password field)

---

## 2. ADDRESSES

### POST /users/{uid}/addresses — Create address
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/addresses
Content-Type: application/json

{
  "address_type": "HOME",
  "address_line": "12 MG Road",
  "city": "Bangalore",
  "state": "Karnataka",
  "pincode": "560001",
  "is_default": true
}
```
**Expected:** `201 Created`
```json
{ "message": "Address created successfully", "address_id": 1 }
```

---

### GET /users/{uid}/addresses — List all addresses
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/addresses
```
**Expected:** `200 OK` — array of address objects

---

### GET /users/{uid}/addresses/{address_id} — Get one
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/addresses/1
```
**Expected:** `200 OK` — address object

---

### PUT /users/{uid}/addresses/{address_id} — Full update
```
PUT http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/addresses/1
Content-Type: application/json

{
  "address_type": "WORK",
  "address_line": "55 Brigade Road",
  "city": "Bangalore",
  "state": "Karnataka",
  "pincode": "560025",
  "is_default": false
}
```
**Expected:** `200 OK`
```json
{ "message": "Address updated successfully" }
```

---

### PATCH /users/{uid}/addresses/{address_id} — Partial update
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/addresses/1
Content-Type: application/json

{ "city": "Mysore" }
```
**Expected:** `200 OK`
```json
{ "message": "Address partially updated" }
```

---

### PATCH /users/{uid}/addresses/{address_id}/default — Set as default
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/addresses/1/default
```
**Expected:** `200 OK`
```json
{ "message": "Default address updated" }
```

---

### DELETE /users/{uid}/addresses/{address_id}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/addresses/1
```
**Expected:** `200 OK`
```json
{ "message": "Address deleted successfully" }
```

**Negative — wrong user:**
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/2/addresses/1  (address belongs to uid=1)
```
**Expected:** `403 Forbidden`
```json
{ "error": "Access denied" }
```

---

## 3. BRANDS

### POST /brands — Admin: create
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/brands
Content-Type: application/json

{ "brand_name": "Honda" }
```
**Expected:** `201 Created`
```json
{ "message": "Brand created successfully", "bid": 1 }
```

---

### GET /brands — Browse all / search
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/brands
GET http://localhost:8080/VRM-1.0-SNAPSHOT/brands?search=hon
```
**Expected:** `200 OK` — array of brands

---

### GET /brands/{bid}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/brands/1
```
**Expected:** `200 OK`
```json
{ "bid": 1, "brandName": "Honda" }
```

---

### PUT /brands/{bid} — Admin: full update
```
PUT http://localhost:8080/VRM-1.0-SNAPSHOT/brands/1
Content-Type: application/json

{ "brand_name": "Honda Motors" }
```
**Expected:** `200 OK`
```json
{ "message": "Brand updated successfully" }
```

---

### PATCH /brands/{bid} — Admin: partial update
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/brands/1
Content-Type: application/json

{ "brand_name": "HONDA" }
```
**Expected:** `200 OK`
```json
{ "message": "Brand partially updated" }
```

---

### DELETE /brands/{bid}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/brands/1
```
**Expected:** `200 OK`
```json
{ "message": "Brand deleted successfully" }
```

**Negative — not found:**
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/brands/999
```
**Expected:** `404 Not Found`
```json
{ "error": "Brand not found" }
```

---

## 4. MODELS

### POST /models — Admin: create
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/models
Content-Type: application/json

{ "bid": 1, "model_name": "CB Shine" }
```
**Expected:** `201 Created`
```json
{ "message": "Model created successfully", "model_id": 1 }
```

---

### GET /models — Browse all / filter by brand
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/models
GET http://localhost:8080/VRM-1.0-SNAPSHOT/models?bid=1
```
**Expected:** `200 OK` — array of models

---

### GET /models/{model_id}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/models/1
```
**Expected:** `200 OK`

---

### PUT /models/{model_id}
```
PUT http://localhost:8080/VRM-1.0-SNAPSHOT/models/1
Content-Type: application/json

{ "bid": 1, "model_name": "CB Shine 125" }
```
**Expected:** `200 OK`

---

### PATCH /models/{model_id}
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/models/1
Content-Type: application/json

{ "model_name": "CB Shine 2026" }
```
**Expected:** `200 OK`

---

### DELETE /models/{model_id}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/models/1
```
**Expected:** `200 OK`

---

## 5. VARIANTS

### POST /variants — Admin: create
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/variants
Content-Type: application/json

{ "model_id": 1, "variant_name": "Drum Brake" }
```
**Expected:** `201 Created`
```json
{ "message": "Variant created successfully", "variant_id": 1 }
```

---

### GET /variants — Browse all / filter by model
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/variants
GET http://localhost:8080/VRM-1.0-SNAPSHOT/variants?model_id=1
```
**Expected:** `200 OK` — array

---

### GET /variants/{variant_id}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/variants/1
```
**Expected:** `200 OK`

---

### PUT /variants/{variant_id}
```
PUT http://localhost:8080/VRM-1.0-SNAPSHOT/variants/1
Content-Type: application/json

{ "model_id": 1, "variant_name": "Disc Brake" }
```
**Expected:** `200 OK`

---

### PATCH /variants/{variant_id}
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/variants/1
Content-Type: application/json

{ "variant_name": "Disc Brake CBS" }
```
**Expected:** `200 OK`

---

### DELETE /variants/{variant_id}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/variants/1
```
**Expected:** `200 OK`

---

## 6. BIKES

### POST /users/{uid}/bikes — User: create bike
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/bikes
X-User-Id: 1
Content-Type: application/json

{
  "variant_id": 1,
  "manufacturing_year": 2022,
  "purchase_date": "2022-06-15",
  "registration_no": "KA01AB1234",
  "fuel_type": "PETROL",
  "odometer_reading": 12500,
  "color": "Red"
}
```
**Expected:** `201 Created`
```json
{ "message": "Bike created successfully", "bike_id": 1 }
```

---

### GET /users/{uid}/bikes — List own bikes
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/bikes
X-User-Id: 1
```
**Expected:** `200 OK` — array

---

### GET /users/{uid}/bikes/{bike_id}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/bikes/1
X-User-Id: 1
```
**Expected:** `200 OK` — bike object

---

### PUT /users/{uid}/bikes/{bike_id}
```
PUT http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/bikes/1
X-User-Id: 1
Content-Type: application/json

{
  "variant_id": 1,
  "manufacturing_year": 2022,
  "purchase_date": "2022-06-15",
  "registration_no": "KA01AB1234",
  "fuel_type": "PETROL",
  "odometer_reading": 15000,
  "color": "Blue"
}
```
**Expected:** `200 OK`

---

### PATCH /users/{uid}/bikes/{bike_id}
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/bikes/1
X-User-Id: 1
Content-Type: application/json

{ "odometer_reading": 16000 }
```
**Expected:** `200 OK`

---

### DELETE /users/{uid}/bikes/{bike_id}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/bikes/1
X-User-Id: 1
```
**Expected:** `200 OK`

---

### GET /bikes — Admin: all bikes with filters
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/bikes
GET http://localhost:8080/VRM-1.0-SNAPSHOT/bikes?uid=1
GET http://localhost:8080/VRM-1.0-SNAPSHOT/bikes?variant_id=1
GET http://localhost:8080/VRM-1.0-SNAPSHOT/bikes?uid=1&variant_id=1
```
**Expected:** `200 OK` — array

---

### GET /bikes/{bike_id} — Admin: view any bike
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/bikes/1
```
**Expected:** `200 OK`

---

**Negative — wrong owner:**
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/2/bikes/1  (bike belongs to uid=1)
```
**Expected:** `403 Forbidden`

---

## 7. GARAGES

### POST /garages — Admin: create
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/garages
Content-Type: application/json

{
  "garage_name": "SpeedFix Garage",
  "address_line": "10 Industrial Area",
  "city": "Bangalore",
  "state": "Karnataka",
  "pincode": "560058"
}
```
**Expected:** `201 Created`
```json
{ "message": "Garage created successfully", "gid": 1 }
```

---

### GET /garages — Browse all
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages
```
**Expected:** `200 OK` — array

---

### GET /garages?variant_id={id} — User: find garages by variant
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages?variant_id=1
```
**Expected:** `200 OK` — garages that service variant 1

---

### GET /garages/{gid}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1
```
**Expected:** `200 OK` — garage object

---

### PUT /garages/{gid}
```
PUT http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1
Content-Type: application/json

{
  "garage_name": "SpeedFix Pro",
  "address_line": "10 Industrial Area",
  "city": "Bangalore",
  "state": "Karnataka",
  "pincode": "560058"
}
```
**Expected:** `200 OK`

---

### PATCH /garages/{gid}
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1
Content-Type: application/json

{ "garage_name": "SpeedFix Elite" }
```
**Expected:** `200 OK`

---

## 8. GARAGE VARIANTS

### POST /garages/{gid}/variants — Add variant
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/variants
Content-Type: application/json

{ "variant_id": 1 }
```
**Expected:** `201 Created`
```json
{ "message": "Variant added to garage" }
```

---

### GET /garages/{gid}/variants
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/variants
```
**Expected:** `200 OK` — array

---

### DELETE /garages/{gid}/variants/{variant_id}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/variants/1
```
**Expected:** `200 OK`
```json
{ "message": "Variant removed from garage" }
```

**Negative — duplicate add:**
```
POST /garages/1/variants  { "variant_id": 1 }  (already exists)
```
**Expected:** `409 Conflict`
```json
{ "error": "Variant already assigned to this garage" }
```

---

## 9. SERVICES

### POST /services — Admin: create
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/services
Content-Type: application/json

{
  "service_name": "Oil Change",
  "duration": 60,
  "price": 499.00,
  "service_type": "MAINTENANCE"
}
```
**Expected:** `201 Created`
```json
{ "message": "Service created successfully", "sid": 1 }
```

---

### GET /services — Browse / filter / sort
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/services
GET http://localhost:8080/VRM-1.0-SNAPSHOT/services?service_type=MAINTENANCE
GET http://localhost:8080/VRM-1.0-SNAPSHOT/services?sort=price&order=asc
GET http://localhost:8080/VRM-1.0-SNAPSHOT/services?sort=price&order=desc
```
**Expected:** `200 OK` — array

---

### GET /services/{sid}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/services/1
```
**Expected:** `200 OK`

---

### PUT /services/{sid}
```
PUT http://localhost:8080/VRM-1.0-SNAPSHOT/services/1
Content-Type: application/json

{
  "service_name": "Full Oil Change",
  "duration": 60,
  "price": 549.00,
  "service_type": "MAINTENANCE"
}
```
**Expected:** `200 OK`

---

### PATCH /services/{sid}
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/services/1
Content-Type: application/json

{ "price": 599.00 }
```
**Expected:** `200 OK`

---

### DELETE /services/{sid}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/services/1
```
**Expected:** `200 OK`

---

## 10. GARAGE SERVICES

### POST /garages/{gid}/services — Add service
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/services
Content-Type: application/json

{ "sid": 1 }
```
**Expected:** `201 Created`
```json
{ "message": "Service added to garage" }
```

---

### GET /garages/{gid}/services
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/services
```
**Expected:** `200 OK` — array of service objects

---

### GET /garages/{gid}/services/{sid}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/services/1
```
**Expected:** `200 OK`

---

### DELETE /garages/{gid}/services/{sid}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/services/1
```
**Expected:** `200 OK`
```json
{ "message": "Service removed from garage" }
```

**Negative — duplicate:**
```
POST /garages/1/services  { "sid": 1 }  (already assigned)
```
**Expected:** `409 Conflict`

---

## 11. SERVICE REQUESTS

### POST /service-requests — User: create
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests
X-User-Id: 1
Content-Type: application/json

{
  "bike_id": 1,
  "gid": 1,
  "bid": 1,
  "title": "Engine noise at startup",
  "issue": "Loud knocking sound when engine starts",
  "preferred_delivery_date": "2026-10-01",
  "vehicle_handling_option": "DROP"
}
```
**Expected:** `201 Created`
```json
{ "message": "Service request created", "sr_id": 1 }
```

---

### GET /service-requests — User own / Admin all
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests
X-User-Id: 1

GET http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests?status=PENDING
GET http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests?bike_id=1
GET http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests?status=PENDING&bike_id=1
```
**Expected:** `200 OK` — array

---

### GET /service-requests/{sr_id}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests/1
```
**Expected:** `200 OK` — full service request object

---

### PATCH /service-requests/{sr_id} — User: update allowed fields
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests/1
X-User-Id: 1
Content-Type: application/json

{ "title": "Updated title", "preferred_delivery_date": "2026-10-05" }
```
**Expected:** `200 OK`

---

### POST /service-requests/{sr_id}/cancel — User: cancel
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests/1/cancel
X-User-Id: 1
```
**Expected:** `200 OK`
```json
{ "message": "Service request cancelled" }
```

---

### PATCH /service-requests/{sr_id}/date-response — User: respond to proposed date
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests/1/date-response
X-User-Id: 1
Content-Type: application/json

{ "response": "ACCEPTED" }
```
**Expected:** `200 OK`
```json
{ "message": "Date response recorded" }
```

**Negative — wrong response:**
```json
{ "response": "MAYBE" }
```
**Expected:** `400 Bad Request`

---

### GET /garages/{gid}/service-requests — Garage: view own requests
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests
X-Garage-Id: 1

GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests?status=PENDING
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests?from=2026-09-01&to=2026-09-30
```
**Expected:** `200 OK` — array

---

### PATCH /garages/{gid}/service-requests/{sr_id}/physical-review — Garage
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests/1/physical-review
X-Garage-Id: 1
Content-Type: application/json

{ "physical_review_date": "2026-09-22" }
```
**Expected:** `200 OK`
```json
{ "message": "Physical review recorded" }
```

---

### PATCH /garages/{gid}/service-requests/{sr_id}/delivery-date — Garage: propose date
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests/1/delivery-date
X-Garage-Id: 1
Content-Type: application/json

{ "garage_proposed_delivery_date": "2026-10-03" }
```
**Expected:** `200 OK`
```json
{ "message": "Delivery date proposed" }
```

---

### POST /garages/{gid}/service-requests/{sr_id}/accept — Garage: accept
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests/1/accept
X-Garage-Id: 1
```
**Expected:** `200 OK`
```json
{ "message": "Service request accepted" }
```

---

### POST /garages/{gid}/service-requests/{sr_id}/reject — Garage: reject
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests/1/reject
X-Garage-Id: 1
```
**Expected:** `200 OK`
```json
{ "message": "Service request rejected" }
```

---

### PATCH /service-requests/{sr_id}/status — Admin
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests/1/status
Content-Type: application/json

{ "status": "ACCEPTED" }
```
**Expected:** `200 OK`

---

### Analytics — counts
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests/count
GET http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests/count?status=PENDING
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests/count
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/service-requests/count?status=ACCEPTED
```
**Expected:** `200 OK`
```json
{ "count": 5 }
```

---

## 12. JOBS

### POST /service-requests/{sr_id}/job — Garage: create job
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/service-requests/1/job
X-Garage-Id: 1
```
**Expected:** `201 Created`
```json
{ "message": "Job created successfully", "jid": 1 }
```

**Negative — not ACCEPTED:**
```
POST /service-requests/2/job  (status=PENDING)
```
**Expected:** `400 Bad Request`
```json
{ "error": "Job can only be created from an ACCEPTED service request" }
```

---

### GET /garages/{gid}/jobs — Garage: view jobs
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs
X-Garage-Id: 1

GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs?status=CREATED
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs?from=2026-09-01&to=2026-09-30
```
**Expected:** `200 OK`

---

### GET /garages/{gid}/jobs/{jid}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs/1
X-Garage-Id: 1
```
**Expected:** `200 OK` — job object

---

### PATCH /garages/{gid}/jobs/{jid}/status
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs/1/status
X-Garage-Id: 1
Content-Type: application/json

{ "status": "IN_PROGRESS" }
```
**Expected:** `200 OK`

---

### PATCH /garages/{gid}/jobs/{jid}/service-start
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs/1/service-start
X-Garage-Id: 1
```
**Expected:** `200 OK`
```json
{ "message": "Service start recorded" }
```

---

### PATCH /garages/{gid}/jobs/{jid}/service-end
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs/1/service-end
X-Garage-Id: 1
```
**Expected:** `200 OK`

---

### PATCH /garages/{gid}/jobs/{jid}/delivery-date
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs/1/delivery-date
X-Garage-Id: 1
Content-Type: application/json

{ "expected_delivery_date": "2026-10-02" }
```
**Expected:** `200 OK`

---

### PATCH /garages/{gid}/jobs/{jid}/pickup-status
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs/1/pickup-status
X-Garage-Id: 1
Content-Type: application/json

{ "pickup_status": "PICKED_UP" }
```
**Expected:** `200 OK`

---

### PATCH /garages/{gid}/jobs/{jid}/delivery-status
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs/1/delivery-status
X-Garage-Id: 1
Content-Type: application/json

{ "delivery_status": "DELIVERED" }
```
**Expected:** `200 OK`

---

### GET /users/{uid}/jobs — User: view own jobs
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/jobs
X-User-Id: 1
```
**Expected:** `200 OK` — array

---

### GET /users/{uid}/jobs/{jid}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/jobs/1
X-User-Id: 1
```
**Expected:** `200 OK`

---

### GET /jobs — Admin: all jobs
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs?status=IN_PROGRESS
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs?gid=1
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs?from=2026-09-01&to=2026-09-30
```
**Expected:** `200 OK`

---

### Analytics
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/count
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/count?status=COMPLETED
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/jobs/count
GET http://localhost:8080/VRM-1.0-SNAPSHOT/users/1/jobs/count
```
**Expected:** `200 OK` — `{ "count": 3 }`

---

## 13. JOB SERVICES

### POST /jobs/{jid}/services — Garage: add service
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services
X-Garage-Id: 1
Content-Type: application/json

{
  "sid": 1,
  "price": 499.00,
  "selection_type": "REQUIRED",
  "service_part_type": "OEM"
}
```
**Expected:** `201 Created`
```json
{ "message": "Service added to job", "js_id": 1 }
```

---

### GET /jobs/{jid}/services
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services
```
**Expected:** `200 OK` — array

---

### GET /jobs/{jid}/services/{js_id}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services/1
```
**Expected:** `200 OK`

---

### PATCH /jobs/{jid}/services/{js_id}/status
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services/1/status
X-Garage-Id: 1
Content-Type: application/json

{ "status": "IN_PROGRESS" }
```
**Expected:** `200 OK`

---

### PATCH /jobs/{jid}/services/{js_id}/start
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services/1/start
X-Garage-Id: 1
```
**Expected:** `200 OK`
```json
{ "message": "Start time recorded" }
```

---

### PATCH /jobs/{jid}/services/{js_id}/complete
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services/1/complete
X-Garage-Id: 1
```
**Expected:** `200 OK`

---

### PATCH /jobs/{jid}/services/{js_id}/time
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services/1/time
X-Garage-Id: 1
Content-Type: application/json

{ "time_taken": 45 }
```
**Expected:** `200 OK`

---

### POST /jobs/{jid}/services/{js_id}/approve — User: approve optional
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services/1/approve
X-User-Id: 1
```
**Expected:** `200 OK`
```json
{ "message": "Service approved" }
```

---

### POST /jobs/{jid}/services/{js_id}/reject — User: reject optional
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/services/1/reject
X-User-Id: 1
```
**Expected:** `200 OK`
```json
{ "message": "Service rejected" }
```

---

### GET /job-services — Admin
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/job-services
GET http://localhost:8080/VRM-1.0-SNAPSHOT/job-services?status=COMPLETED
GET http://localhost:8080/VRM-1.0-SNAPSHOT/job-services?sid=1
GET http://localhost:8080/VRM-1.0-SNAPSHOT/job-services/count?sid=1
```
**Expected:** `200 OK`

---

## 14. INVOICES + PAYMENTS

### POST /invoices/{iv_id}/payments — User: make payment
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/invoices/1/payments
X-User-Id: 1
Content-Type: application/json

{
  "amount": 499.00,
  "payment_method": "UPI",
  "payment_reference": "UPI123456789"
}
```
**Expected:** `201 Created`
```json
{ "message": "Payment recorded successfully", "payment_id": 1 }
```

---

### GET /invoices/{iv_id}/payments
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/invoices/1/payments
```
**Expected:** `200 OK` — array

---

### GET /payments/{payment_id}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/payments/1
```
**Expected:** `200 OK` — payment object

---

### GET /payments — Admin
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/payments
GET http://localhost:8080/VRM-1.0-SNAPSHOT/payments?status=PAID
GET http://localhost:8080/VRM-1.0-SNAPSHOT/payments?gid=1
GET http://localhost:8080/VRM-1.0-SNAPSHOT/payments?from=2026-09-01&to=2026-09-30
```
**Expected:** `200 OK`

---

### GET /payments/summary — Analytics
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/payments/summary
GET http://localhost:8080/VRM-1.0-SNAPSHOT/payments/summary?from=2026-09-01&to=2026-09-30
```
**Expected:** `200 OK`
```json
{ "total_count": 10, "total_amount": 4990.00 }
```

---

**Negative — already paid:**
```
POST /invoices/1/payments  (invoice already PAID)
```
**Expected:** `409 Conflict`
```json
{ "error": "Invoice is already paid" }
```

---

## 15. FEEDBACK

### POST /jobs/{jid}/feedback — User: submit
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/feedback
X-User-Id: 1
Content-Type: application/json

{
  "rating": 4,
  "comments": "Good service, a bit slow."
}
```
**Expected:** `201 Created`
```json
{ "message": "Feedback submitted", "feedback_id": 1 }
```

---

### GET /jobs/{jid}/feedback — User: view own
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/jobs/1/feedback
X-User-Id: 1
```
**Expected:** `200 OK` — feedback object

---

### PATCH /feedback/{feedback_id} — User: update
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/feedback/1
X-User-Id: 1
Content-Type: application/json

{ "rating": 5, "comments": "Actually great service!" }
```
**Expected:** `200 OK`
```json
{ "message": "Feedback updated" }
```

---

### GET /garages/{gid}/feedback — Garage: view received
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/feedback
X-Garage-Id: 1
```
**Expected:** `200 OK` — array

---

### GET /feedback — Admin
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/feedback
GET http://localhost:8080/VRM-1.0-SNAPSHOT/feedback?gid=1
GET http://localhost:8080/VRM-1.0-SNAPSHOT/feedback?rating=5
```
**Expected:** `200 OK`

---

### GET /feedback/summary — Analytics
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/feedback/summary
```
**Expected:** `200 OK`
```json
{ "total": 25, "avg_rating": 4.2 }
```

---

**Negative — rating out of range:**
```json
{ "rating": 6 }
```
**Expected:** `400 Bad Request`
```json
{ "error": "Rating must be between 1 and 5" }
```

---

## 16. GARAGE BAYS

### POST /garages/{gid}/bays — Create bay
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bays
X-Garage-Id: 1
Content-Type: application/json

{ "bay_name": "Bay A" }
```
**Expected:** `201 Created`
```json
{ "message": "Bay created successfully", "bay_id": 1 }
```

---

### GET /garages/{gid}/bays
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bays
```
**Expected:** `200 OK` — array

---

### GET /garages/{gid}/bays/{bay_id}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bays/1
```
**Expected:** `200 OK` — bay object

---

### PATCH /garages/{gid}/bays/{bay_id}
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bays/1
X-Garage-Id: 1
Content-Type: application/json

{ "bay_name": "Bay A Updated" }
```
**Expected:** `200 OK`
```json
{ "message": "Bay updated successfully" }
```

---

### DELETE /garages/{gid}/bays/{bay_id}
```
DELETE http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bays/1
X-Garage-Id: 1
```
**Expected:** `200 OK`
```json
{ "message": "Bay deleted successfully" }
```

---

## 17. BAY ALLOCATIONS

### POST /garages/{gid}/bay-allocations — Allocate bay
```
POST http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bay-allocations
X-Garage-Id: 1
Content-Type: application/json

{
  "bay_id": 1,
  "jid": 1,
  "start_date": "2026-09-22",
  "end_date": "2026-09-23"
}
```
**Expected:** `201 Created`
```json
{ "message": "Bay allocated successfully", "allocation_id": 1 }
```

---

### GET /garages/{gid}/bay-allocations
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bay-allocations
```
**Expected:** `200 OK` — array

---

### GET /garages/{gid}/bay-allocations/{id}
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bay-allocations/1
```
**Expected:** `200 OK`

---

### PATCH /garages/{gid}/bay-allocations/{id}
```
PATCH http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bay-allocations/1
X-Garage-Id: 1
Content-Type: application/json

{ "status": "COMPLETED" }
```
**Expected:** `200 OK`
```json
{ "message": "Allocation status updated" }
```

---

### GET /garages/{gid}/bay-availability?date= — Check availability
```
GET http://localhost:8080/VRM-1.0-SNAPSHOT/garages/1/bay-availability?date=2026-09-22
```
**Expected:** `200 OK`
```json
[
  { "bay_id": 1, "bay_name": "Bay A", "availability": "OCCUPIED" },
  { "bay_id": 2, "bay_name": "Bay B", "availability": "AVAILABLE" }
]
```

---

**Negative — date conflict:**
```
POST /garages/1/bay-allocations  (bay_id=1, dates overlap existing allocation)
```
**Expected:** `409 Conflict`
```json
{ "error": "Bay is already allocated for the requested date range" }
```

---

## Common Error Reference

| Scenario | Status | Body |
|---|---|---|
| Required field missing | `400` | `{"error": "Field X is required"}` |
| Invalid ID in URL | `400` | `{"error": "Invalid user ID"}` |
| Resource not found | `404` | `{"error": "X not found"}` |
| Wrong owner / access denied | `403` | `{"error": "Access denied"}` |
| Duplicate record | `409` | `{"error": "X already exists"}` |
| Server / DB error | `500` | `{"error": "Internal server error"}` |
