package com.github.justinwon777.humancompanions.entity.ai;

import com.github.justinwon777.humancompanions.entity.AbstractHumanCompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class PickupItemGoal extends Goal {
    private static final double SEARCH_RADIUS = 8.0;
    private static final int SEARCH_INTERVAL = 50;
    private static final int PATH_UPDATE_INTERVAL = 10;

    private final AbstractHumanCompanionEntity companion;
    private final double speed;
    private ItemEntity targetItem;
    private Path path;
    private long nextSearchTime;
    private int pathUpdateTicks;

    public PickupItemGoal(AbstractHumanCompanionEntity companion, double speed) {
        this.companion = companion;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        long gameTime = this.companion.level().getGameTime();
        if (gameTime < this.nextSearchTime || !this.canMoveToItems()) {
            return false;
        }
        this.nextSearchTime = gameTime + SEARCH_INTERVAL;

        List<ItemEntity> items = this.companion.level().getEntitiesOfClass(
                ItemEntity.class,
                this.companion.getBoundingBox().inflate(SEARCH_RADIUS, 3.0, SEARCH_RADIUS),
                this::canPickUp
        );
        items.sort(Comparator.comparingDouble(item -> this.companion.distanceToSqr(item)));

        for (ItemEntity item : items) {
            Path candidatePath = this.companion.getNavigation().createPath(item, 0);
            if (candidatePath != null && candidatePath.canReach()) {
                this.targetItem = item;
                this.path = candidatePath;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.pathUpdateTicks = this.adjustedTickDelay(PATH_UPDATE_INTERVAL);
        this.companion.getNavigation().moveTo(this.path, this.speed);
    }

    @Override
    public boolean canContinueToUse() {
        return this.canMoveToItems()
                && this.targetItem != null
                && this.canPickUp(this.targetItem)
                && !this.companion.getNavigation().isDone();
    }

    @Override
    public void tick() {
        if (--this.pathUpdateTicks <= 0) {
            this.pathUpdateTicks = this.adjustedTickDelay(PATH_UPDATE_INTERVAL);
            this.companion.getNavigation().moveTo(this.targetItem, this.speed);
        }
    }

    @Override
    public void stop() {
        this.companion.getNavigation().stop();
        this.targetItem = null;
        this.path = null;
        this.nextSearchTime = this.companion.level().getGameTime() + SEARCH_INTERVAL;
    }

    private boolean canMoveToItems() {
        return this.companion.isFollowing()
                && !this.companion.isOrderedToSit()
                && this.companion.getTarget() == null
                && this.companion.canPickUpLoot()
                && ForgeEventFactory.getMobGriefingEvent(this.companion.level(), this.companion);
    }

    private boolean canPickUp(ItemEntity item) {
        return item.isAlive()
                && !item.hasPickUpDelay()
                && this.companion.distanceToSqr(item) <= SEARCH_RADIUS * SEARCH_RADIUS
                && this.companion.wantsToPickUp(item.getItem());
    }
}
