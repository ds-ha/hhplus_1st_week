package io.hhplus.tdd.point;

import io.hhplus.tdd.point.exception.InsufficientPointException;
import org.springframework.stereotype.Component;

@Component
public class PointValidator {
    public void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID입니다.");
        }
    }

    public void validateChargeAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 포인트는 0보다 커야 합니다.");
        }
    }

    public void validateUseAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 포인트는 0보다 커야 합니다.");
        }
    }

    public void validateBalance(long currentAmount, long useAmount) {
        if (currentAmount < useAmount) {
            throw new InsufficientPointException();
        }
    }

    public void validateAmountOverflow(long current, long amount) {
        if (Long.MAX_VALUE - current < amount) {
            throw new IllegalArgumentException("충전 포인트가 너무 큽니다.");
        }
    }
} 