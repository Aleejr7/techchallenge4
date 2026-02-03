package br.com.fiap.lambda.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ApplicationScoped
public class DynamoDbService {

    @Inject
    DynamoDbClient dynamoDbClient;

    public void salvar(String dados) {
        // Lógica para salvar no DynamoDB
        System.out.println("Salvando no DynamoDB: " + dados);
    }
}