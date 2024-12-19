package io.hhplus.tdd.point.exception;

public class InsufficientPointException extends PointException {
    public InsufficientPointException() {
        super("포인트가 부족합니다.");
    }
}
