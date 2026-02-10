package br.com.fiap.lambda.model;

import br.com.fiap.lambda.util.Urgencia;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.UUID;

@RegisterForReflection
public class Feedback {

    private String id;
    private int nota;
    private String descricao;
    private LocalDateTime dataAvaliacao;
    private Urgencia nivelUrgencia;


    public Feedback() {}

    public Feedback(String descricao, int nota) {
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("A nota deve ser entre 0 e 10.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }

        this.id = UUID.randomUUID().toString();
        this.descricao = descricao;
        this.nota = nota;
        this.dataAvaliacao = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNota() {
        return nota;
    }

    public void setNota(int nota) {
        this.nota = nota;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void setDataAvaliacao(LocalDateTime dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }

    public Urgencia getNivelUrgencia() {
        return nivelUrgencia;
    }

    public void setNivelUrgencia(Urgencia nivelUrgencia) {
        this.nivelUrgencia = nivelUrgencia;
    }
}