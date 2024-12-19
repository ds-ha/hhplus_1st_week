package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

@Component
public class UserPointLockManager {
    private final Map<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    
    public ReentrantLock getLockForUser(long userId) {
        return userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    public boolean tryLock(long userId, long timeout, TimeUnit unit) throws InterruptedException {
        return getLockForUser(userId).tryLock(timeout, unit);
    }

    public void unlock(long userId) {
        getLockForUser(userId).unlock();
    }
} 