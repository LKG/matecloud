package vip.mate.starter.rediskey;

/**
 * Redis key 统一构建入口 —— 集中前缀 + 模板格式化 + 参数个数校验, 取代全仓散落的拼字符串。
 *
 * <p><b>解决什么</b>:① 多服务共享同一 Redis 时, 可配 {@code mate.redis.key.prefix}
 * (如各服务名)做隔离, 避免跨服务 key 撞车;② {@code %s} 占位的参数个数不匹配会<b>当场抛错</b>,
 * 而不是 {@link String#format} 那样静默漏参/吞多余参。
 *
 * <p>前缀在启动时由 {@code RedisKeyAutoConfiguration} 从配置注入(matecloud 的静态持有者范式,
 * 不在每次调用时 {@code getBean} —— 区别于 damai 的 {@code SpringUtil} 写法)。
 *
 * <pre>{@code
 *   redissonClient.getBucket(MateRedisKey.of("user:login:%s", userId));
 *   redissonClient.getBucket(MateRedisKey.of(SystemRedisKey.USER_LOGIN, userId)); // 枚举目录
 * }</pre>
 *
 * @author mateaix
 */
public final class MateRedisKey {

    private static volatile String prefix = "";

    private MateRedisKey() {
    }

    /** 由 {@code RedisKeyAutoConfiguration} 启动时注入。 */
    static void setPrefix(String prefix) {
        MateRedisKey.prefix = (prefix == null) ? "" : prefix;
    }

    /** 当前全局前缀(测试/诊断用)。 */
    public static String prefix() {
        return prefix;
    }

    /**
     * 用模板 + 参数构建 key (前缀 + 格式化)。模板用 {@code %s} 占位, 个数须与 {@code args} 一致。
     */
    public static String of(String template, Object... args) {
        if (template == null || template.isEmpty()) {
            throw new IllegalArgumentException("Redis key template must not be empty");
        }
        int slots = countPlaceholders(template);
        int given = (args == null) ? 0 : args.length;
        if (slots != given) {
            throw new IllegalArgumentException("Redis key template '" + template + "' expects "
                    + slots + " arg(s) but got " + given);
        }
        return prefix + (given == 0 ? template : String.format(template, args));
    }

    /** 用枚举目录 {@link RedisKeyDef} 构建 key。 */
    public static String of(RedisKeyDef def, Object... args) {
        return of(def.template(), args);
    }

    private static int countPlaceholders(String template) {
        int count = 0;
        for (int i = 0; i + 1 < template.length(); i++) {
            if (template.charAt(i) == '%' && template.charAt(i + 1) == 's') {
                count++;
                i++;
            }
        }
        return count;
    }
}
