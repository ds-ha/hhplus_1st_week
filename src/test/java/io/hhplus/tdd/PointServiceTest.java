package io.hhplus.tdd;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PointServiceTest {
    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    private PointService pointService;

    @BeforeEach
    void setUp() {
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    // 1. getUserPoint 테스트
    @Test
    void testGetUserPointWithNegativeId() {
        // given
        long negativeId = -1L;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.getUserPoint(negativeId);
        });

        // then
        assertEquals("유효하지 않은 ID입니다.", exception.getMessage());
    }

    // 2. chargePoint 테스트들
    @Test
    void testChargePointWithInvalidId() {
        // given
        long invalidId = -1L;
        long amount = 1000L;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(invalidId, amount);
        });

        // then
        assertEquals("유효하지 않은 ID입니다.", exception.getMessage());
    }

    @Test
    void testChargePointWithZeroAmount() {
        // given
        long userId = 1L;
        long amount = 0L;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, amount);
        });

        // then
        assertEquals("충전 포인트는 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    void testChargePointWithNegativeAmount() {
        // given
        long userId = 1L;
        long amount = -1000L;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, amount);
        });

        // then
        assertEquals("충전 포인트는 0보다 커야 합니다.", exception.getMessage());
    }


    // 3. usePoint 테스트들
    @Test
    void testUsePointWithInvalidId() {
        // given
        long invalidId = -1L;
        long amount = 1000L;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(invalidId, amount);
        });

        // then
        assertEquals("유효하지 않은 ID입니다.", exception.getMessage());
    }

    @Test
    void testUsePointWithZeroAmount() {
        // given
        long userId = 1L;
        long amount = 0L;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, amount);
        });

        // then
        assertEquals("사용 포인트는 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    void testUsePointWithNegativeAmount() {
        // given
        long userId = 1L;
        long amount = -1000L;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, amount);
        });

        // then
        assertEquals("사용 포인트는 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    void testUsePointWithInsufficientBalance() {
        // given
        long userId = 1L;
        long currentAmount = 500L;
        long useAmount = 1000L;

        UserPoint currentPoint = new UserPoint(userId, currentAmount, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(currentPoint);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, useAmount);
        });

        // then
        assertEquals("포인트가 부족합니다.", exception.getMessage());

        verify(userPointTable).selectById(userId);
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    // getPointHistory 테스트들
    @Test
    void testGetPointHistoryWithInvalidId() {
        // given
        long invalidId = -1L;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.getPointHistory(invalidId);
        });

        // then
        assertEquals("유효하지 않은 ID입니다.", exception.getMessage());
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
