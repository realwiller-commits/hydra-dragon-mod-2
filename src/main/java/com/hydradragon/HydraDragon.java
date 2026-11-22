package com.hydradragon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Three-headed boss dragon.
 *  - Heads 0 and 2 (left / right) spit ghast-style fireballs.
 *  - Head 1 (middle) spits poison balls that burst into a poison cloud.
 *  - Every so often it launches a nuclear strike on a player's position.
 */
public class HydraDragon extends Mob {
    private static final double ORBIT_RADIUS = 22.0;
    private static final double ORBIT_HEIGHT = 11.0;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.hydradragon.hydra_dragon"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private final List<PoisonBall> poisonBalls = new ArrayList<>();
    private double orbitAngle = 0.0;
    private int attackTimer = 60;
    private int nextHead = 0;
    private int nukeCooldown = 600;
    private int nukeCountdown = -1;
    private Vec3 nukeTarget = Vec3.ZERO;

    public HydraDragon(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createDragonAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.SCALE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 100.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    // ------------------------------------------------------------------
    // Movement: fly in circles above the nearest player
    // ------------------------------------------------------------------

    @Override
    public void travel(Vec3 input) {
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    private void flyAround(ServerPlayer target) {
        this.orbitAngle += 0.035;
        Vec3 desired = new Vec3(
                target.getX() + Math.cos(this.orbitAngle) * ORBIT_RADIUS,
                target.getY() + ORBIT_HEIGHT,
                target.getZ() + Math.sin(this.orbitAngle) * ORBIT_RADIUS);
        Vec3 toDesired = desired.subtract(this.position());
        double dist = toDesired.length();
        Vec3 wanted = dist > 0.01 ? toDesired.scale(1.0 / dist).scale(Math.min(0.9, dist * 0.1)) : Vec3.ZERO;
        Vec3 motion = this.getDeltaMovement().scale(0.85).add(wanted.scale(0.15));
        if (this.horizontalCollision) {
            motion = motion.add(0, 0.4, 0);
        }
        this.setDeltaMovement(motion);
    }

    private void faceTarget(ServerPlayer target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(-dx, dz) * (180.0 / Math.PI));
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    // ------------------------------------------------------------------
    // Brain
    // ------------------------------------------------------------------

    private ServerPlayer findTarget(ServerLevel level) {
        ServerPlayer best = null;
        double bestDist = 100.0 * 100.0;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator()) {
                continue;
            }
            double d = p.distanceToSqr(this);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.tickPoisonBalls(level);

        ServerPlayer target = this.findTarget(level);
        this.setTarget(target);
        if (target == null) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            return;
        }

        this.flyAround(target);
        this.faceTarget(target);

        float health = this.getHealth() / this.getMaxHealth();

        if (this.nukeCountdown >= 0) {
            this.tickNuke(level);
            return;
        }

        if (--this.attackTimer <= 0) {
            this.fireFromHead(level, target, this.nextHead);
            this.nextHead = (this.nextHead + 1) % 3;
            this.attackTimer = health > 0.66f ? 30 : (health > 0.33f ? 22 : 15);
        }

        if (--this.nukeCooldown <= 0) {
            this.startNuke(level, target);
        }
    }

    // ------------------------------------------------------------------
    // Heads and projectiles
    // ------------------------------------------------------------------

    /** Rough world position of a mouth. Head 0 = left, 1 = middle, 2 = right. */
    private Vec3 headPosition(int head) {
        float s = this.getScale();
        double yaw = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        double spread = head == 1 ? 0.0 : (head == 0 ? 1.0 : -1.0);
        double reach = 1.5 * Math.cos(0.35);
        double fwd = s * (1.0 + reach * Math.cos(0.5 * Math.abs(spread)));
        double side = s * (0.31 * spread + reach * Math.sin(0.5) * spread);
        double up = s * (1.0 + 1.5 * Math.sin(0.35));
        return this.position().add(forward.scale(fwd)).add(left.scale(side)).add(0, up, 0);
    }

    private void fireFromHead(ServerLevel level, ServerPlayer target, int head) {
        Vec3 origin = this.headPosition(head);
        Vec3 aim = target.getEyePosition().subtract(origin).normalize();

        if (head == 1) {
            this.poisonBalls.add(new PoisonBall(origin, aim.scale(0.9)));
            this.playSound(SoundEvents.GHAST_SHOOT, 4.0f, 0.6f);
        } else {
            LargeFireball fireball = new LargeFireball(level, this, aim, 3);
            fireball.setPos(origin.x, origin.y, origin.z);
            level.addFreshEntity(fireball);
            this.playSound(SoundEvents.GHAST_SHOOT, 4.0f, 1.0f);
        }
    }

    private void tickPoisonBalls(ServerLevel level) {
        Iterator<PoisonBall> it = this.poisonBalls.iterator();
        while (it.hasNext()) {
            PoisonBall ball = it.next();
            ball.age++;
            ball.pos = ball.pos.add(ball.vel);

            level.sendParticles(ParticleTypes.SNEEZE, ball.pos.x, ball.pos.y, ball.pos.z, 6, 0.3, 0.3, 0.3, 0.01);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ball.pos.x, ball.pos.y, ball.pos.z, 4, 0.4, 0.4, 0.4, 0.0);

            boolean hit = ball.age > 200 || !level.getBlockState(BlockPos.containing(ball.pos)).isAir();
            if (!hit) {
                for (ServerPlayer p : level.players()) {
                    if (p.isAlive() && !p.isSpectator()
                            && (p.position().distanceToSqr(ball.pos) < 6.0 || p.getEyePosition().distanceToSqr(ball.pos) < 6.0)) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) {
                this.burstPoison(level, ball.pos);
                it.remove();
            }
        }
    }

    private void burstPoison(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SNEEZE, pos.x, pos.y, pos.z, 120, 2.0, 1.0, 2.0, 0.05);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 40, 2.0, 1.0, 2.0, 0.1);
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator() && p.position().distanceToSqr(pos) < 36.0) {
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 240, 1));
            }
        }
    }

    // ------------------------------------------------------------------
    // Nuclear strike
    // ------------------------------------------------------------------

    private void startNuke(ServerLevel level, ServerPlayer target) {
        this.nukeCountdown = 120;
        this.nukeTarget = target.position();
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 10.0f, 0.6f);
        this.broadcast(level, "NUCLEAR STRIKE INCOMING - RUN!");
    }

    private void tickNuke(ServerLevel level) {
        this.nukeCountdown--;

        if (this.nukeCountdown % 20 == 0 && this.nukeCountdown > 0) {
            this.broadcast(level, "NUKE IN " + (this.nukeCountdown / 20) + "...");
        }

        if (this.nukeCountdown % 2 == 0) {
            for (int k = 0; k < 40; k++) {
                double a = (Math.PI * 2.0) * k / 40.0;
                level.sendParticles(ParticleTypes.FLAME,
                        this.nukeTarget.x + Math.cos(a) * 14.0, this.nukeTarget.y + 0.3, this.nukeTarget.z + Math.sin(a) * 14.0,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
            for (int y = 0; y < 60; y += 2) {
                level.sendParticles(ParticleTypes.END_ROD,
                        this.nukeTarget.x, this.nukeTarget.y + y, this.nukeTarget.z,
                        1, 0.2, 0.0, 0.2, 0.0);
            }
        }

        if (this.nukeCountdown <= 0) {
            level.explode(this, this.nukeTarget.x, this.nukeTarget.y, this.nukeTarget.z, 14.0f, Level.ExplosionInteraction.MOB);
            this.nukeCountdown = -1;
            this.nukeCooldown = (this.getHealth() / this.getMaxHealth()) < 0.33f ? 400 : 700;
        }
    }

    private void broadcast(ServerLevel level, String message) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 150.0 * 150.0) {
                p.displayClientMessage(Component.literal(message), true);
            }
        }
    }

    // ------------------------------------------------------------------
    // Boss bar, sounds, despawn
    // ------------------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    private static final class PoisonBall {
        Vec3 pos;
        final Vec3 vel;
        int age;

        PoisonBall(Vec3 pos, Vec3 vel) {
            this.pos = pos;
            this.vel = vel;
        }
    }
}
