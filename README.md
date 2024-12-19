# 포인트 서비스 동시성 제어 분석

## 📌 개요
현재 구현된 포인트 서비스에는 사용자의 포인트 충전/사용에 대한 동시성 제어가 구현되어 있음

## 🔒 동시성 제어 방식
Lock 기반 동시성 제어

## ✨ 주요 특징
* 사용자별 독립적인 락
  * ConcurrentHashMap으로 사용자별 ReentrantLock 관리
  * 서로 다른 사용자의 트랜잭션은 독립적 실행

* 타임아웃 메커니즘 
  * tryLock(timeout, unit) 사용으로 데드락 방지 
  * 5초 타임아웃으로 무한 대기 방지

* 락 조정 
  * 트랜잭션의 핵심 부분만 락으로 보호 
  * 불필요한 대기 시간 최소화

## 🛡️ 구현된 안전장치
검증 로직(PointValidator 일부)

    @Component
    public class PointValidator {
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

트랜잭션 안전성(PointService)

    try {
    if (!lockManager.tryLock(id, 5, TimeUnit.SECONDS)) {
        throw new RuntimeException("포인트 처리 중 타임아웃이 발생했습니다.");
    }
    try {
        // 트랜잭션 로직
    } finally {
        lockManager.unlock(id);
    }


## 🚀 성능 분석
장점
1. 높은 동시성
   사용자별 독립적 락으로 간섭 최소화
   전체 시스템 처리량 향상
2. 안전성
   락 획득/해제의 명확한 생명주기
   finally 블록으로 확실한 락 해제
3. 확장성
   ConcurrentHashMap으로 동적 사용자 확장
   효율적인 메모리 관리
## ⚠️ 잠재적 이슈
* 메모리 사용 
  * 사용자별 Lock 객체 생성 
  * 미사용 Lock 객체 정리 필요 
* 타임아웃 설정 
  * 5초 타임아웃이 일부 상황에서 부적절할 수 있음 
  * 상황별 최적화 필요
## 🧪 테스트 케이스
1. 동시 포인트 충전/사용에 대한 테스트 케이스
2. 다수 유저가 api를 요청할 경우에 대비, 조회, 충전, 사용 스레드를 생성 무작위로 실행   
## ✅ 검증 항목
1. 동시 충전 시 금액 정확성
2. 동시 사용 시 잔액 일관성
3. 혼합 작업 시 데이터 정합성 
## 🔄 개선 제안사항
1. Lock 타임아웃 설정 최적화 
2. 미사용 Lock 객체 정리 메커니즘
3. 실패 시 재시도 로직
---