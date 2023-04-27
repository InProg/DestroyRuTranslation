package com.petrolpark.destroy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.petrolpark.destroy.Destroy;
import com.petrolpark.destroy.block.entity.BubbleCapBlockEntity;
import com.petrolpark.destroy.recipe.DestroyRecipeTypes;
import com.petrolpark.destroy.recipe.DistillationRecipe;
import com.simibubi.create.content.contraptions.processing.BasinTileEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.recipe.RecipeFinder;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class DistillationTower {

    private static final Object distillationRecipeKey = new Object();
    private static final int PROCESS_TIME = 100; // How often (in ticks) to attempt processing

    private BlockPos position; // The bottom of the Distillation Tower
    private List<BubbleCapBlockEntity> fractions;
    private DistillationRecipe lastRecipe;
    private int tick;

    public DistillationTower(Level level, BlockPos controllerPos) { // Create a new Distillation Tower from scratch
        position = controllerPos;
        fractions = new ArrayList<>();
        tick = PROCESS_TIME;
        int i = 0;
        while (true) {
            BlockEntity be = level.getBlockEntity(controllerPos.above(i));
            if (be == null || !(be instanceof BubbleCapBlockEntity bubbleCap)) {
                break;
            } else {
                addBubbleCap(bubbleCap);
            };
            i++;
        };
        if (getHeight() == 0) {
            throw new IllegalStateException("Could not initialize Distillation Tower at "+controllerPos.toShortString());
        };
    };

    public DistillationTower(CompoundTag compound, Level level, BlockPos pos) { // Create a new Distillation Tower from NBT
        position = pos;
        tick = compound.getInt("Tick");
        int height = compound.getInt("Height");
        fractions = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            BlockEntity be = level.getBlockEntity(position.above(i));
            if (be == null || !(be instanceof BubbleCapBlockEntity bubbleCap)) {
                Destroy.LOGGER.warn("Could not load Distillation Tower starting at "+pos+". New height is "+(i+1));
                break;
            } else {
                addBubbleCap(bubbleCap);
            };
        };
    };

    public BlockPos getControllerPos() {
        return position;
    };
    
    public int getHeight() {
        return fractions.size();
    };

    public BubbleCapBlockEntity getControllerBubbleCap() {
        return getHeight() == 0 ? null : fractions.get(0);
    };

    /**
     * Adds a Bubble Cap to the top of the Distillation Tower, and updates the information that Bubble Cap has.
     * The Bubble Cap will only be added if it is not already in this Distillation Tower.
     */
    public void addBubbleCap(BubbleCapBlockEntity bubbleCap) {
        if (fractions.contains(bubbleCap)) return;
        bubbleCap.addToDistillationTower(this);
        fractions.add(bubbleCap);
        getControllerBubbleCap().sendData(); // Let the controller know it's changed
    };

    /**
     * Removes this Bubble Cap and all Bubble Caps above it from the Distillation Tower.
     * @param bubbleCapToRemove If not present in this Distillation Tower, nothing happens
     */
    public void removeBubbleCap(BubbleCapBlockEntity bubbleCapToRemove) {
        List<BubbleCapBlockEntity> newFractions = new ArrayList<>();
        for (BubbleCapBlockEntity bubbleCap : fractions) {
            if (bubbleCap == bubbleCapToRemove) {
                break;
            };
            newFractions.add(bubbleCap);
        };
        fractions = newFractions;
    };

    public void tick(Level level) {
        tick--;
        if (tick <= 0) {
            findRecipe(level);
            process();
            tick = PROCESS_TIME; // Reset counter
        };
    };

    public void findRecipe(Level level) {
        if (getControllerBubbleCap() == null) return;
        SmartFluidTank inputTank = getControllerBubbleCap().getTank();
        if (lastRecipe == null || !lastRecipe.getRequiredFluid().test(inputTank.getFluid())) { // If the Recipe has changed
            List<Recipe<?>> possibleRecipes = RecipeFinder.get(distillationRecipeKey, level, r -> r.getType() == DestroyRecipeTypes.DISTILLATION.getType()).stream().filter(r -> {
                DistillationRecipe recipe = (DistillationRecipe) r;
                if (!recipe.getRequiredFluid().test(inputTank.getFluid())) return false; // If there is insufficient input Fluid
                return true;
            }).collect(Collectors.toList());
            if (possibleRecipes.size() >= 1) {
                lastRecipe = (DistillationRecipe)possibleRecipes.get(0);
            } else {
                lastRecipe = null;
            };
        };
    };

    /**
     * Applies the current Recipe.
     * @return Whether the Recipe was successfully processed
     */
    public boolean process() {
        if (lastRecipe == null) return false;
        if (lastRecipe.getFractions() > getHeight() - 1) return false;
        BubbleCapBlockEntity controller = getControllerBubbleCap();
        if (controller == null) return false;
        Level level = controller.getLevel();
        if (level == null) return false;

        FluidStack fluidDrained = FluidStack.EMPTY;
        for (boolean simulate : Iterate.trueAndFalse) { // First simulate to check if all the Fluids can actually fit, then execute if they do. 

            // Check if heat requirement is fulfilled
            if (!lastRecipe.getRequiredHeat().testBlazeBurner(BasinTileEntity.getHeatLevelOf(level.getBlockState(getControllerPos().below())))) return false;

            // Check if required Fluid is present
            int requiredFluidAmount = lastRecipe.getRequiredFluid().getRequiredAmount();
            fluidDrained = getControllerBubbleCap().getTank().drain(requiredFluidAmount, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
            if (fluidDrained.getAmount() < requiredFluidAmount) { // If there is not enough Fluid in the controller Bubble Cap
                return false;
            };

            // Check if resultant Fluids can fit
            for (int i = 0; i < lastRecipe.getFractions(); i++) {
                FluidStack distillate = lastRecipe.getFluidResults().get(i);
                BubbleCapBlockEntity bubbleCap = fractions.get(i + 1);
                if (bubbleCap.getInternalTank().fill(distillate, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE) < distillate.getAmount()) { // If not all the Fluid can be added to this Bubble Cap
                    return false; // TODO fix as this isn't being checked properly
                };
                if (!simulate) {
                    bubbleCap.setTicksToFill(i * BubbleCapBlockEntity.getTankCapacity() / BubbleCapBlockEntity.getTransferRate());
                };
            };

        };
        // If we've got to this point, the Recipe is being successfully processed
        controller.getTank().drain(lastRecipe.getRequiredFluid().getRequiredAmount(), FluidAction.EXECUTE);
        controller.particleFluid = fluidDrained.copy();
        controller.onDistill();
        return true;
    };


    public CompoundTag serializeNBT() {
        CompoundTag compound = new CompoundTag();
        compound.putInt("Height", getHeight());
        compound.putInt("Tick", tick);
        return compound;
    };

};