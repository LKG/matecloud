package vip.mate.starter.rediskey;

/**
 * Redis key 模板定义 —— 让各模块用<b>枚举</b>集中声明自己的 key, 取代散落的魔法字符串,
 * 可发现、防 typo。模板用 {@code %s} 占位。
 *
 * <pre>{@code
 * public enum SystemRedisKey implements RedisKeyDef {
 *     USER_LOGIN("user:login:%s"),
 *     CAPTCHA("captcha:%s");
 *     private final String template;
 *     SystemRedisKey(String t) { this.template = t; }
 *     public String template() { return template; }
 * }
 * // 用: redissonClient.getBucket(MateRedisKey.of(SystemRedisKey.USER_LOGIN, userId))
 * }</pre>
 *
 * @author mateaix
 */
public interface RedisKeyDef {

    /** Key 模板, 用 {@code %s} 占位 (如 {@code "user:login:%s"})。 */
    String template();
}
