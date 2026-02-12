package br.com.fiap.lambda.service;

import br.com.fiap.lambda.model.Feedback;
import br.com.fiap.lambda.util.Urgencia;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;


@ApplicationScoped
public class NivelUrgenciaService {

    private static final Logger LOG = Logger.getLogger(NivelUrgenciaService.class);

    public Urgencia processarFeedback(Feedback feedback) {
        LOG.info("Processando feedback ID: " + feedback.getId());

        Urgencia nivelUrgencia = calcularNivelUrgencia(feedback.getNota());

        LOG.info(String.format(
            "Feedback ID: %s | Nota: %d | Urgência: %s",
            feedback.getId(),
            feedback.getNota(),
            nivelUrgencia
        ));

        if (nivelUrgencia == Urgencia.ALTA) {
            emitirAlertaUrgenciaAlta(feedback);
        }

        return nivelUrgencia;
    }

    private Urgencia calcularNivelUrgencia(int nota) {
        if (nota >= 0 && nota <= 3) {
            return Urgencia.ALTA;
        } else if (nota >= 4 && nota <= 7) {
            return Urgencia.MEDIA;
        } else {
            return Urgencia.BAIXA;
        }
    }

    private void emitirAlertaUrgenciaAlta(Feedback feedback) {
        // publicarNotificacaoSNS(feedback);
    }

}

