package com.fiap.feedback.domain.entity;

/**
 * Enum que representa os níveis de urgência de um feedback.
 * 
 * A urgência é calculada automaticamente com base na nota atribuída:
 * - HIGH: nota de 0 a 5 (feedback muito negativo)
 * - MEDIUM: nota de 6 a 7 (feedback moderado)
 * - LOW: nota de 8 a 10 (feedback positivo)
 */
public enum Urgencia {
    HIGH("Alta"),
    MEDIUM("Média"),
    LOW("Baixa");

    private final String descricao;

    Urgencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Calcula o nível de urgência com base na nota fornecida.
     * 
     * @param nota valor de 0 a 10
     * @return Urgencia correspondente à nota
     * @throws IllegalArgumentException se a nota for inválida (< 0 ou > 10)
     */
    public static Urgencia fromNota(int nota) {
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("Nota deve estar entre 0 e 10, recebido: " + nota);
        }
        
        if (nota <= 5) {
            return HIGH;
        } else if (nota <= 7) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}
