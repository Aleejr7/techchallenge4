package br.com.fiap.lambda.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@ApplicationScoped
public class SnsService {

    @Inject
    SnsClient  snsClient;

    private static final Logger LOG = Logger.getLogger(SnsService.class);

    String arn = System.getenv("SNS_TOPIC_ARN");

    public void publicarNotificacaoSNS(String assunto, String message) {
        try {
            PublishRequest publishRequest = PublishRequest.builder().topicArn(arn).subject(assunto).message(message).build();
            snsClient.publish(publishRequest);
            LOG.info("Mensagem enviada para o topic: " + arn);
        }catch (Exception e){
            LOG.error("Erro ao enviar o topic: " + arn, e);
        }
    }
}