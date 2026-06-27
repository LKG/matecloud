package vip.mate.starter.rediskey;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis key 配置。
 *
 * @author mateaix
 */
@ConfigurationProperties(prefix = "mate.redis.key")
public class RedisKeyProperties {

    /**
     * 全局 key 前缀。默认空(不改变现有 key 行为)。多服务共享同一 Redis 时, 设为各自服务名
     * (如 {@code "mate-system:"})可做跨服务隔离。注意: 若有<b>跨服务共享</b>的 key(如分布式锁),
     * 不要给那些 key 加服务前缀。
     */
    private String prefix = "";

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
