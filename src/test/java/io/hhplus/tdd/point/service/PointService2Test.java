package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointShortageException;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

@SpringBootTest
class PointService2Test {

    @Autowired
    PointService pointService;

    @Test
    @DisplayName("포인트 충전 동시성 체크")
    void chargePointTest() throws InterruptedException {
        //given
        pointService.chargePoint(0, 10000);

        //when
        Runnable A = () -> {
            UserPoint userPoint1 = pointService.getUserPoint(0);
            UserPoint userPoint2 = pointService.chargePoint(0, userPoint1.point() + 3000);
            System.out.println("point " + userPoint2.point());
        };

        Runnable B = () -> {
            UserPoint userPoint1 = pointService.getUserPoint(0);
            UserPoint userPoint2 = pointService.chargePoint(0, userPoint1.point() + 4000);
            System.out.println("point " + userPoint2.point());
        };

        Runnable C = () -> {
            UserPoint userPoint1 = pointService.getUserPoint(0);
            UserPoint userPoint2 = pointService.chargePoint(0, userPoint1.point() + 2000);
            System.out.println("point " + userPoint2.point());
        };

        CompletableFuture.runAsync(A).thenCompose((a) -> CompletableFuture.runAsync(B).thenCompose((b) -> CompletableFuture.runAsync(C))).join();

        Thread.sleep(1000); // 1000ms 후에 테스트 종료(결과 값을 확인)

        //then
        UserPoint userPoint = pointService.getUserPoint(0);
        assertThat(userPoint.point()).isEqualTo(10000 + 3000 + 4000 + 2000);
    }

    @Test
    @DisplayName("포인트 사용 동시성 체크")
    void usePointTest() throws InterruptedException {
        //given
        pointService.chargePoint(0, 10000);

        //when
        Runnable A = () -> {
            try {
                UserPoint userPoint1 = pointService.usePoint(0, 3000);
                System.out.println("point1" + userPoint1.point());
            } catch (PointShortageException e) {
                throw new RuntimeException(e);
            }
        };

        Runnable B = () -> {
            try {
                UserPoint userPoint1 = pointService.usePoint(0, 4000);
                System.out.println("point1" + userPoint1.point());
            } catch (PointShortageException e) {
                throw new RuntimeException(e);
            }
        };

        Runnable C = () -> {
            try {
                UserPoint userPoint1 = pointService.usePoint(0, 3000);
                System.out.println("point1" + userPoint1.point());
            } catch (PointShortageException e) {
                throw new RuntimeException(e);
            }
        };

        CompletableFuture.runAsync(A).thenCompose((a) -> CompletableFuture.runAsync(B).thenCompose((b) -> CompletableFuture.runAsync(C))).join();

        Thread.sleep(1000); // 1000ms 후에 테스트 종료(결과 값을 확인)

        //then
        UserPoint userPoint = pointService.getUserPoint(0);
        assertThat(userPoint.point()).isEqualTo(10000 - 3000 - 4000 - 3000);
    }


    @Test
    @DisplayName("포인트 사용 동시성 체크2")
    public void usePointTest2() throws InterruptedException {
        //given
        long amount = 4000L;
        pointService.chargePoint(1L, 30000);

        int threadCount = 3;

        // thread 사용할 수 있는 서비스 선언, 몇 개의 스레드 사용할건지 지정
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        CountDownLatch latch = new CountDownLatch (threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    synchronized (this)
                    {
                        UserPoint userPoint = pointService.getUserPoint(1L);

                        if(userPoint.point() < amount) {
                            throw new PointShortageException("포인트 부족");
                        }

                        pointService.usePoint(1L, 3000L);
                    }

                } catch (PointShortageException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //then
        UserPoint userPoint = pointService.getUserPoint(1L);
        assertThat(userPoint.point()).isEqualTo(30000 - 3000 - 3000 - 3000);
    }
}