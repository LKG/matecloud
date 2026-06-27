package vip.mate.starter.lock;

/**
 * 在 {@code waitTime} 内未获取到锁时的处理策略。
 *
 * @author mateaix
 */
public enum LockTimeoutStrategy {

    /** 抛 {@link DistributedLockException}(默认, 保持原有行为)。 */
    FAIL,

    /** 不执行目标方法, 直接返回 {@code null}(适合"拿不到锁就放弃本次"的幂等/防抖场景)。 */
    RETURN_NULL
}
