package net.mineabyss.core.util;

import net.mineabyss.core.Cardinal;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class Tasks {
	
	public static void runAsync(@NotNull Runnable runnable) {
		Bukkit.getScheduler().runTaskAsynchronously(Cardinal.getInstance(), runnable);
	}
	
	public static void runAsyncLater(@NotNull Runnable runnable, long delay) {
		Bukkit.getScheduler().runTaskLaterAsynchronously(Cardinal.getInstance(), runnable, delay);
	}
	
	public static void runSync(@NotNull Runnable runnable) {
		Bukkit.getScheduler().runTask(Cardinal.getInstance(), runnable);
	}
	
	public static void runLater(@NotNull Runnable runnable, long delay) {
		Bukkit.getScheduler().runTaskLater(Cardinal.getInstance(), runnable, delay);
	}

	public static void runSyncLater(Runnable runnable, long l) {
		Bukkit.getScheduler().runTaskLater(Cardinal.getInstance(), runnable, l);
	}
}