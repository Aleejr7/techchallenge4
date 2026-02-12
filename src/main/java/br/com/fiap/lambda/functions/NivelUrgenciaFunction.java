package br.com.fiap.lambda.functions;

import br.com.fiap.lambda.model.Feedback;
import br.com.fiap.lambda.service.NivelUrgenciaService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;


@Named("nivel-urgencia")
public class NivelUrgenciaFunction implements RequestHandler<SQSEvent, Void> {

    private static final Logger LOG = Logger.getLogger(NivelUrgenciaFunction.class);

    @Inject
    NivelUrgenciaService service;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        LOG.info("Processando " + event.getRecords().size() + " mensagem(ns) da fila SQS");

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                processarMensagem(message);
            } catch (Exception e) {
                LOG.error("Erro ao processar mensagem: " + message.getMessageId(), e);
                throw new RuntimeException("Erro ao processar mensagem: " + message.getMessageId(), e);
            }
        }

        return null;
    }

    private void processarMensagem(SQSEvent.SQSMessage message) throws Exception {
        String body = message.getBody();
        LOG.info("Processando mensagem ID: " + message.getMessageId());

        Feedback feedback = objectMapper.readValue(body, Feedback.class);

        service.processarFeedback(feedback);
        
        LOG.info("✓ Mensagem processada com sucesso: " + message.getMessageId());
    }

}
