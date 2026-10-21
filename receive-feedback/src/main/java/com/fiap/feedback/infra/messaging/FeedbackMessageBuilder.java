package com.fiap.feedback.infra.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fiap.feedback.domain.entity.Feedback;

import org.jboss.logging.Logger;

/**
 * Builder utility for constructing JSON message payloads for SNS publishing.
 * 
 * This class is responsible for converting Feedback domain entities into
 * properly formatted JSON strings that can be sent as SNS message bodies.
 * 
 * JSON Format:
 * {
 *   "id": "uuid",
 *   "description": "feedback text",
 *   "rating": 5,
 *   "urgency": "HIGH",
 *   "createdAt": "2024-01-15T10:30:00"
 * }
 */
public class FeedbackMessageBuilder {

    private static final Logger LOGGER = Logger.getLogger(FeedbackMessageBuilder.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Builds a JSON string representation of the feedback for SNS publishing.
     * 
     * @param feedback the feedback entity to serialize
     * @return a JSON string containing the feedback data
     * @throws RuntimeException if JSON serialization fails
     */
    public static String buildFeedbackJson(Feedback feedback) {
        try {
            ObjectNode jsonNode = OBJECT_MAPPER.createObjectNode();
            jsonNode.put("id", feedback.getId());
            jsonNode.put("description", feedback.getDescription());
            jsonNode.put("rating", feedback.getRating());
            jsonNode.put("urgency", feedback.getUrgency().name());
            jsonNode.put("createdAt", feedback.getCreatedAt().toString());

            return OBJECT_MAPPER.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            LOGGER.errorf(
                    "Error building JSON message for feedback %s: %s",
                    feedback.getId(),
                    e.getMessage()
            );
            throw new RuntimeException("Failed to serialize feedback to JSON", e);
        }
    }
}
