package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final ReentrantLock lock = new ReentrantLock();

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(long id) {
        validateId(id);
        return userPointTable.selectById(id);
    }

    public List<PointHistory> getPointHistory(long id) {
        validateId(id);
        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint chargePoint(long id, long amount) {
        validateId(id);
        validateChargeAmount(amount);

        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("포인트 충전 처리 중 타임아웃이 발생했습니다.");
            }

            try {
                UserPoint currentPoint = getUserPoint(id);
                validateAmountOverflow(currentPoint.point(), amount);

                UserPoint updatedPoint = userPointTable.insertOrUpdate(
                        id,
                        currentPoint.point() + amount
                );

                long updateTime = System.currentTimeMillis();
                pointHistoryTable.insert(id, amount, TransactionType.CHARGE, updateTime);

                return updatedPoint;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("포인트 충전이 중단되었습니다.", e);
        }
    }

    public UserPoint usePoint(long id, long amount) {
        validateId(id);
        validateUseAmount(amount);

        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("포인트 사용 처리 중 타임아웃이 발생했습니다.");
            }

            try {
                UserPoint currentPoint = getUserPoint(id);
                validateBalance(currentPoint.point(), amount);

                UserPoint updatedPoint = userPointTable.insertOrUpdate(
                        id,
                        currentPoint.point() - amount
                );

                long updateTime = System.currentTimeMillis();
                pointHistoryTable.insert(id, amount, TransactionType.USE, updateTime);

                return updatedPoint;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("포인트 사용이 중단되었습니다.", e);
        }
    }

    // 검증 메서드들
    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID입니다.");
        }
    }

    private void validateChargeAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 포인트는 0보다 커야 합니다.");
        }
    }

    private void validateUseAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 포인트는 0보다 커야 합니다.");
        }
    }

    private void validateBalance(long currentAmount, long useAmount) {
        if (currentAmount < useAmount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
    }

    private void validateAmountOverflow(long current, long amount) {
        if (Long.MAX_VALUE - current < amount) {
            throw new IllegalArgumentException("충전 포인트가 너무 큽니다.");
        }
    }
}
