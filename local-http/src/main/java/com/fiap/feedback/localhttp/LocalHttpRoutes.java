package com.fiap.feedback.localhttp;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fiap.feedback.infra.handler.GraphQLHandler;
import com.fiap.feedback.infra.handler.ListFeedbackHandler;
import com.fiap.feedback.infra.handler.ReceiveFeedbackHandler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Dev-only HTTP front: turns local HTTP requests into API Gateway proxy events
 * and calls the same Lambda handlers used in production.
 */
@ApplicationScoped
public class LocalHttpRoutes {

    @Inject
    ReceiveFeedbackHandler receiveFeedbackHandler;

    @Inject
    ListFeedbackHandler listFeedbackHandler;

    @Inject
    GraphQLHandler graphQLHandler;

    void init(@Observes Router router) {
        router.route().order(-1000).handler(BodyHandler.create());
        router.post("/avaliacao").order(-900).blockingHandler(ctx -> handle(ctx, receiveFeedbackHandler::handleRequest));
        router.get("/avaliacao").order(-900).blockingHandler(ctx -> handle(ctx, listFeedbackHandler::handleRequest));
        router.post("/graphql").order(-900).blockingHandler(ctx -> handle(ctx, graphQLHandler::handleRequest));
    }

    static void handle(RoutingContext ctx,
                       BiFunction<APIGatewayProxyRequestEvent, com.amazonaws.services.lambda.runtime.Context,
                               APIGatewayProxyResponseEvent> handler) {
        APIGatewayProxyResponseEvent response = handler.apply(toEvent(ctx), null);
        write(ctx.response(), response);
    }

    static APIGatewayProxyRequestEvent toEvent(RoutingContext ctx) {
        Map<String, String> headers = new HashMap<>();
        ctx.request().headers().forEach(e -> headers.put(e.getKey().toLowerCase(), e.getValue()));
        Map<String, String> query = new HashMap<>();
        ctx.queryParams().forEach(e -> query.put(e.getKey(), e.getValue()));

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod(ctx.request().method().name());
        event.setPath(ctx.request().path());
        event.setHeaders(headers);
        event.setQueryStringParameters(query);
        event.setBody(ctx.body() == null ? null : ctx.body().asString());
        return event;
    }

    static void write(HttpServerResponse out, APIGatewayProxyResponseEvent response) {
        out.setStatusCode(response.getStatusCode());
        if (response.getHeaders() != null) {
            response.getHeaders().forEach(out::putHeader);
        }
        out.end(response.getBody() == null ? "" : response.getBody());
    }
}
