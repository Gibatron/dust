package dev.mrturtle.entity;

import dev.mrturtle.Dust;
import dev.mrturtle.item.DustBunnyModelItem;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.EnumSet;
import java.util.List;

public class DustBunnyEntity extends HostileEntity implements PolymerEntity {
    private final ElementHolder holder;
    private final ItemDisplayElement eyesElement;
    private final ItemDisplayElement[] blades = new ItemDisplayElement[3];
    private float bladeRotation = 0;

    public DustBunnyEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        experiencePoints = SMALL_MONSTER_XP;
        holder = new ElementHolder();
        eyesElement = new ItemDisplayElement();
        eyesElement.setItem(DustBunnyModelItem.getStackForPart(DustBunnyModelItem.ModelPart.EYES));
        eyesElement.setBrightness(Brightness.FULL);
        eyesElement.setInterpolationDuration(3);
        eyesElement.setTeleportDuration(1);
        eyesElement.setOffset(new Vec3d(0, 0.5f, 0));
        eyesElement.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        holder.addElement(eyesElement);
        for (int i = 0; i < 3; i++) {
            ItemDisplayElement bladeElement = new ItemDisplayElement();
            bladeElement.setItem(DustBunnyModelItem.getStackForPart(DustBunnyModelItem.ModelPart.BLADE));
            bladeElement.setBrightness(Brightness.FULL);
            bladeElement.setInterpolationDuration(3);
            bladeElement.setTeleportDuration(1);
            holder.addElement(bladeElement);
            blades[i] = bladeElement;
        }
        InteractionElement interactionElement = InteractionElement.redirect(this);
        interactionElement.setSize(0.75f, 0.75f);
        holder.addElement(interactionElement);
        EntityAttachment.ofTicking(holder, this);
    }

    @Override
    public void tick() {
        super.tick();
        bladeRotation += 15f;
        float radianRotation = bladeRotation * MathHelper.RADIANS_PER_DEGREE;
        blades[0].setTransformation(new Matrix4f().translate(0.1f, 0.6f, 0).rotateX(-radianRotation).rotateY(radianRotation));
        blades[1].setTransformation(new Matrix4f().translate(0, 0.5f, 0).rotateX(radianRotation).rotateZ(-radianRotation));
        blades[2].setTransformation(new Matrix4f().translate(0, 0.4f, -0.1f).rotateZ(-radianRotation).rotateY(radianRotation));
        eyesElement.startInterpolation();
        for (ItemDisplayElement blade : blades) {
            blade.startInterpolation();
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isIn(ItemTags.CREEPER_IGNITERS)) {
            SoundEvent soundEvent = itemStack.isOf(Items.FIRE_CHARGE) ? SoundEvents.ITEM_FIRECHARGE_USE : SoundEvents.ITEM_FLINTANDSTEEL_USE;
            playSound(soundEvent);
            if (!getWorld().isClient) {
                kill();
                ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5f, getZ(), 10, 0.1, 0.1, 0.1, 0.06);
                playSound(SoundEvents.ENTITY_BLAZE_SHOOT);
                if (!itemStack.isDamageable()) {
                    itemStack.decrement(1);
                } else {
                    itemStack.damage(1, player, getSlotForHand(hand));
                }
            }
            return ActionResult.success(getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (getWorld().isClient)
            return;
        ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.SPIT, getX(), getY() + 0.5f, getZ(), 30, 0.1, 0.1, 0.1, 0.12);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!getWorld().isClient) {
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.SPIT, getX(), getY() + 0.5f, getZ(), 30, 0.1, 0.1, 0.1, 0.12);
            if (source.isIn(DamageTypeTags.IS_FIRE)) {
                kill();
            }
        }
        return super.damage(source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ALLAY_DEATH;
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new ChargeTargetGoal());
        goalSelector.add(2, new PursueTargetGoal(this));
        goalSelector.add(3, new WanderAroundFarGoal(this, 0.5));
        targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    public static DefaultAttributeContainer.Builder createDustBunnyAttributes() {
        return createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0);
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (!canTarget(player))
            return;
        player.damage(getDamageSources().mobAttack(this), 5.0f);
        Vec3d launchVelocity = getPos().subtract(player.getPos()).normalize();
        launchVelocity = launchVelocity.add(0, 0.2, 0);
        launchVelocity = launchVelocity.multiply(0.5);
        setVelocity(launchVelocity);
        velocityModified = true;
        playSound(SoundEvents.ENTITY_PUFFER_FISH_DEATH);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        if (target instanceof ServerPlayerEntity serverPlayerEntity)
            if ((Util.getMeasuringTimeMs() - serverPlayerEntity.getLastActionTime()) > Dust.TIME_FOR_AFK)
                return false;
        return super.canTarget(target);
    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.ARMOR_STAND;
    }

    @Override
    public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
        data.add(DataTracker.SerializedEntry.of(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX)));
        data.add(DataTracker.SerializedEntry.of(EntityTrackedData.NO_GRAVITY, true));
        data.add(DataTracker.SerializedEntry.of(ArmorStandEntity.ARMOR_STAND_FLAGS, (byte) (ArmorStandEntity.SMALL_FLAG | ArmorStandEntity.MARKER_FLAG)));
    }

    class ChargeTargetGoal extends Goal {
        private static final int JUMP_DELAY_TICKS = 2;
        private static final int LAUNCH_DELAY_TICKS = 20;

        private int jumpCount = 0;
        private int launchDelayTicks = 10;
        private int ticksInAir = 0;

        public ChargeTargetGoal() {
            setControls(EnumSet.of(Goal.Control.MOVE, Control.JUMP));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && (canSee(target) || jumpCount > 0);
        }

        @Override
        public void tick() {
            if (isOnGround()) {
                ticksInAir = 0;
                if (launchDelayTicks <= 0) {
                    jumpCount += 1;
                    launchDelayTicks = JUMP_DELAY_TICKS;
                    jump();
                    playSound(SoundEvents.ENTITY_BREEZE_DEATH);
                } else {
                    launchDelayTicks -= 1;
                }
            } else {
                ticksInAir += 1;
                if (jumpCount >= 2 && ticksInAir >= 4) {
                    LivingEntity target = getTarget();
                    double distanceToTarget = target.getPos().distanceTo(getPos());
                    distanceToTarget = Math.min(distanceToTarget, 10);
                    Vec3d launchVelocity = target.getPos().subtract(getPos()).normalize();
                    launchVelocity = launchVelocity.add(0, 0.2, 0);
                    launchVelocity = launchVelocity.multiply(distanceToTarget / 6.0);
                    setVelocity(launchVelocity);
                    velocityModified = true;
                    jumpCount = 0;
                    launchDelayTicks = LAUNCH_DELAY_TICKS;
                    playSound(SoundEvents.ENTITY_BREEZE_SHOOT);
                }
            }
        }
    }

    class PursueTargetGoal extends Goal {
        private final DustBunnyEntity entity;

        public PursueTargetGoal(DustBunnyEntity entity) {
            this.entity = entity;
            setControls(EnumSet.of(Goal.Control.MOVE, Control.JUMP));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && !canSee(target);
        }

        @Override
        public void tick() {
            if (!isNavigating()) {
                Vec3d targetPos = getTarget().getBlockPos().toBottomCenterPos();
                Vec3d navPos = NoPenaltyTargeting.findTo(entity, 15, 4, targetPos, Math.PI / 2.0);
                if (navPos != null) {
                    getNavigation().startMovingTo(navPos.x, navPos.y, navPos.z, 0.35);
                }
            }
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }
    }
}
