package com.drychan.handler.strategy;

public interface CommandStrategy {
    /**
     *
     * @return if the process completed successfully
     */
    boolean process(int userId);
}
