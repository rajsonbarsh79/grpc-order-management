# ADR-001: REST at the edge and gRPC between services

- Status: Accepted
- Context: External clients benefit from widely supported HTTP/JSON, while internal service calls benefit from generated, strongly typed contracts.
- Decision: Expose the order-creation API over REST and use gRPC for Inventory and Payment operations.
- Consequences: Contracts must be versioned compatibly; deadlines, error mapping and idempotency must be designed explicitly.
