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
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Guards the public contract of the GraphQL mutation: JSON keys and GraphQL field names
 * must not change when Java identifiers are renamed.
 */
class LegacyContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final Set<String> OUTPUT_KEYS = Set.of("id", "description", "rating", "urgency", "createdAt");

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
    void jacksonOutputSerializesExactlyTheContractKeys() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(sampleOutput()));

        assertEquals(OUTPUT_KEYS, keys(json));
        assertEquals("Atendimento excelente", json.get("description").asText());
        assertEquals(9, json.get("rating").asInt());
        assertEquals("LOW", json.get("urgency").asText());
        assertEquals("2024-11-09T10:30:15", json.get("createdAt").asText());
    }

    @Test
    void jsonbOutputSerializesExactlyTheContractKeys() {
        JsonNode json = readTree(JSONB.toJson(sampleOutput()));

        assertEquals(OUTPUT_KEYS, keys(json));
        assertEquals("Atendimento excelente", json.get("description").asText());
        assertEquals(9, json.get("rating").asInt());
    }

    @Test
    void jacksonInputAcceptsContractKeys() throws Exception {
        ReceiveFeedbackInput input = MAPPER.readValue("{\"description\":\"Bom servico\",\"rating\":7}",
                ReceiveFeedbackInput.class);

        assertEquals("Bom servico", input.description());
        assertEquals(7, input.rating());
    }

    @Test
    void jsonbInputAcceptsContractKeys() {
        ReceiveFeedbackInput input = JSONB.fromJson("{\"description\":\"Bom servico\",\"rating\":7}",
                ReceiveFeedbackInput.class);

        assertEquals("Bom servico", input.description());
        assertEquals(7, input.rating());
    }

    @Test
    void graphqlOutputFieldNamesMatchContract() throws Exception {
        assertEquals("description", graphqlName(ReceiveFeedbackOutput.class, "description"));
        assertEquals("rating", graphqlName(ReceiveFeedbackOutput.class, "rating"));
        assertEquals("urgency", graphqlName(ReceiveFeedbackOutput.class, "urgency"));
        assertEquals("createdAt", graphqlName(ReceiveFeedbackOutput.class, "createdAt"));
    }

    @Test
    void graphqlInputFieldNamesMatchContract() throws Exception {
        assertEquals("description", graphqlName(ReceiveFeedbackInput.class, "description"));
        assertEquals("rating", graphqlName(ReceiveFeedbackInput.class, "rating"));
    }

    private static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
