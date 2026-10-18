package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.TrackedParticleCountsMap;
import fun.qu_an.minecraft.asyncparticles.client.util.BusyWaitEvictingQueue;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleLimit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine implements ParticleEngineAddon {
	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts;
	@Mutable
	@Final
	@Shadow
	private Queue<Particle> particlesToAdd;
	@Mutable
	@Final
	@Shadow
	private Queue<TrackingEmitter> trackingEmitters;
	@Shadow
	@Final
	public Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Inject(order = 1500, method = "<init>", at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void clearParticles(CallbackInfo ci) {
		AsyncTickBehavior.getInstance().reset();
		// Guarantee: call remove for each particle when clearing particles.
		particlesToAdd.forEach(ParticleHelper::onClearParticle);
		particlesToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		trackingEmitters.forEach(ParticleHelper::onClearParticle);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		particles.values().forEach(g -> ((ParticleGroupAddition) g).asyncparticles$onClearParticles());
		ParticleHelper.onClearParticles();
	}
}
