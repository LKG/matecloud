package vip.mate.starter.job.trace;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import vip.mate.base.trace.MateTrace;

/**
 * 给每次 XXL-Job 任务执行注入 traceId —— 定时任务线程不经 HTTP/Dubbo 入口, 没有 active span,
 * 否则整段任务日志的 traceId 全空、无从 grep。
 *
 * <p>切 {@code @XxlJob} 注解的方法(job 业务方法即此): 入口若无 traceId 则生成一个,
 * 整个任务期间一致, {@code finally} 清理。job bean 为 Spring 组件时被代理, 切面即生效。
 *
 * @author mateaix
 */
@Aspect
public class JobTraceAspect {

    @Around("@annotation(com.xxl.job.core.handler.annotation.XxlJob)")
    public Object aroundJob(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean bound = !MateTrace.hasCurrent();
        if (bound) {
            MateTrace.bindOrNew();
        }
        try {
            return joinPoint.proceed();
        } finally {
            if (bound) {
                MateTrace.clear();
            }
        }
    }
}
