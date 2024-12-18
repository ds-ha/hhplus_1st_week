package io.hhplus.tdd;

// Spring 관련
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

// JUnit 관련
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 동시성 관련
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// 컬렉션 관련
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

// 프로젝트 클래스
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.PointHistory;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PointServiceConcurrencyTest {

    @Autowired
    private PointService pointService;

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
                    System.out.println("충전 완료: " + result.point()); // 로그 추가
                } catch (Exception e) {
                    exceptions.add(e);
                    System.err.println("충전 실패: " + e.getMessage());
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
                    pointService.usePoint(userId, useAmount);
                } catch (Exception e) {
                    exceptions.add(e);
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
        pointService.chargePoint(userId, amount * 10);  // 초기값 1000원

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when: 충전, 사용, 조회를 동시에 실행
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    switch (index % 3) {
                        case 0 -> pointService.chargePoint(userId, amount);  // 충전
                        case 1 -> pointService.usePoint(userId, amount);     // 사용
                        case 2 -> pointService.getUserPoint(userId);         // 조회
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    System.err.println("Operation failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await(15, TimeUnit.SECONDS);  // 시간 증가
        executorService.shutdown();

        // then
        UserPoint finalPoint = pointService.getUserPoint(userId);
        List<PointHistory> histories = pointService.getPointHistory(userId);

        // 검증
        assertTrue(exceptions.isEmpty(), "모든 요청이 성공해야 함");

        // 충전 5번, 사용 5번이므로 초기값 + (충전 - 사용) * amount
        long expectedBalance = amount * 10 + (amount * 5 - amount * 5);
        assertEquals(expectedBalance, finalPoint.point(), "최종 포인트 확인");

        // 이력은 초기 충전 1회 + 충전 5회 + 사용 5회 = 11회
        assertEquals(11, histories.size(), "이력 개수 확인");
    }
}