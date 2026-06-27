package vip.mate.starter.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 分布式锁类型 —— 每种类型自己知道如何从 Redisson 取对应的 {@link RLock}
 * (JDK 21 switch 表达式, 无需工厂类)。
 *
 * @author mateaix
 */
public enum LockType {

    /** 可重入锁(默认) —— 同一线程可重复获取。 */
    REENTRANT,

    /** 公平锁 —— 按请求先后顺序获取, 防止线程饥饿。 */
    FAIR,

    /** 读写锁的读锁 —— 多读并发, 与同名写锁互斥。 */
    READ,

    /** 读写锁的写锁 —— 写独占, 与同名读/写锁互斥。 */
    WRITE;

    /**
     * 用同一个 {@code key} 取本类型对应的 Redisson 锁。READ/WRITE 共享同一把读写锁,
     * 因此读写方法用<b>相同的 name</b> 即可获得正确的读写互斥语义。
     */
    public RLock getLock(RedissonClient client, String key) {
        return switch (this) {
            case REENTRANT -> client.getLock(key);
            case FAIR -> client.getFairLock(key);
            case READ -> client.getReadWriteLock(key).readLock();
            case WRITE -> client.getReadWriteLock(key).writeLock();
        };
    }
}
