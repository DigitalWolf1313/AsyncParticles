package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticleHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Mixin(value = PolytoneAsyncParticleHandler.class, remap = false)
public abstract class MixinPolytoneAsyncParticleHandler {
	@WrapOperation(method = "dispatch", at = @At(value = "INVOKE",
		target = "Ljava/util/concurrent/ForkJoinPool;submit(Ljava/lang/Runnable;)Ljava/util/concurrent/ForkJoinTask;"))
	private static ForkJoinTask<?> submitInit(ForkJoinPool pool, Runnable task, Operation<ForkJoinTask<?>> original) {
		if (!ConfigHelper.isPolytoneAsyncTick()) {
			return original.call(pool, task);
		}
		return AsyncTickBehavior.getInstance().getTickTaskManager().submitPrerequisite(task);
	}

	@Inject(method = "enqueue", at = @At("HEAD"), cancellable = true)
	private static void tickInCurrentBatch(CustomParticleInstance particle, CallbackInfo ci) {
		if (ConfigHelper.isPolytoneAsyncTick()) {
			particle.tickSync();
			ci.cancel();
		}
	}

	@Inject(method = "deferToMain", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"), cancellable = true)
	private static void deferToClient(Runnable action, CallbackInfo ci) {
		if (ConfigHelper.isPolytoneAsyncTick()) {
			PolytoneCompat.deferToMain(action);
			ci.cancel();
		}
	}
}
