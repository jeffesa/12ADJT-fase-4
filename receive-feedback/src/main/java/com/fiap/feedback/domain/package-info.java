/**
 * Domain Layer - Clean Architecture
 * 
 * This package contains domain entities and gateways that represent
 * the business rules of the application, independent of any
 * framework or persistence technology.
 * 
 * Structure:
 * - entity: Pure domain entities (POJO) that encapsulate business rules
 * - gateway: Interfaces that define contracts for communication with infrastructure
 * 
 * Characteristics:
 * - Pure POJO: without annotations from Spring, Quarkus or frameworks
 * - Immutability: entities are created with all necessary data
 * - Domain validations: business rules implemented in entities
 * - Technology independence: code does not depend on external libraries
 * 
 * References:
 * - Clean Architecture (Robert C. Martin)
 * - Domain-Driven Design (Eric Evans)
 * - Ports & Adapters (Alistair Cockburn)
 * 
 * GraphQL API:
 * Domain entities are ready for use in GraphQL resolvers
 * in the presentation layer, without need for refactoring.
 */
package com.fiap.feedback.domain;
