package com.petrolpark.destroy.util;

import java.util.ArrayList;
import java.util.List;

import com.petrolpark.destroy.Destroy;
import com.petrolpark.destroy.chemistry.Molecule;
import com.petrolpark.destroy.config.DestroyAllConfigs;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import com.simibubi.create.foundation.utility.LangNumberFormat;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class DestroyLang {

    public static LangBuilder builder() {
        return new LangBuilder(Destroy.MOD_ID);
    };

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    };

    public static LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    };

    public static LangBuilder direction(Direction direction) {
        return translate("generic.direction."+Lang.asId(direction.name())+"");
    };

    /**
     * Adds information about a Tank to the given tooltip.
     * @param tankName What should be displayed before the contents of the Tank (e.g. '[tankName]: 1000mB of Water')
     * @param tooltip
     * @param tank
     */
    //TODO redo
    public static void tankContentsTooltip(List<Component> tooltip, LangBuilder tankName, FluidTank tank) {
        if (tank.isEmpty()) return;
        Component indent = Component.literal(IHaveGoggleInformation.spacing);
        Lang.builder()
            .add(indent.plainCopy())
            .add(tankName
                .add(Component.literal(":"))
                .style(ChatFormatting.DARK_GRAY))
            .space()
            .add(Lang.number(tank.getFluidAmount())
                .add(Lang.translate("generic.unit.millibuckets"))
                .style(ChatFormatting.GOLD)
            ).space()
            .add(DestroyLang.translate("tooltip.fluidcontraption.of")
                .style(ChatFormatting.DARK_GRAY)
            ).space()
            .add(Lang.fluidName(tank.getFluid()))
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip);
    };

    /**
     * Returns a progress bar which changes color depending on how full it is.
     * @param value
     * @param maxValue Should not be greater than value
     * @return Pretty text component
     */
    public static MutableComponent barMeterComponent(int value, int maxValue) {
        return barMeterComponent(value, maxValue, maxValue);
    };

    /**
     * Returns a progress bar which changes color depending on how full it is.
     * @param value
     * @param maxValue Should not be greater than value
     * @param totalBars How many total bars should be displayed
     * @return Pretty text component
     */
    public static MutableComponent barMeterComponent(int value, int maxValue, int totalBars) {
        float proportion = (float)value / maxValue;
        ChatFormatting color;
        if (proportion <= 0.25) {
            color = ChatFormatting.DARK_RED;
        } else if (proportion <= 0.5) {
            color = ChatFormatting.GOLD;
        } else {
            color = ChatFormatting.DARK_GREEN;
        };
        int bars = (int) Math.round((proportion * totalBars));
        return Component.empty()
            .append(Component.literal("|".repeat(bars)).withStyle(color))
            .append(Component.literal("|".repeat(totalBars - bars)).withStyle(ChatFormatting.DARK_GRAY));
    };

    /**
     * The required contents of a Mixture to be used as an Ingredient in a Recipe.
     * @param fluidTag The NBT of the Fluid Stack
     */
    public static List<Component> mixtureIngredientTooltip(CompoundTag fluidTag) {
        List<Component> tooltip = new ArrayList<>();
        String moleculeID = fluidTag.getString("IngredientMolecule");
        float concentration = fluidTag.getFloat("IngredientConcentration");

        Molecule molecule = Molecule.getMolecule(moleculeID);
        Component moleculeName = molecule == null ? DestroyLang.translate("tooltip.unknown_molecule").component() : molecule.getName(DestroyAllConfigs.CLIENT.chemistry.iupacNames.get());

        tooltip.add(DestroyLang.translate("tooltip.mixture_ingredient_1").style(ChatFormatting.GRAY)
            .space()
            .add(moleculeName.plainCopy().withStyle(ChatFormatting.WHITE))
            .space()
            .add(DestroyLang.translate("tooltip.mixture_ingredient_2").style(ChatFormatting.GRAY))
            .space()
            .add(Component.literal(Float.toString(concentration) + "M").withStyle(ChatFormatting.WHITE))
            .add(Component.literal(".").withStyle(ChatFormatting.GRAY))
            .component()
        );
        tooltip.add(DestroyLang.translate("tooltip.mixture_ingredient_3").component().withStyle(ChatFormatting.GRAY));
        return tooltip;
    };
}
