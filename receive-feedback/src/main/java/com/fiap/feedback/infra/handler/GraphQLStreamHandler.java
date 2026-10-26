package com.fiap.feedback.infra.handler;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler;

/**
 * GraphQL Stream Handler for AWS Lambda with Quarkus.
 * 
 * This handler delegates to Quarkus's built-in HTTP request handling,
 * which properly initializes the CDI container before processing requests.
 * This allows GraphQL resolvers with @Inject annotations to work correctly.
 * 
 * Replaces REST RequestHandler which had CDI initialization timing issues.
 * GraphQL endpoint is automatically exposed at /graphql by Quarkus SmallRye GraphQL.
 */
public class GraphQLStreamHandler implements RequestStreamHandler {

    private final RequestStreamHandler delegate = new QuarkusStreamHandler();

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try {
            // Delegate to Quarkus runtime to handle the request through its HTTP layer
            delegate.handleRequest(input, output, context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle GraphQL request", e);
        }
    }
}
