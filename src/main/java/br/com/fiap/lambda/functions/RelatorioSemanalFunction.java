package br.com.fiap.lambda.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import br.com.fiap.lambda.service.RelatorioSemanalService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.time.*;
import java.util.Map;

@Named("relatorio-semanal")
public class RelatorioSemanalFunction implements RequestHandler<Map<String, Object>, String> {

    private static final Logger LOG = Logger.getLogger(RelatorioSemanalFunction.class);
    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");

    @Inject
    RelatorioSemanalService relatorioSemanalService;

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        ZonedDateTime now = ZonedDateTime.now(ZONE_BR);

        LocalDate thisMonday = now.toLocalDate().with(DayOfWeek.MONDAY);
        LocalDate lastMonday = thisMonday.minusWeeks(1);
        LocalDate lastSunday = thisMonday.minusDays(1);

        ZonedDateTime start = lastMonday.atStartOfDay(ZONE_BR);
        ZonedDateTime end = lastSunday.atTime(LocalTime.MAX).atZone(ZONE_BR);

        LOG.infof("Relatório semanal iniciado. Periodo=%s até %s", start, end);

        int total = relatorioSemanalService.gerarEEnviarRelatorio(start, end, now);

        LOG.infof("Relatório semanal finalizado. Total=%d", total);
        return "OK - itens=" + total;
    }
}
