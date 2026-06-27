package vip.mate.starter.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 3 (tools.jackson) 的 HTTP 序列化约定 —— Spring Boot 4 默认用 Jackson 3 做
 * {@code @ResponseBody} 转换, 此前 {@link JacksonConfiguration}(com.fasterxml ObjectMapper)的
 * 定制已<b>不再作用于 API 响应</b>。本类通过 Boot 4 的 {@link JsonMapperBuilderCustomizer} 把同一套
 * 约定加回 Jackson 3 的 HTTP mapper, 恢复 API 契约(日期格式等)。
 *
 * <p>这是「拥抱 Jackson 3」迁移的第一步: HTTP 出口走 Jackson 3 且符合约定。手动序列化处
 * (MQ/缓存等 105 处注入 com.fasterxml ObjectMapper)仍由 {@link JacksonConfiguration} 提供,
 * 后续逐步迁移到 tools.jackson 后即可下线 Jackson 2 栈。
 *
 * @author mateaix
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {
        "tools.jackson.databind.json.JsonMapper",
        "org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer"
})
public class Jackson3Configuration {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public JsonMapperBuilderCustomizer mateJacksonConventionsCustomizer() {
        return builder -> {
            SimpleModule javaTime = new SimpleModule("mate-javatime");
            javaTime.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME));
            javaTime.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME));
            javaTime.addSerializer(LocalDate.class, new LocalDateSerializer(DATE));
            javaTime.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE));
            javaTime.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME));
            javaTime.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME));
            builder.addModule(javaTime);

            // 与旧 JacksonConfiguration 一致的容错/格式约定。
            builder.configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        };
    }
}
