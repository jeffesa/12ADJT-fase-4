/**
 * Infrastructure Layer — Persistence (Output Adapter)
 *
 * Contains the implementation of the persistence gateway using DynamoDB.
 * 
 * Responsibilities:
 * - Implement FeedbackGateway domain interface
 * - Convert domain entities to/from DynamoDB AttributeValue maps
 * - Handle DynamoDB CRUD operations (Create, Read, Query)
 * 
 * Classes:
 * - DynamoFeedbackGateway: implements FeedbackGateway, manages DynamoDB operations
 * - FeedbackDynamoMapper: utility for entity-to-DynamoDB conversions
 */
package com.fiap.feedback.infra.persistence;
