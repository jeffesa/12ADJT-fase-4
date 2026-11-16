package com.fiap.feedback.infra.dto;

/**
 * Body of POST /v1/avaliacao (Portuguese field names).
 *
 * Mapped to FeedbackInput before validation, so the same rules and messages apply.
 *
 * @param descricao the evaluation text (JSON name: descricao)
 * @param nota the numeric grade (JSON name: nota), validated as 0 to 10 after mapping
 */
public record AvaliacaoInput(
        String descricao,
        Integer nota
) {
}
