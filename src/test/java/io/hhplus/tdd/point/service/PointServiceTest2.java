package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest
class PointServiceTest2 {

    @Autowired
    UserPointTable userPointTable;

    @Autowired
    PointHistoryTable pointHistoryTable;

    // 포인트 충전 여부 (동시성 체크)
    @Test
    void chargePointTest() throws Exception {
        //given
        long chargePoint = 3000L;
        AtomicReference<UserPoint> userPoint = new AtomicReference<>(userPointTable.insertOrUpdate(0, 10000));

        //when
        // 쓰레드 3개 가정
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                synchronized (userPointTable) {
                    UserPoint up = userPointTable.selectById(0L);
                    userPoint.set(userPointTable.insertOrUpdate(0L, up.point() + chargePoint));
                    System.out.println(userPoint.get().point());

                    try {
                        Thread.sleep(1); // 추가적인 딜레이(복잡한 로직이 추가된다고 가정)
                    } catch (InterruptedException e) {}
                }

            }).start();
        }

        Thread.sleep(1000); // 1000ms 후에 테스트 종료(결과 값을 확인)

        //then
        Assertions.assertThat(userPoint.get().point()).isEqualTo(19000);
    }

    // 보유 포인트가 사용할 포인트보다 많을 경우 포인트 사용(동시성 체크2)
    @Test
    public void usePointTest() throws InterruptedException {
        //given
        long amount = 4000L;
        AtomicReference<UserPoint> userPoint = new AtomicReference<>(userPointTable.insertOrUpdate(0, 10000));

        int threadCount = 3;

        // thread 사용할 수 있는 서비스 선언, 몇 개의 스레드 사용할건지 지정
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        CountDownLatch latch = new CountDownLatch (threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    synchronized (userPointTable)
                    {
                        UserPoint up = userPointTable.selectById(0L);

                        //수행할 작업(메소드)
                        if(up.point() > amount) {
                            userPoint.set(userPointTable.insertOrUpdate(0L, up.point() - amount));
                            System.out.println(userPoint.get().point());
                        } else {
                            System.out.println("포인트 부족!!!");
                        }
                    }

                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //then
        Assertions.assertThat(userPoint.get().point()).isEqualTo(2000);
    }
}