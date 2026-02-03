package br.com.fiap.lambda.service;

import br.com.fiap.lambda.dto.FeedbackDTO;
import br.com.fiap.lambda.model.Feedback;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class FeedbackIngestaoService {

    private static final Logger LOG = Logger.getLogger(FeedbackIngestaoService.class);

    @Inject
    DynamoDbClient dynamoDbClient;

    @Inject
    SqsClient sqsClient;

    @Inject
    ObjectMapper objectMapper;

    String tableName = System.getenv("DYNAMO_TABLE_NAME");
    String queueUrl = System.getenv("SQS_QUEUE_URL");

    public Feedback processarNovoFeedback(FeedbackDTO dto) {
        LOG.info("Iniciando ingestão de feedback...");

        Feedback feedback = new Feedback(dto.descricao, dto.nota);

        salvarNoBanco(feedback);

        publicarEvento(feedback);

        return feedback;
    }

    private void salvarNoBanco(Feedback f) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();

            item.put("id", AttributeValue.builder().s(f.getId()).build());
            item.put("descricao", AttributeValue.builder().s(f.getDescricao()).build());
            item.put("nota", AttributeValue.builder().n(String.valueOf(f.getNota())).build());
            item.put("dataAvaliacao", AttributeValue.builder().s(f.getDataAvaliacao().toString()).build());
            item.put("nivelUrgencia", AttributeValue.builder().s(null).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(request);
            LOG.info("Feedback salvo no DynamoDB com ID: " + f.getId());

        } catch (Exception e) {
            LOG.error("Erro ao salvar avaliação no DynamoDB", e);
            throw new RuntimeException("Erro ao persistir dados", e);
        }
    }

    private void publicarEvento(Feedback f) {
        try {
            // Serializa o objeto Feedback completo para JSON
            String jsonBody = objectMapper.writeValueAsString(f);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(jsonBody)
                    .build();

            sqsClient.sendMessage(sendMsgRequest);
            LOG.info("Evento enviado para fila SQS para o feedback ID: " + f.getId());

        } catch (Exception e) {
            LOG.error("Erro ao enviar mensagem para SQS", e);
            throw new RuntimeException("Erro na mensageria", e);
        }
    }
}