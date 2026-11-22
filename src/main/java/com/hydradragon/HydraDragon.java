package com.hydradragon;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Stage 1: a giant flying boss built on the Ghast, with three "heads" that take turns
 * firing fireballs from left / middle / right, plus a boss bar.
 */
public class HydraDragon extends Ghast {
private final ServerBossEvent bossEvent = new ServerBossEvent(
        UUID.randomUUID(),
        Component.translatable("entity.hydradragon.hydra_dragon"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private int nextHead = 0;

    public HydraDragon(EntityType<? extends Ghast> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createDragonAttributes() {
        return Ghast.createAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.SCALE, 4.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Remove the normal ghast's single-fireball goal so only our three heads shoot.
        this.goalSelector.removeAllGoals(goal -> goal.getClass().getSimpleName().contains("ShootFireball"));
        this.goalSelector.addGoal(7, new ThreeHeadFireGoal(this));
    }

    /** One head fires per call. 0 = left, 1 = middle, 2 = right. */
    private void fireFromHead(LivingEntity target, int head) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 toTarget = target.getEyePosition().subtract(this.position());
        Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z).normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        double sideOffset = (head - 1) * 3.0;

        Vec3 origin = this.position()
                .add(0, this.getBbHeight() * 0.5, 0)
                .add(flat.scale(3.0))
                .add(right.scale(sideOffset));

        Vec3 direction = target.getEyePosition().subtract(origin).normalize();
        LargeFireball fireball = new LargeFireball(serverLevel, this, direction, 2);
        fireball.setPos(origin.x, origin.y, origin.z);
        serverLevel.addFreshEntity(fireball);
    }

    // ---- boss bar ----

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
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    // ---- attack goal ----

    private static class ThreeHeadFireGoal extends Goal {
        private final HydraDragon dragon;
        private int cooldown;

        ThreeHeadFireGoal(HydraDragon dragon) {
            this.dragon = dragon;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = dragon.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            cooldown = 30;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = dragon.getTarget();
            if (target == null) {
                return;
            }
            dragon.getLookControl().setLookAt(target, 10.0f, 10.0f);
            if (--cooldown <= 0) {
                dragon.fireFromHead(target, dragon.nextHead);
                dragon.nextHead = (dragon.nextHead + 1) % 3;
                cooldown = 25;
            }
        }
    }
}
