package br.com.fiap.lambda.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class RelatorioSemanalService {

    private static final Logger LOG = Logger.getLogger(RelatorioSemanalService.class);

    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter ISO_OUT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final DynamoDbClient dynamo = DynamoDbClient.builder().region(Region.US_EAST_2).build();
    private final String tableName = requiredEnv("TABLE_NAME");

    @Inject
    SnsService snsService;

    public int gerarEEnviarRelatorio(ZonedDateTime startBr, ZonedDateTime endBr, ZonedDateTime sentAtBr) {
        String startIsoUtc = startBr.withZoneSameInstant(ZoneOffset.UTC).format(ISO_OUT);
        String endIsoUtc = endBr.withZoneSameInstant(ZoneOffset.UTC).format(ISO_OUT);

        LOG.infof("Buscando feedbacks no Dynamo. tabela=%s startUtc=%s endUtc=%s", tableName, startIsoUtc, endIsoUtc);

        List<Map<String, AttributeValue>> items = fetchItems(startIsoUtc, endIsoUtc);

        LOG.infof("Itens encontrados=%d", items.size());

        Report report = aggregate(items, startBr.toLocalDate(), endBr.toLocalDate(), sentAtBr);

        String message = format(report);

        LOG.infof("Enviando relatório ao SNS. subject=%s tamanhoMensagem=%d", "Relatório Semanal de Feedbacks", message.length());

        snsService.publicarNotificacaoSNS("Relatório Semanal de Feedbacks", message);

        LOG.info("Processo de envio do relatório acionado.");
        return report.total();
    }

    private List<Map<String, AttributeValue>> fetchItems(String startIsoUtc, String endIsoUtc) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":start", AttributeValue.builder().s(startIsoUtc).build());
        eav.put(":end", AttributeValue.builder().s(endIsoUtc).build());

        ScanRequest base = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("dataAvaliacao BETWEEN :start AND :end")
                .expressionAttributeValues(eav)
                .projectionExpression("id, dataAvaliacao, nota")
                .build();

        List<Map<String, AttributeValue>> out = new ArrayList<>();
        ScanRequest req = base;

        int pages = 0;

        while (true) {
            ScanResponse res = dynamo.scan(req);
            pages++;
            out.addAll(res.items());

            LOG.infof("Scan pagina=%d itensPagina=%d totalAcumulado=%d", pages, res.items().size(), out.size());

            if (res.lastEvaluatedKey() == null || res.lastEvaluatedKey().isEmpty()) break;
            req = base.toBuilder().exclusiveStartKey(res.lastEvaluatedKey()).build();
        }

        return out;
    }

    private Report aggregate(List<Map<String, AttributeValue>> items,
                             LocalDate startDate,
                             LocalDate endDate,
                             ZonedDateTime sentAt) {

        Map<LocalDate, Long> countByDay = new TreeMap<>();
        EnumMap<Urgency, Long> countByUrgency = new EnumMap<>(Urgency.class);
        for (Urgency u : Urgency.values()) countByUrgency.put(u, 0L);

        BigDecimal sum = BigDecimal.ZERO;
        long countNota = 0;

        long semNota = 0;

        for (Map<String, AttributeValue> it : items) {
            LocalDate day = parseDate(it.get("dataAvaliacao"));
            countByDay.put(day, countByDay.getOrDefault(day, 0L) + 1);

            Integer notaInt = readInt(it.get("nota"));
            if (notaInt == null) {
                semNota++;
                countByUrgency.put(Urgency.DESCONHECIDA, countByUrgency.getOrDefault(Urgency.DESCONHECIDA, 0L) + 1);
                continue;
            }

            Urgency urgency = calcularNivelUrgencia(notaInt);
            countByUrgency.put(urgency, countByUrgency.getOrDefault(urgency, 0L) + 1);

            sum = sum.add(BigDecimal.valueOf(notaInt));
            countNota++;
        }

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            countByDay.putIfAbsent(d, 0L);
        }

        BigDecimal avgNota = (countNota == 0)
                ? BigDecimal.ZERO
                : sum.divide(BigDecimal.valueOf(countNota), 2, java.math.RoundingMode.HALF_UP);

        long criticalCount = countByUrgency.getOrDefault(Urgency.ALTA, 0L);

        LOG.infof("Resumo calculado. total=%d avg=%s altas=%d medias=%d baixas=%d semNota=%d",
                items.size(),
                avgNota,
                countByUrgency.getOrDefault(Urgency.ALTA, 0L),
                countByUrgency.getOrDefault(Urgency.MEDIA, 0L),
                countByUrgency.getOrDefault(Urgency.BAIXA, 0L),
                semNota
        );

        return new Report(sentAt, startDate, endDate, items.size(), avgNota, countByDay, countByUrgency, criticalCount);
    }

    private Urgency calcularNivelUrgencia(int nota) {
        if (nota >= 0 && nota <= 3) {
            return Urgency.ALTA;
        } else if (nota >= 4 && nota <= 7) {
            return Urgency.MEDIA;
        } else if (nota >= 8 && nota <= 10) {
            return Urgency.BAIXA;
        } else {
            return Urgency.DESCONHECIDA;
        }
    }

    private String format(Report r) {
        StringBuilder sb = new StringBuilder();

        sb.append("RELATÓRIO SEMANAL - FEEDBACKS\n");
        sb.append("Data de envio: ")
                .append(r.sentAt().withZoneSameInstant(ZONE_BR)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("\n");
        sb.append("Período: ").append(r.startDate()).append(" até ").append(r.endDate()).append("\n\n");

        sb.append("Resumo\n");
        sb.append("- Total de avaliações: ").append(r.total()).append("\n");
        sb.append("- Média das avaliações: ").append(r.avgNota()).append("\n");
        sb.append("- Avaliações críticas (ALTA): ").append(r.criticalCount()).append("\n\n");

        sb.append("Quantidade de avaliações por dia\n");
        for (var e : r.countByDay().entrySet()) {
            sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        sb.append("\n");

        sb.append("Quantidade por urgência\n");
        sb.append("- ALTA: ").append(r.countByUrgency().getOrDefault(Urgency.ALTA, 0L)).append("\n");
        sb.append("- MEDIA: ").append(r.countByUrgency().getOrDefault(Urgency.MEDIA, 0L)).append("\n");
        sb.append("- BAIXA: ").append(r.countByUrgency().getOrDefault(Urgency.BAIXA, 0L)).append("\n");

        long desconhecida = r.countByUrgency().getOrDefault(Urgency.DESCONHECIDA, 0L);
        if (desconhecida > 0) {
            sb.append("- DESCONHECIDA: ").append(desconhecida).append("\n");
        }

        return sb.toString();
    }

    private LocalDate parseDate(AttributeValue v) {
        if (v == null || v.s() == null) return LocalDate.of(1970, 1, 1);
        String s = v.s().trim();

        try {
            OffsetDateTime odt = OffsetDateTime.parse(s);
            return odt.atZoneSameInstant(ZONE_BR).toLocalDate();
        } catch (Exception ignored) { }

        try {
            LocalDateTime ldt = LocalDateTime.parse(s);
            return ldt.atZone(ZoneOffset.UTC).withZoneSameInstant(ZONE_BR).toLocalDate();
        } catch (Exception ignored) { }

        if (s.length() >= 10) return LocalDate.parse(s.substring(0, 10));
        return LocalDate.of(1970, 1, 1);
    }

    private Integer readInt(AttributeValue v) {
        if (v == null) return null;

        try {
            if (v.n() != null) {
                return Integer.parseInt(v.n());
            }
        } catch (Exception ignored) { }

        if (v.s() != null) {
            try {
                return Integer.parseInt(v.s().trim());
            } catch (Exception ignored) { }
        }

        return null;
    }

    private static String requiredEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing env var: " + key);
        return v.trim();
    }

    enum Urgency { BAIXA, MEDIA, ALTA, DESCONHECIDA }

    record Report(
            ZonedDateTime sentAt,
            LocalDate startDate,
            LocalDate endDate,
            int total,
            BigDecimal avgNota,
            Map<LocalDate, Long> countByDay,
            Map<Urgency, Long> countByUrgency,
            long criticalCount
    ) {}
}