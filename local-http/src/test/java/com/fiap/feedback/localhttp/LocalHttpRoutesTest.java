package com.fiap.feedback.localhttp;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalHttpRoutesTest {

    @Mock RoutingContext ctx;
    @Mock HttpServerRequest request;
    @Mock HttpServerResponse out;
    @Mock RequestBody body;

    @BeforeEach
    void setUp() {
        when(ctx.request()).thenReturn(request);
        when(ctx.response()).thenReturn(out);
        when(ctx.body()).thenReturn(body);
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(out.setStatusCode(org.mockito.ArgumentMatchers.anyInt())).thenReturn(out);
    }

    private void request(HttpMethod method, String path, MultiMap headers, String payload) {
        when(request.method()).thenReturn(method);
        when(request.path()).thenReturn(path);
        when(request.headers()).thenReturn(headers);
        when(body.asString()).thenReturn(payload);
    }

    @Test
    void mapsMethodPathHeadersAndBodyToEvent() {
        request(HttpMethod.POST, "/avaliacao",
                MultiMap.caseInsensitiveMultiMap().add("Content-Type", "application/json").add("X-API-Key", "k"),
                "{\"nota\":8}");
        MultiMap q = MultiMap.caseInsensitiveMultiMap().add("a", "1");
        when(ctx.queryParams()).thenReturn(q);

        APIGatewayProxyRequestEvent event = LocalHttpRoutes.toEvent(ctx);

        assertEquals("POST", event.getHttpMethod());
        assertEquals("/avaliacao", event.getPath());
        assertEquals("application/json", event.getHeaders().get("content-type"));
        assertEquals("k", event.getHeaders().get("x-api-key"));
        assertEquals("{\"nota\":8}", event.getBody());
        assertEquals(Map.of("a", "1"), event.getQueryStringParameters());
    }

    @Test
    void nullBodyStaysNull() {
        request(HttpMethod.GET, "/avaliacao", MultiMap.caseInsensitiveMultiMap(), null);
        when(ctx.body()).thenReturn(null);

        APIGatewayProxyRequestEvent event = LocalHttpRoutes.toEvent(ctx);

        assertEquals("GET", event.getHttpMethod());
        assertNull(event.getBody());
    }

    @Test
    void passesThroughStatusHeadersAndBody() {
        request(HttpMethod.POST, "/avaliacao", MultiMap.caseInsensitiveMultiMap(), "{}");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(201).withHeaders(Map.of("Content-Type", "application/json")).withBody("{\"ok\":true}");

        LocalHttpRoutes.handle(ctx, (e, c) -> response);

        verify(out).setStatusCode(201);
        verify(out).putHeader("Content-Type", "application/json");
        verify(out).end("{\"ok\":true}");
    }

    @Test
    void handlerReceivesBuiltEventAndNullContext() {
        request(HttpMethod.POST, "/graphql", MultiMap.caseInsensitiveMultiMap().add("X-Api-Key", "k"), "q");
        APIGatewayProxyRequestEvent[] seen = new APIGatewayProxyRequestEvent[1];
        Object[] context = {"unset"};

        LocalHttpRoutes.handle(ctx, (e, c) -> {
            seen[0] = e;
            context[0] = c;
            return new APIGatewayProxyResponseEvent().withStatusCode(200);
        });

        assertEquals("/graphql", seen[0].getPath());
        assertEquals("k", seen[0].getHeaders().get("x-api-key"));
        assertNull(context[0]);
    }

    @Test
    void errorStatusesAreUnchanged() {
        request(HttpMethod.POST, "/graphql", MultiMap.caseInsensitiveMultiMap(), null);
        for (int status : new int[]{400, 401, 403, 500}) {
            LocalHttpRoutes.handle(ctx, (e, c) ->
                    new APIGatewayProxyResponseEvent().withStatusCode(status).withBody("err " + status));
            verify(out).setStatusCode(status);
            verify(out).end("err " + status);
        }
    }

    @Test
    void nullBodyAndHeadersInResponseBecomeEmptyBody() {
        request(HttpMethod.GET, "/avaliacao", MultiMap.caseInsensitiveMultiMap(), null);

        LocalHttpRoutes.handle(ctx, (e, c) -> new APIGatewayProxyResponseEvent().withStatusCode(204));

        verify(out).setStatusCode(204);
        verify(out).end("");
    }
}
