# Purchase Transaction Service
A coding challenge to develop a purchase transaction store service with currency convertion.

## Table of Contents

1. [Requirements](#requirements)
2. [Technical Details](#technical-details)
3. [API Details](#api-details)
4. [Using APIs](#using-apis)


## Requirements

This service provides APIs endpoints over HTTP protocol, inspired on REST concepts, to insert and retrieve purchase transactions.

### Insertion

A transaction must have the following attributes:
- A description, with a maximum of 50 characters;
- A transaction date, in *yyyy-MM-dd* or *MM/dd/yyyy* formats;
- A purchase amount, a positive amount rounded to the nearest cent;

When stored, the transaction receives an unique integer identifier.

### Retrieving

The service provides an API to retrieve purchase transactions with amount converted to a given currency.

To convert the transaction amount, API will query the [Treasury Reporting Rates of Exchange API](https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange), based on the transaction date.

Requirements to convert transaction amount:
- Currency convertion rate must have effective date equal or less than transaction date, within the last 6 months;
- If no convertion rate is available within 6 months, equal or before the transaction date, an error will be returned to inform that conversion cannot be done.


## Technical Details

The service is built with Java 21 and Spring Boot framework.

Persistence layer is in-memory H2 database, for simplicity.

Spring Boot Retry is used to handle Treasury API call errors and retries.

Spring Boot Actuator is enabled to health check and application metrics.

Default values could be overrided in application.properties config file.

JUnit, Mockito and Spring MockMVC are used in tests.

To start this service, use `./mvnw spring-boot:run` in project folder (Maven required).

## API details

### Base URLs

All API endpoints use JSON for request and response bodies.

- `POST /api/purchase-transactions`
- `GET /api/purchase-transactions`
- `GET /api/purchase-transactions/converted`
- `GET /api/purchase-transactions/converted/{transactionId}`
- `GET /api/rates-of-exchange`

### Pagination parameters

The list endpoints support Spring Data pageable parameters:

- `page` (integer, zero-based page index)
- `size` (integer, page size)
- `sort` (property[,ASC|DESC])

Example:

`GET /api/purchase-transactions?page=0&size=10&sort=transactionDate,desc`

### 1) Create a purchase transaction

`POST /api/purchase-transactions`

Request body schema:

- `description` (string, required, max 50 chars)
- `amount` (number, required, positive, rounded to cents)
- `transactionDate` (string, required, format `yyyy-MM-dd` or `MM/dd/yyyy`)

Request example:

```json
{
  "description": "Office supplies",
  "amount": 123.45,
  "transactionDate": "2024-04-30"
}
```

Success response:

- Status: `201 Created`
- Body:

```json
{
  "id": 1,
  "description": "Office supplies",
  "amount": 123.45,
  "transactionDate": "2024-04-30"
}
```

### 2) List stored purchase transactions

`GET /api/purchase-transactions`

Query parameters:

- `page` (optional)
- `size` (optional)
- `sort` (optional)

Success response:

- Status: `200 OK`
- Body: a pageable `Slice` object with transaction DTOs

Response example:

```json
{
  "content": [
    {
      "id": 1,
      "description": "Office supplies",
      "amount": 123.45,
      "transactionDate": "2024-04-30"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true,
      "empty": true
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": false,
    "unsorted": true,
    "empty": true
  },
  "empty": false
}
```

### 3) List converted purchase transactions

`GET /api/purchase-transactions/converted?currency={currency}`

Query parameters:

- `currency` (string, required). Currency name (Real, Euro, Peso, etc).
- pagination parameters: `page`, `size`, `sort`

Success response:

- Status: `200 OK`
- Body: a pageable `Slice` of converted transaction DTOs

Response example:

```json
{
  "content": [
    {
      "id": 1,
      "description": "Office supplies",
      "amount": 123.45,
      "transactionDate": "2024-04-30",
      "convertedAmount": 98.76,
      "currency": "Euro",
      "exchangeRate": 0.8
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true,
      "empty": true
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": false,
    "unsorted": true,
    "empty": true
  },
  "empty": false
}
```

Notes:

1. Successful queries for rates of exchange in Treasury API are stored in database, so subsequent requests using an already provided currency will hit the database cache.

2. If the conversion rate cannot be found within the allowed lookback window, the returned item may still include the original transaction information and a `message` field indicating the missing conversion rate.

```json
{
  "content": [
    {
      "id": 1,
      "description": "Office supplies",
      "amount": 123.45,
      "transactionDate": "2024-04-30",
      "currency": "Euro",
      "message": "Exchange rate for currency Euro not found between 2025-12-23 and 2026-06-23"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true,
      "empty": true
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": false,
    "unsorted": true,
    "empty": true
  },
  "empty": false
}
```

### 4) Get a single converted transaction

`GET /api/purchase-transactions/converted/{transactionId}?currency={currency}`

Path parameters:

- `transactionId` (integer, required)

Query parameters:

- `currency` (string, required). Currency name (Real, Euro, Peso, etc).

Success response:

- Status: `200 OK`
- Body:

```json
{
  "id": 1,
  "description": "Office supplies",
  "amount": 123.45,
  "transactionDate": "2024-04-30",
  "convertedAmount": 98.76,
  "currency": "Euro",
  "exchangeRate": 0.8
}
```

Note:

If the conversion rate cannot be found within the allowed lookback window, the returned item may still include the original transaction information and a `message` field indicating the missing conversion rate.

```json
{
  "message": "Exchange rate for currency Cayman Islands Dollar not found between 2025-12-23 and 2026-06-23 for transaction 1"
}
```

### 5) List exchange rates

`GET /api/rates-of-exchange`

Query parameters:

- `page` (optional)
- `size` (optional)
- `sort` (optional)

Success response:

- Status: `200 OK`
- Body: a pageable `Slice` object with exchange rate DTOs

Response example:

```json
{
  "content": [
    {
      "country": "Eurozone",
      "currency": "Euro",
      "exchangeRate": 0.8,
      "effectiveDate": "2024-04-01"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true,
      "empty": true
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": false,
    "unsorted": true,
    "empty": true
  },
  "empty": false
}
```

Example request with pagination:

```http
GET /api/rates-of-exchange?page=0&size=10&sort=effectiveDate,desc
```

### Error responses

The API returns `400 Bad Request` for validation and request errors.

Invalid request example:

```json
{
  "message": "Some attributes are invalid, check details",
  "details": [
    "description is required",
    "amount must be a positive value"
  ]
}
```

Other bad request example:

```json
{
  "message": "Currency is required"
}
```

When conversion cannot be performed because no exchange rate is available within the allowed window, the API returns a `400 Bad Request` with a descriptive message.


## Using APIs

1) Store some purchase transactions
- `POST /api/purchase-transactions`

2) Check the transactions stored in database
- `GET /api/purchase-transactions`

3) Retrieve a transaction converting its amount to a given currency
- `GET /api/purchase-transactions/converted/{transactionId}?currency={currency}`

4) Want to see all transactions converted to a given currency? Go ahead!
- `GET /api/purchase-transactions/converted?currency={currency}`

5) List rates of exchange stored in database after some queries (cache)
- `GET /api/rates-of-exchange`