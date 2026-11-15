package com.fiap.feedback.infra.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fiap.feedback.infra.graphql.ReceiveFeedbackQueryResolver;

/**
 * Guards the legacy public contract: JSON keys, GraphQL field names and the
 * external GraphQL query name must not change when Java identifiers are renamed.
 */
class LegacyContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final Set<String> LEGACY_OUTPUT_KEYS = Set.of("id", "descricao", "nota", "urgencia", "dataCriacao");

    private static ReceiveFeedbackOutput sampleOutput() {
        return new ReceiveFeedbackOutput(
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                "Atendimento excelente",
                9,
                "LOW",
                LocalDateTime.of(2024, 11, 9, 10, 30, 15));
    }

    private static Set<String> keys(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static String graphqlName(Class<?> type, String componentName) throws NoSuchFieldException {
        Field field = type.getDeclaredField(componentName);
        return field.getAnnotation(Name.class).value();
    }

    @Test
    void jacksonOutputSerializesExactlyTheLegacyKeys() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(sampleOutput()));

        assertEquals(LEGACY_OUTPUT_KEYS, keys(json));
        assertEquals("Atendimento excelente", json.get("descricao").asText());
        assertEquals(9, json.get("nota").asInt());
        assertEquals("LOW", json.get("urgencia").asText());
        assertEquals("2024-11-09T10:30:15", json.get("dataCriacao").asText());
    }

    @Test
    void jsonbOutputSerializesExactlyTheLegacyKeys() {
        JsonNode json = readTree(JSONB.toJson(sampleOutput()));

        assertEquals(LEGACY_OUTPUT_KEYS, keys(json));
        assertEquals("Atendimento excelente", json.get("descricao").asText());
        assertEquals(9, json.get("nota").asInt());
    }

    @Test
    void jacksonInputAcceptsLegacyKeys() throws Exception {
        ReceiveFeedbackInput input = MAPPER.readValue("{\"descricao\":\"Bom servico\",\"nota\":7}",
                ReceiveFeedbackInput.class);

        assertEquals("Bom servico", input.description());
        assertEquals(7, input.rating());
    }

    @Test
    void jsonbInputAcceptsLegacyKeys() {
        ReceiveFeedbackInput input = JSONB.fromJson("{\"descricao\":\"Bom servico\",\"nota\":7}",
                ReceiveFeedbackInput.class);

        assertEquals("Bom servico", input.description());
        assertEquals(7, input.rating());
    }

    @Test
    void graphqlOutputFieldNamesKeepLegacyNames() throws Exception {
        assertEquals("descricao", graphqlName(ReceiveFeedbackOutput.class, "description"));
        assertEquals("nota", graphqlName(ReceiveFeedbackOutput.class, "rating"));
        assertEquals("urgencia", graphqlName(ReceiveFeedbackOutput.class, "urgency"));
        assertEquals("dataCriacao", graphqlName(ReceiveFeedbackOutput.class, "createdAt"));
    }

    @Test
    void graphqlInputFieldNamesKeepLegacyNames() throws Exception {
        assertEquals("descricao", graphqlName(ReceiveFeedbackInput.class, "description"));
        assertEquals("nota", graphqlName(ReceiveFeedbackInput.class, "rating"));
    }

    @Test
    void graphqlQueryKeepsExternalName() throws Exception {
        Query query = ReceiveFeedbackQueryResolver.class.getMethod("listLegacyFeedbacks").getAnnotation(Query.class);

        assertEquals("listarAvaliacoes", query.value());
    }

    private static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
