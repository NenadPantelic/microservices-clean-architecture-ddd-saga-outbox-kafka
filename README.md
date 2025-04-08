# microservices-clean-architecture-ddd-saga-outbox-kafka

## Introduction

- Microservices

  - well-focused, fine-grained services that expose a lighweight protocol, e.g. HTTP
  - pros
    - independent development and deployment lifecycle (different teams can be in charge of them)
    - easy to scale up
    - better fault isolation
    - enables us to use different stacks for different services
  - they are developed in a distributed system, so that requires the use of microservice patterns and orchestration tools for common problems

### Clean architecture

- isolated domain logic from external dependencies (databases, message queues) and runtime tools
  - proposed by Robert Martin in 2012
  - depedency inversion - high level modules should not depend on low level modules. Both should depend on abstractions.
- Hexagonal architecture (ports and adapters):
  - invented by Alistair Cockburn in 2005
  - rely on well-defined interfaces called ports and implementing interfaces with replaceable adapters
- Onion architecture - similar to hexagonal architecture, the idea is to isolate the domain logic from outside dependencies

### Domain-driven design

- mapping the business domain and bounded context for the domain model
- tactical DDD - implement the domain model with `Aggregates`, `Entities`, `Value Objects`, `Services` and `Events`
- Kafka: an event store for event-driven services. Enables loosely coupled services that communicate through events.
- SAGA: distributed long running transactions accross services. Used for Long lived transactions (LLT). First invented in a publication in 1987
  - ACID transaction - guarantees that a database will be in a consistent state after running a group of operations
  - Choreography-based SAGA: based on events - local transactions publish domain events that trigger local transactions in other services
  - Orchestration-based SAGA: orchestrator coordinates the participants to run local transactions
  - Frameworks: Eventuate or Axon
- Outbox: helps the use of local ACID transactions to let consistent (eventual) distributed transactions. It will complete SAGA in a safe and consistent way.
  - the problem: local ACID transaction + Event publishing operation
  - Scenario 1: database transaction commited successfully and an event publishing failed -> SAGA cannot continue (incosistent state, one transaction happened, the other one did not even start)
  - Scenario 2: event published and reached to target successfully. After that the database transaction failed. Practically, the first transaction failed, the other one passed.
  - Solutions:
    1. direct event sourcing: use an event log as the primary source for your data. So, no database, but an event log as a data source. In practise, in most cases, we will need the local db transaction, especially if we have hard ACID needs, like monetary operations.
    2. Outbox pattern: instead of publishing the events directly, we store them in a local database table called an **Outbox table**. This table belongs to the same local database of the service used for local transactions.
    - run a single ACID transaction for a local DB transaction + Insertion into an `Outbox table`
    - to publish an event, we read the data from outbox table; two approaches
      1. pulling the data from an outbox table (poll it periodically)
      2. using CDC - Change Data Capture: listens transaction logs of the outbox table
- CQRS (Command Query Responsibility Segregation): separate read and write operations.
  - Better performance on read part using the right technology for reading and preventing conflicts with update commands -> develop, manage and scale read and write parts of a system
  - Scales each part separately.
  - Leads to evental consistency (some delay between the data propagation from write to read storage).
  - Outbox pattern should be used with this pattern as well to guarantee the consistency

![](images/project-overview-section-1.png)

- to achieve the hexagonal (clean) architecture, every service will be deployed in multiple units - every service has multiple layers, those will be deployable modules
- Primary adapters::input ports-> Domain (business) logic - REST API
- Secondary adapter::output ports -> messaging and data access
- Domain-driven design
  - **Aggregates** - a group of objects which always needs to be consistent
  - **Entitites** - aggregation happens through entity objects
  - **Value objects** - define simple immutable objects with domain-driven names. They do not contain identifiers, only values
  - **Domain services** - handle business object that spans multiple aggregate routes and logic that cannot fit into any entity by nature
  - **Application services** - point of contact to outer world of a domain that wants to communicate with the domain layer
  - **Domain events** - send notifications to other services which run in different bounded contexts
