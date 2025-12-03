package com.queue.indexer.exception;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;

import java.util.stream.Collectors;

public final class EsBulkErrorClassifier {

    private EsBulkErrorClassifier() {}

    public static void handle(BulkResponse resp) {
        if (resp == null || !Boolean.TRUE.equals(resp.errors())) return;

        var retryables   = resp.items().stream()
                .filter(i -> i.error() != null && isRetryable(i))
                .collect(Collectors.toList());

        var mappings     = resp.items().stream()
                .filter(i -> i.error() != null && isMapping(i))
                .collect(Collectors.toList());

        var badRequests  = resp.items().stream()
                .filter(i -> i.error() != null && isBadEvent(i))
                .collect(Collectors.toList());

        // 우선순위: 재시도 가능한 게 하나라도 있으면 Retryable 처리
        if (!retryables.isEmpty()) {
            throw new RetryableEsException(("Retryable bulk errors"));
        }
        // 매핑 문제 있으면 MappingException
        if (!mappings.isEmpty()) {
            throw new MappingException(("Mapping-related bulk errors"));
        }
        // 나머지 4xx 등은 BadEvent로 묶기
        if (!badRequests.isEmpty()) {
            throw new BadEventException(("Bad-event bulk errors"));
        }
        // 혹시 분류되지 않았는데 errors=true인 케이스
        throw new RetryableEsException("Bulk errors detected but could not classify precisely.");
    }

    private static boolean isRetryable(BulkResponseItem i) {
//        if (i.status() == null)
//            return true;
        int s = i.status();
        // 429/5xx는 재시도
        if (s == 429 || (s >= 500 && s <= 599)) return true;
        // 413 Payload Too Large: 배치 크기 조정 후 재시도
        if (s == 413) return true;
        return false;
    }

    private static boolean isMapping(BulkResponseItem i) {
        if (i.error() == null) return false;
        var type = safe(i.error().type());
        // 대표적인 매핑/문서 구조 문제들
        if (type.contains("mapper_parsing_exception")) return true;
        if (type.contains("strict_dynamic_mapping_exception")) return true;
        if (type.contains("illegal_argument_exception")) return true; // 종종 매핑 충돌로 옴
        // 상태 코드로도 보조 판정
        Integer s = i.status();
        return s != null && (s == 400);
    }

    private static boolean isBadEvent(BulkResponseItem i) {
        // 그 외 4xx 전반은 이벤트/입력 문제로 보고 DLQ
        Integer s = i.status();
        if (s == null) return false;
        if (s >= 400 && s < 500) {
            // 단, 매핑은 위에서 이미 걸러짐. 여기선 나머지 4xx.
            return !isMapping(i);
        }
        // 409 버전충돌: 보통 재시도 무의미(외부버전 사용 시 오래된 이벤트)
        if (s == 409) return true;
        return false;
    }

//    private static String buildMessage(String title, List<BulkResponseItem> items) {
//        String detail = items.stream()
//                .limit(5)
//                .map(i -> String.format("#%s op=%s status=%s errorType=%s reason=%s",
//                        i.itemId(), i.operationType(), i.status(),
//                        i.error() != null ? i.error().type() : "null",
//                        i.error() != null ? i.error().reason() : "null"))
//                .collect(Collectors.joining("; "));
//        return title + " -> " + detail + (items.size() > 5 ? " ..." : "");
//    }

    private static String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}
