# Architecture

## Request path

Client --REST/JSON--> Order Service --gRPC--> Inventory Service
                                      \\--gRPC--> Payment Service

The shared `contracts` module owns versioned Protocol Buffer contracts. Service business logic should not be placed in this module.

## Order workflow

1. Validate the request.
2. Check inventory availability.
3. Reserve inventory using an idempotency key.
4. Request payment authorization using an idempotency key.
5. Return a confirmed order if payment succeeds.
6. Attempt to release inventory if payment fails.

## Production hardening

This starter uses in-memory service state to keep the first run easy. Before treating the workflow as durable, persist order state, reservations, payments and idempotency keys. Use a Saga state machine and reconciliation for ambiguous timeouts. Do not rely on a single synchronous HTTP request to recover distributed work.
