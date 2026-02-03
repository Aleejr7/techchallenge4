package br.com.fiap.lambda.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class SqsService {

    @Inject
    SqsClient sqsClient;

    public void enviarParaFila(String mensagem) {
        // Lógica para enviar para o SQS
        System.out.println("Enviando para fila SQS: " + mensagem);
    }
}