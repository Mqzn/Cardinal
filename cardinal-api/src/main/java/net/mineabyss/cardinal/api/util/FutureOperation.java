package net.mineabyss.cardinal.api.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A wrapper around CompletableFuture that provides fluent error handling and success callbacks
 * @author Mqzen
 * @date 5/6/2025 (dd/MM/yyyy)
 */
public final class FutureOperation<T> {
    
    private final CompletableFuture<T> future;
    
    private FutureOperation(CompletableFuture<T> future) {
        this.future = future;
    }
    
    /**
     * Create a FutureWrapper from a CompletableFuture
     */
    public static <T> FutureOperation<T> of(CompletableFuture<T> future) {
        return new FutureOperation<>(future);
    }

    public static <T> FutureOperation<T> completed(@Nullable T value) {
        return of(CompletableFuture.completedFuture(value));
    }
    
    /**
     * Handle success with automatic error handling (printStackTrace)
     */
    public FutureOperation<T> onSuccess(Consumer<T> onSuccess) {
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
            } else if (onSuccess != null) {
                onSuccess.accept(result);
            }
        });
        return this;
    }
    
    /**
     * Handle success for CompletableFuture<Void> with automatic error handling
     */
    public FutureOperation<T> onSuccess(Runnable onSuccess) {
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
            } else if (onSuccess != null) {
                onSuccess.run();
            }
        });
        return this;
    }
    
    /**
     * Handle errors with custom error handler
     */
    public FutureOperation<T> onError(Consumer<Throwable> onError) {
        future.exceptionally((ex) -> {
            if (ex != null && onError != null) {
                onError.accept(ex);
            }
            return null;
        });
        return this;
    }
    
    /**
     * Handle both success and error
     */
    public FutureOperation<T> handle(Consumer<T> onSuccess, Consumer<Throwable> onError) {
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                if (onError != null) {
                    onError.accept(ex);
                } else {
                    ex.printStackTrace();
                }
            } else if (onSuccess != null) {
                onSuccess.accept(result);
            }
        });
        return this;
    }
    
    /**
     * Handle both success and error for CompletableFuture<Void>
     */
    public FutureOperation<T> handle(Runnable onSuccess, Consumer<Throwable> onError) {
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                if (onError != null) {
                    onError.accept(ex);
                } else {
                    ex.printStackTrace();
                }
            } else if (onSuccess != null) {
                onSuccess.run();
            }
        });
        return this;
    }
    
    /**
     * Send a message to CommandSender on success
     */
    public FutureOperation<T> sendMessage(CommandSender sender, String successMessage) {
        return onSuccess(result -> sender.sendRichMessage(successMessage));
    }
    
    /**
     * Send a message to CommandSender on success (for Void futures)
     */
    public FutureOperation<T> sendMessageOnSuccess(CommandSender sender, String successMessage) {
        return onSuccess(() -> sender.sendRichMessage(successMessage));
    }
    
    /**
     * Send messages on both success and error
     */
    public FutureOperation<T> sendMessages(CommandSender sender, String successMessage, String errorMessage) {
        return handle(
            result -> sender.sendRichMessage(successMessage),
            ex -> {
                ex.printStackTrace();
                if (sender == null || (sender instanceof Player playerSender && !playerSender.isOnline())) {
                    return; // Avoid NPE if sender is null
                }
                sender.sendRichMessage(errorMessage);
            }
        );
    }
    
    /**
     * Send messages on both success and error (for Void futures)
     */
        public FutureOperation<T> sendMessagesOnComplete(CommandSender sender, String successMessage, String errorMessage) {
        return handle(
            () -> sender.sendRichMessage(successMessage),
            ex -> {
                ex.printStackTrace();
                if (sender == null || (sender instanceof Player playerSender && !playerSender.isOnline())) {
                    return; // Avoid NPE if sender is null
                }
                sender.sendRichMessage(errorMessage);
            }
        );
    }
    
    /**
     * Send error message to CommandSender on error
     */
    public FutureOperation<T> sendErrorMessage(CommandSender sender, String errorMessage) {
        return onError(ex -> {
            ex.printStackTrace();
            if (sender == null || (sender instanceof Player playerSender && !playerSender.isOnline())) {
                return; // Avoid NPE if sender is null
            }
            sender.sendRichMessage(errorMessage);
        });
    }
    
    /**
     * Only handle errors with printStackTrace (no success callback)
     */
    public FutureOperation<T> printErrors() {
        return onError(Throwable::printStackTrace);
    }
    
    /**
     * Transform the result and return a new FutureWrapper
     */
    public <U> FutureOperation<U> map(Function<T, U> mapper) {
        return FutureOperation.of(future.thenApply(mapper));
    }
    
    /**
     * Flat map to another CompletableFuture
     */
    public <U> FutureOperation<U> flatMap(Function<T, CompletableFuture<U>> mapper) {
        return FutureOperation.of(future.thenCompose(mapper));
    }
    
    /**
     * Get the underlying CompletableFuture if needed
     */
    public CompletableFuture<T> unwrap() {
        return future;
    }
    
    /**
     * Execute after completion (both success and error)
     */
    public FutureOperation<T> always(Runnable action) {
        future.whenComplete((result, ex) -> {
            if (action != null) {
                action.run();
            }
        });
        return this;
    }

    /**
     * Execute a Runnable after successful completion
     */
    public FutureOperation<Void> thenRun(Runnable action) {
        return FutureOperation.of(future.thenRun(action));
    }

    /**
     * Apply a function to the result and return a new FutureWrapper
     */
    public <U> FutureOperation<U> thenApply(Function<T, U> function) {
        return FutureOperation.of(future.thenApply(function));
    }

    /**
     * Apply a function to the result and return a new FutureWrapper
     */
    public <U> FutureOperation<U> thenApplyAsync(Function<T, U> function) {
        return FutureOperation.of(future.thenApplyAsync(function));
    }

    /**
     * Compose with another CompletableFuture-returning function
     */
    public <U> FutureOperation<U> thenCompose(Function<T, CompletableFuture<U>> function) {
        return FutureOperation.of(future.thenCompose(function));
    }

    public T join() {
        return unwrap().join();
    }

}