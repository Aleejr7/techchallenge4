package br.com.fiap.lambda.functions;

import br.com.fiap.lambda.service.DynamoDbService;
import br.com.fiap.lambda.service.SqsService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("ingestao-feedback")
public class IngestaoFeedbackFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    DynamoDbService dynamoService; // (Você vai criar depois)

    @Inject
    SqsService sqsService; // (Você vai criar depois)

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        // 1. Log (CloudWatch)
        context.getLogger().log("Recebendo feedback: " + request.getBody());

        // 2. Lógica (Simplificada)
        // Aqui você converte o JSON, valida e salva no Dynamo
        // dynamoService.salvar(request.getBody());

        // 3. Envia para SQS para ser analisado pela Lambda 2
        // sqsService.enviarParaFila(request.getBody());

        // 4. Retorno HTTP 201
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withBody("{\"message\": \"Feedback recebido com sucesso\"}");
    }
}