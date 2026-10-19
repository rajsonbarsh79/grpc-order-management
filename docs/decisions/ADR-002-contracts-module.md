# ADR-002: Shared Protobuf contracts module

- Status: Accepted
- Context: Several services need the same generated request, response and service-stub types.
- Decision: Keep `.proto` files in a dedicated Maven module and consume the generated JAR from service modules.
- Consequences: Contract changes are built and versioned with the monorepo. Do not put domain entities or persistence models in this module.
