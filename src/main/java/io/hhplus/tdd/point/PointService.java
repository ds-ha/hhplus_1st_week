package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final PointValidator validator;
    private final UserPointLockManager lockManager;

    public PointService(
            UserPointTable userPointTable,
            PointHistoryTable pointHistoryTable,
            PointValidator validator,
            UserPointLockManager lockManager) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
        this.validator = validator;
        this.lockManager = lockManager;
    }

    public UserPoint getUserPoint(long id) {
        validator.validateId(id);
        return userPointTable.selectById(id);
    }

    public List<PointHistory> getPointHistory(long id) {
        validator.validateId(id);
        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint chargePoint(long id, long amount) {
        validator.validateId(id);
        validator.validateChargeAmount(amount);

        try {
            if (!lockManager.tryLock(id, 5, TimeUnit.SECONDS)) {
                throw new RuntimeException("포인트 충전 처리 중 타임아웃이 발생했습니다.");
            }
            try {
                UserPoint currentPoint = getUserPoint(id);
                validator.validateAmountOverflow(currentPoint.point(), amount);

                UserPoint updatedPoint = userPointTable.insertOrUpdate(
                        id,
                        currentPoint.point() + amount
                );
                pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

                return updatedPoint;
            } finally {
                lockManager.unlock(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("포인트 충전이 중단되었습니다.", e);
        }
    }

    public UserPoint usePoint(long id, long amount) {
        validator.validateId(id);
        validator.validateUseAmount(amount);

        try {
            if (!lockManager.tryLock(id, 5, TimeUnit.SECONDS)) {
                throw new RuntimeException("포인트 사용 처리 중 타임아웃이 발생했습니다.");
            }
            try {
                UserPoint currentPoint = getUserPoint(id);
                validator.validateBalance(currentPoint.point(), amount);

                UserPoint updatedPoint = userPointTable.insertOrUpdate(
                        id,
                        currentPoint.point() - amount
                );
                pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

                return updatedPoint;
            } finally {
                lockManager.unlock(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("포인트 사용이 중단되었습니다.", e);
        }
    }
}
