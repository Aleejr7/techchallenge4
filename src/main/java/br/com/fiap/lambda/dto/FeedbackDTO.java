package br.com.fiap.lambda.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class FeedbackDTO {

    public String descricao;
    public int nota;

    public FeedbackDTO() {}

    public FeedbackDTO(String descricao, int nota) {
        this.descricao = descricao;
        this.nota = nota;
    }

}
