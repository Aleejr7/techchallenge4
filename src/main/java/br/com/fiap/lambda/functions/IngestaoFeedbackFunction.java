package br.com.fiap.lambda.functions;

import br.com.fiap.lambda.dto.FeedbackDTO;
import br.com.fiap.lambda.model.Feedback;
import br.com.fiap.lambda.service.FeedbackIngestaoService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("feedback-ingestao")
public class IngestaoFeedbackFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    FeedbackIngestaoService service;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Recebendo requisição na Lambda de Ingestão.");

        try {
            if (request.getBody() == null || request.getBody().isEmpty()) {
                return createResponse(400, "{\"erro\": \"Corpo da requisição vazio.\"}");
            }

            FeedbackDTO dto = objectMapper.readValue(request.getBody(), FeedbackDTO.class);

            Feedback feedbackSalvo = service.processarNovoFeedback(dto);

            String jsonResposta = objectMapper.writeValueAsString(feedbackSalvo);
            return createResponse(201, jsonResposta);

        } catch (IllegalArgumentException e) {
            context.getLogger().log("Erro de validação: " + e.getMessage());
            return createResponse(400, "{\"erro\": \"" + e.getMessage() + "\"}");

        } catch (Exception e) {
            context.getLogger().log("Erro interno: " + e.getMessage());
            e.printStackTrace();
            return createResponse(500, "{\"erro\": \"Erro interno ao processar feedback.\"}");
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(body)
                .withIsBase64Encoded(false);
    }
}