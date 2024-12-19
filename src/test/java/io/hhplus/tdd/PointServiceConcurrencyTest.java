package io.hhplus.tdd;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.PointHistory;
import org.springframework.test.annotation.DirtiesContext;
import io.hhplus.tdd.point.UserPointLockManager;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PointServiceConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointLockManager lockManager;

    @Test
    void testConcurrentChargePoint() throws InterruptedException {
        // given
        long userId = 1L;
        int threadCount = 10;
        long chargeAmount = 100L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when: 동시에 10개의 충전 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    UserPoint result = pointService.chargePoint(userId, chargeAmount);
                    System.out.println(String.format(
                            "[Thread-%d] 충전 완료: %d원",
                            Thread.currentThread().getId(),
                            result.point()
                    ));
                } catch (Exception e) {
                    exceptions.add(e);
                    System.err.println(String.format(
                            "[Thread-%d] 충전 실패: %s",
                            Thread.currentThread().getId(),
                            e.getMessage()
                    ));
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint finalPoint = pointService.getUserPoint(userId);
        List<PointHistory> histories = pointService.getPointHistory(userId);

        System.out.println("\n=== 테스트 결과 ===");
        System.out.println("최종 포인트: " + finalPoint.point());
        System.out.println("이력 개수: " + histories.size());

        // 검증
        assertTrue(exceptions.isEmpty(), "모든 요청이 성공해야 함");
        assertEquals(chargeAmount * threadCount, finalPoint.point(), "최종 포인트 확인");
        assertEquals(threadCount, histories.size(), "이력 개수 확인");
    }

    @Test
    void testConcurrentUsePoint() throws InterruptedException {
        // given
        long userId = 1L;
        int threadCount = 10;
        long useAmount = 100L;

        // 초기 포인트 설정 (사용할 금액의 2배)
        long initialAmount = useAmount * threadCount * 2;
        pointService.chargePoint(userId, initialAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when: 동시에 10개의 사용 요청
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    UserPoint result = pointService.usePoint(userId, useAmount);
                    System.out.println(String.format(
                            "[Thread-%d] 사용 완료: %d원",
                            Thread.currentThread().getId(),
                            result.point()
                    ));
                } catch (Exception e) {
                    exceptions.add(e);
                    System.err.println(String.format(
                            "[Thread-%d] 사용 실패: %s",
                            Thread.currentThread().getId(),
                            e.getMessage()
                    ));
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint finalPoint = pointService.getUserPoint(userId);
        List<PointHistory> histories = pointService.getPointHistory(userId);

        System.out.println("\n=== 테스트 결과 ===");
        System.out.println("초기 포인트: " + initialAmount);
        System.out.println("최종 포인트: " + finalPoint.point());
        System.out.println("이력 개수: " + histories.size());

        // 검증
        assertTrue(exceptions.isEmpty(), "모든 요청이 성공해야 함");
        assertEquals(initialAmount - (useAmount * threadCount), finalPoint.point(), "최종 포인트 확인");
        assertEquals(threadCount + 1, histories.size(), "이력 개수 확인 (초기 충전 포함)");
    }

    @Test
    void testMixedConcurrentOperations() throws InterruptedException {
        // given
        long userId = 1L;
        int threadCount = 15;  // 각 작업당 5개씩, 총 15개
        long amount = 100L;

        // 초기 포인트 설정
        UserPoint initialPoint = pointService.chargePoint(userId, amount * 10);
        System.out.println(String.format("[시작] 초기 포인트: %d원", initialPoint.point()));

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when: 충전, 사용, 조회를 동시에 실행
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    String threadInfo = String.format(
                            "[Thread-%d] 작업-%d: ",
                            Thread.currentThread().getId(),
                            index
                    );

                    switch (index % 3) {
                        case 0 -> {
                            UserPoint result = pointService.chargePoint(userId, amount);
                            System.out.println(threadInfo + String.format(
                                    "충전 완료 (현재: %d원)",
                                    result.point()
                            ));
                        }
                        case 1 -> {
                            UserPoint result = pointService.usePoint(userId, amount);
                            System.out.println(threadInfo + String.format(
                                    "사용 완료 (현재: %d원)",
                                    result.point()
                            ));
                        }
                        case 2 -> {
                            UserPoint result = pointService.getUserPoint(userId);
                            System.out.println(threadInfo + String.format(
                                    "조회 완료 (현재: %d원)",
                                    result.point()
                            ));
                        }
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    System.err.println(String.format(
                            "[Thread-%d] 작업-%d 실패: %s",
                            Thread.currentThread().getId(),
                            index,
                            e.getMessage()
                    ));
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        UserPoint finalPoint = pointService.getUserPoint(userId);
        List<PointHistory> histories = pointService.getPointHistory(userId);

        System.out.println("\n=== 테스트 결과 ===");
        System.out.println("초기 포인트: " + initialPoint.point());
        System.out.println("최종 포인트: " + finalPoint.point());
        System.out.println("이력 개수: " + histories.size());

        // 검증
        assertTrue(exceptions.isEmpty(), "모든 요청이 성공해야 함");
        assertEquals(amount * 10 + (amount * 5 - amount * 5), finalPoint.point(), "최종 포인트 확인");
        assertEquals(11, histories.size(), "이력 개수 확인");
    }
}