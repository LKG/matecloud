package vip.mate.base.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪 ID 的<b>日志侧</b>单一出处 —— 全仓只认 MDC 的 {@value #MDC_KEY} 槽,
 * 与 {@code mate-defaults.yml} 里的日志 pattern {@code [%X{traceId},%X{spanId}]} 对齐。
 *
 * <p><b>与 Micrometer/OTel 的关系(关键)</b>:当服务装了 {@code mate-monitor-starter}
 * 且当前处于一个 active span 时, Micrometer 的关联机制<b>已经</b>把真实 traceId 写进了
 * 同名 MDC 槽。本工具的全部写入路径都走 {@link #bindIfAbsent},<b>绝不覆盖</b> OTel 已写的值
 * —— 即「OTel 在就用 OTel 的, OTel 不在(或非 span 入口, 如 Dubbo provider / job / MQ)
 * 才兜底生成」。这样日志里的 traceId 永远是单一、连续的一个。
 *
 * <p>纯类型、零框架依赖(仅 SLF4J MDC), 故落在 {@code mate-base}。
 *
 * @author mateaix
 */
public final class MateTrace {

    /** MDC 槽名 —— 必须等于日志 pattern 里的 {@code %X{traceId}} 与 OTel 的 MDC key。 */
    public static final String MDC_KEY = "traceId";

    private MateTrace() {
    }

    /** 当前线程的 traceId(可能为 null)。 */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    /** 是否已有 traceId(OTel 写的或本工具兜底写的)。 */
    public static boolean hasCurrent() {
        String v = MDC.get(MDC_KEY);
        return v != null && !v.isEmpty();
    }

    /**
     * 仅在<b>当前没有</b> traceId 时写入 —— 不覆盖 OTel/上游已建立的值。
     *
     * @param traceId 期望写入的 id(为空则忽略)
     * @return 写入后(或本就存在)的有效 traceId; 若入参为空且当前也无值则返回 null
     */
    public static String bindIfAbsent(String traceId) {
        String existing = MDC.get(MDC_KEY);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        if (traceId == null || traceId.isEmpty()) {
            return null;
        }
        MDC.put(MDC_KEY, traceId);
        return traceId;
    }

    /**
     * 确保当前线程有 traceId:已有则原样返回, 没有则生成一个并写入。
     * 用于 OTel 未覆盖的入口(Dubbo provider 兜底 / 定时任务 / MQ 消费)。
     */
    public static String bindOrNew() {
        String existing = MDC.get(MDC_KEY);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        String id = newTraceId();
        MDC.put(MDC_KEY, id);
        return id;
    }

    /**
     * 强制写入(非空时覆盖)。仅用于明确「这条线程的 traceId 就该是这个」的入口,
     * 如 Dubbo provider 兜底分支(OTel 未接管时)。普通路径请用 {@link #bindIfAbsent}。
     */
    public static void bind(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(MDC_KEY, traceId);
        }
    }

    /** 清除 traceId(池化线程必须在收尾时调用, 避免串号)。 */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    /**
     * 生成一个与 OTel 等宽的 32 位小写十六进制 id, 兜底入口用。
     * 不追求全局严格唯一(那是 OTel 的活), 只为「日志可 grep、跨服务可关联」。
     */
    public static String newTraceId() {
        UUID u = UUID.randomUUID();
        return hex16(u.getMostSignificantBits()) + hex16(u.getLeastSignificantBits());
    }

    private static String hex16(long value) {
        String hex = Long.toHexString(value);
        if (hex.length() == 16) {
            return hex;
        }
        return "0".repeat(16 - hex.length()) + hex;
    }
}
