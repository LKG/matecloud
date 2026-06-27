package vip.mate.starter.rediskey;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 启动时把配置的前缀注入 {@link MateRedisKey} 静态持有者(matecloud 范式, 见 {@code WorkerIdHolder})。
 *
 * @author mateaix
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisKeyProperties.class)
public class RedisKeyAutoConfiguration {

    public RedisKeyAutoConfiguration(RedisKeyProperties properties) {
        MateRedisKey.setPrefix(properties.getPrefix());
    }
}
