package net.mineabyss.cardinal.api.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;

/**
 * Borrowed this class from <a href="https://github.com/LuckPerms/LuckPerms">LuckPerms</a>
 */
public abstract class AsyncInterface {

    protected <T> CompletableFuture<T> future(Callable<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new CompletionException(e);
            }
        }, new ForkJoinPool());
    }

    /**
     * Executes the given runnable asynchronously using a new ForkJoinPool.
     * If the runnable throws an exception, it is rethrown as a CompletionException.
     *
     * @param runnable a Throwing.Runnable that is executed asynchronously
     * @return a CompletableFuture that completes when the runnable has been executed
     */
    protected CompletableFuture<Void> future(Throwing.Runnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new CompletionException(e);
            }
        }, new ForkJoinPool());
    }

}