# ADR-003: Database per service

- Status: Accepted (target architecture)
- Context: Independent services should own their data and avoid sharing database tables.
- Decision: Provision separate PostgreSQL databases for orders, inventory and payments.
- Consequences: Cross-service workflows require explicit orchestration and compensation; a single database transaction cannot cover the whole Saga.
