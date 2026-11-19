package com.github.justinwon777.humancompanions.entity.ai;

import com.github.justinwon777.humancompanions.entity.AbstractHumanCompanionEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.phys.Vec3;

public class CustomFollowOwnerGoal extends FollowOwnerGoal {

    public AbstractHumanCompanionEntity companion;

    public CustomFollowOwnerGoal(AbstractHumanCompanionEntity p_25294_, double p_25295_, float p_25296_, float p_25297_, boolean p_25298_) {
        super(p_25294_, p_25295_, p_25296_, p_25297_, p_25298_);
        this.companion = p_25294_;
    }

    public boolean canUse() {
        if (!companion.isFollowing()) {
            return false;
        }
        return super.canUse();
    }
    @Override
    public void tick() {
        Vec3 oldPosition = companion.position();
        super.tick();

        if (companion.distanceToSqr(oldPosition) > 0.0) {
            companion.targetSelector.getRunningGoals()
                    .forEach(goal -> goal.stop());
            companion.setTarget(null);
        }
    }
}
