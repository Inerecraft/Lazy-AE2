package io.github.phantamanta44.threng.tile.base;

import io.github.phantamanta44.libnine.LibNine;
import io.github.phantamanta44.libnine.capability.impl.L9AspectSlot;
import io.github.phantamanta44.libnine.recipe.IRcp;
import io.github.phantamanta44.libnine.recipe.input.IRcpIn;
import io.github.phantamanta44.libnine.recipe.output.IRcpOut;
import io.github.phantamanta44.libnine.util.TriBool;
import io.github.phantamanta44.libnine.util.data.serialization.AutoSerialize;
import io.github.phantamanta44.threng.util.AppEngUtils;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileSimpleProcessor<IT, OT, I extends IRcpIn<IT>, O extends IRcpOut<OT>, R extends IRcp<IT, I, O>>
        extends TileMachine {

    private final Class<R> recipeType;

    @AutoSerialize
    private final L9AspectSlot slotUpgrade = new TileSimpleProcessor.UpgradeSlot(this);

    private TriBool canWork = TriBool.NONE;
    @Nullable
    private R activeRecipe = null;
    @Nullable
    private O activeRecipeOutput = null;
    private int maxWork = -1;
    private int energyCost = -1;

    public TileSimpleProcessor(Class<R> recipeType, int energyBuffer) {
        super(energyBuffer);
        this.recipeType = recipeType;
    }

    @Override
    protected boolean canWork() {
        if (canWork == TriBool.NONE) {
            IT input = getInput();
            activeRecipe = LibNine.PROXY.getRecipeManager().getRecipeList(recipeType).findRecipe(input);
            if (activeRecipe != null) {
                activeRecipeOutput = activeRecipe.mapToOutput(input);
                canWork = TriBool.wrap(activeRecipeOutput.isAcceptable(getOutputEnvironment()));
            } else {
                canWork = TriBool.FALSE;
            }
        }
        return canWork.value;
    }

    @Override
    protected int getDeltaWork() {
        return 1;
    }

    @Override
    protected int getMaxWork() {
        if (maxWork == -1) {
            maxWork = 150 - 16 * getUpgradeCount();
        }
        return maxWork;
    }

    @Override
    protected int getEnergyCost() {
        if (energyCost == -1) {
            energyCost = (int)Math.ceil(Math.pow(1.61616, getUpgradeCount())
                    * getBaseEnergyPerOperation() / getMaxWork());
        }
        return energyCost;
    }

    private int getUpgradeCount() {
        ItemStack stack = slotUpgrade.getStackInSlot();
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    @Override
    protected void onWorkFinished() {
        acceptOutput(
                Objects.requireNonNull(activeRecipe).input().consume(getInput()),
                Objects.requireNonNull(activeRecipeOutput));
    }

    protected void markWorkStateDirty() {
        canWork = TriBool.NONE;
    }

    protected int getBaseEnergyPerOperation() {
        return 8100;
    }

    protected abstract IT getInput();

    protected abstract OT getOutputEnvironment();

    protected abstract void acceptOutput(IT newInputs, O output);

    public L9AspectSlot getUpgradeSlot() {
        return slotUpgrade;
    }

    private static class UpgradeSlot extends L9AspectSlot.Observable {

        UpgradeSlot(TileSimpleProcessor<?, ?, ?, ?, ?> tile) {
            super(AppEngUtils.IS_UPGRADE_ACCEL, (s, o, n) -> tile.maxWork = tile.energyCost = -1);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 8;
        }

    }

}