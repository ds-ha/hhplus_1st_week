package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.PointValidator;
import io.hhplus.tdd.point.UserPointLockManager;
import io.hhplus.tdd.point.exception.InsufficientPointException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PointServiceTest {
    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private PointValidator validator;

    @Mock
    private UserPointLockManager lockManager;

    private PointService pointService;

    @BeforeEach
    void setUp() {
        pointService = new PointService(userPointTable, pointHistoryTable, validator, lockManager);
    }

    // 1. getUserPoint 테스트
    @Test
    void testGetUserPointWithNegativeId() {
        // given
        long negativeId = -1L;
        doThrow(new IllegalArgumentException("유효하지 않은 ID입니다."))
                .when(validator).validateId(negativeId);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.getUserPoint(negativeId);
        });

        // then
        assertEquals("유효하지 않은 ID입니다.", exception.getMessage());
        verify(validator).validateId(negativeId);
    }

    // 2. chargePoint 테스트들
    @Test
    void testChargePointWithInvalidId() {
        // given
        long invalidId = -1L;
        long amount = 1000L;
        doThrow(new IllegalArgumentException("유효하지 않은 ID입니다."))
                .when(validator).validateId(invalidId);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(invalidId, amount);
        });

        // then
        assertEquals("유효하지 않은 ID입니다.", exception.getMessage());
        verify(validator).validateId(invalidId);
    }

    @Test
    void testChargePointWithZeroAmount() {
        // given
        long userId = 1L;
        long amount = 0L;
        doThrow(new IllegalArgumentException("충전 포인트는 0보다 커야 합니다."))
                .when(validator).validateChargeAmount(amount);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, amount);
        });

        // then
        assertEquals("충전 포인트는 0보다 커야 합니다.", exception.getMessage());
        verify(validator).validateChargeAmount(amount);
    }

    @Test
    void testChargePointWithNegativeAmount() {
        // given
        long userId = 1L;
        long amount = -1000L;
        doThrow(new IllegalArgumentException("충전 포인트는 0보다 커야 합니다."))
                .when(validator).validateChargeAmount(amount);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, amount);
        });

        // then
        assertEquals("충전 포인트는 0보다 커야 합니다.", exception.getMessage());
        verify(validator).validateChargeAmount(amount);
    }

    // 3. usePoint 테스트들
    @Test
    void testUsePointWithInvalidId() {
        // given
        long invalidId = -1L;
        long amount = 1000L;
        doThrow(new IllegalArgumentException("유효하지 않은 ID입니다."))
                .when(validator).validateId(invalidId);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(invalidId, amount);
        });

        // then
        assertEquals("유효하지 않은 ID입니다.", exception.getMessage());
        verify(validator).validateId(invalidId);
    }

    @Test
    void testUsePointWithZeroAmount() {
        // given
        long userId = 1L;
        long amount = 0L;
        doThrow(new IllegalArgumentException("사용 포인트는 0보다 커야 합니다."))
                .when(validator).validateUseAmount(amount);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, amount);
        });

        // then
        assertEquals("사용 포인트는 0보다 커야 합니다.", exception.getMessage());
        verify(validator).validateUseAmount(amount);
    }

    @Test
    void testUsePointWithNegativeAmount() {
        // given
        long userId = 1L;
        long amount = -1000L;
        doThrow(new IllegalArgumentException("사용 포인트는 0보다 커야 합니다."))
                .when(validator).validateUseAmount(amount);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, amount);
        });

        // then
        assertEquals("사용 포인트는 0보다 커야 합니다.", exception.getMessage());
        verify(validator).validateUseAmount(amount);
    }

    @Test
    void testUsePointWithInsufficientBalance() throws InterruptedException {
        // given
        long userId = 1L;
        long currentAmount = 500L;
        long useAmount = 1000L;

        UserPoint currentPoint = new UserPoint(userId, currentAmount, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(currentPoint);
        when(lockManager.tryLock(userId, 5, TimeUnit.SECONDS)).thenReturn(true);
        doThrow(new InsufficientPointException())
                .when(validator).validateBalance(currentAmount, useAmount);

        // when
        InsufficientPointException exception = assertThrows(InsufficientPointException.class, () -> {
            pointService.usePoint(userId, useAmount);
        });

        // then
        assertEquals("포인트가 부족합니다.", exception.getMessage());
        verify(validator).validateBalance(currentAmount, useAmount);
        verify(lockManager).tryLock(userId, 5, TimeUnit.SECONDS);
        verify(lockManager).unlock(userId);
    }

    // 4. getPointHistory 테스트들
    @Test
    void testGetPointHistoryWithInvalidId() {
        // given
        long invalidId = -1L;
        doThrow(new IllegalArgumentException("유효하지 않은 ID입니다."))
                .when(validator).validateId(invalidId);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.getPointHistory(invalidId);
        });

        // then
        assertEquals("유효하지 않은 ID입니다.", exception.getMessage());
        verify(validator).validateId(invalidId);
    }

    @Test
    void testGetPointHistoryEmpty() {
        // given
        long userId = 1L;
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.emptyList());

        // when
        List<PointHistory> histories = pointService.getPointHistory(userId);

        // then
        assertTrue(histories.isEmpty());
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    void testGetPointHistoryWithRecords() {
        // given
        long userId = 1L;
        List<PointHistory> expectedHistories = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(expectedHistories);

        // when
        List<PointHistory> histories = pointService.getPointHistory(userId);

        // then
        assertEquals(expectedHistories, histories);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }
}
