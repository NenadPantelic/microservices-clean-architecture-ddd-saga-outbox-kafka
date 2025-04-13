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

## Clean architecutre and Hexagonal architecture

### Clean architecture

- separation of concerns by diving the software into different layers
- use Dependency inversion and Polymorphism to create a dependency rule to make the domain layer the most independent and stable layer
- Dependency inversion - high-level modules should not depend on low-level modules
- source code dependencies can only point inward, towards the domain layer
  ![](images/clean_architecture.webp)
- independent of frameworks, UI, database and any external components
- the end result: testable domain layer
- define entities and use cases for domain logic
- the same high level principle is achieved with hexagonal and onion architectures: Isolate the domain

### Hexagonal architecture

- also known as `Ports and Adapters`; Ports (interfaces) and Adapters which implement the ports
- separate insides (domain layer) and outsides (infrastructure layers); isolate domain from any dependency such as UI, data layer, message queue, client components, external APIs or even a framework like Spring
- domain layer which holds the business logic is developed first and it will not depend on any other layer; again the domain layer should be the most independent and stable component of a system
- so, our software is only the domain layer, i.e. business logic
- all other parts (database, queue, client services...) are just plugins to our software
- invented in 2005 by Alistair Cockburn
  > Allow an application to be equally driven by users, programs , automated test or batch scripts, and to be developed and tested in isolation from its eventual runtime devices and databases
- ![](images/hexagonal_architecture.jpg) <br>
- two types of adapters:
  1. primary/driving adapters - use the core logic that is in domain layer. They use the input ports to call the implementations that are defined in the domain layer.
  2. secondary/driven adapters - implemented in external modules like database or message queue. These are called by business logic to complete external tasks. The results of such tasks can be returned back to an end user through the primary adapters.
- adapters are interchangeable components
- we use interfaces on both input and output and dependency injection is used to pass the implementation of the secondary adapters to the core logic
- input ports are implemented in the domain layer, but is not the adapter itself, whilst output ports are implemented by secondary adapters with infrastructure modules

### Advantages of Clean & Hexagonal architecture

- good for mainly long lasting applications
- Clean architecture - leaves as many options open as possible for as long as possible
  - we have to keep track of technological changes to keep up with evolving changes
  - we can decide to change one relational database with another or use a NoSQL database. If our business logic is separated, we can easily do any such transformation (e.g. change the data source)
- in a traditional 3-tier architecture

1. presentation layer
2. business layer
3. data layer

- dependencies are stacked on top of each other, upper layer is dependent on the layer below, eventually pointing to data layer. The idea is to have an upper layer (high-level module) independent of a lower layer (low-level module)
- with Clean architecture, all dependency arrows point to business layer, making it independent and stable
- with Clean architecture, we can delay the implementation details of dependencies (use abstractions and delay with any constraint of any data source or framework)
- easier to test the business logic by mocking the dependencies even before deciding the implementation details
- we can easily replace an adapter implementation, without touching the business logic
- every layer can be maintained separately without touching other layers
- independent development and deployment of different parts

#### Drawback

- writing more code, even duplicating some portions of it

### Business logic in Clean architecture

- **Entities**: Objects that embodies a small set of critical business rules (e.g. for an order that would validate whether we have it in stock, check the total price) and has the core business logic.
- **Use Cases**: describes the **application-specific business rules** (e.g. having a discount on an item temporarily, setting a daily purchase limit, etc). Contains the rules that specify how and when the Critical Business Rules within the Entities are invoked.
  Orchestrates the flow of data and from the entities, and direct those entities to use their Critical Business Rules to achieve the goals of the use case.

### Clean architecture and Domain-driven design

- in Clean architecture we have:
  - Entities as main components of the domain layer
  - Use cases that orchestrates Entities and the use of their Critical Business rules to achieve the goals of the use case.
- In DDD, we have:
  - Entities with an Aggregate root - main entity that orchestrate the core business rule of the current context. This context is called as **Bounded context** - boundary within a domain with a particular domain model.
    - **Aggregate** - a group of domain objects that always needs to be in consistent state. Therefore, we should save and update Aggregate as a whole inside a transaction. Aggregate root is a class which works as an entry point to our Aggregate and all business operations should go through the root. This way the Aggregate root will make sure that the Aggregate is in a consistent state.
  - **Domain Services** - equivalent to Use cases in Clean architecture. That is a business logic that spans multiple Aggregate roots, so that they can be implemented in a single Bounded context. It can also include the business logic that spans multiple entities and the logic that doesn't fit to an entity.
  - **Application service** - it exposes the required business layer methods to the outside using ports (first contact point from outside to domain layer) and these ports will be implemented in the domain layer itself.
    - In this layer we can also have data mappings, validations, transaction management and security.
    - responsible to make any data adapter calls and gather the data to pass into domain service and entities

![](images/clean_architecture_and_ddd.png) <br>
![](images/order-service-hexagonal.png)<br>

- Dependency inversion principle allows to revert any runtime dependency. It will lead to having an independent domain layer.
