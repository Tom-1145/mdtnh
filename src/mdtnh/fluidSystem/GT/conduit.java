package mdtnh.fluidSystem.GT;

import arc.Core;
import mdtnh.ModItems;
import mdtnh.ModStat;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;

import java.awt.*;

public class conduit extends Block {
    public int GTFluidCapacity,GTHeatLimit;
    public String ingredient;
    public conduit(String name) {
        super(name);
    }
    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.size, "@x@", size, size);
        if(synthetic()){
            stats.add(Stat.health, health, StatUnit.none);
            if(armor > 0){
                stats.add(Stat.armor, armor, StatUnit.none);
            }
        }
        if(canBeBuilt() && requirements.length > 0){
            stats.add(Stat.buildTime, buildTime / 60, StatUnit.seconds);
            stats.add(Stat.buildCost, StatValues.items(false, requirements));
        }
        stats.add(ModStat.GTPipeFluidCapacity,GTFluidCapacity,ModStat.GTL);
        stats.add(ModStat.GTPipeHeatLimit,GTHeatLimit,ModStat.GTK);
        stats.add(ModStat.GTPipeIngredient,table->{
            table.add(StatValues.displayItem(ModItems.get(ingredient,"ingot")));
        });
    }
    @Override
    public void setBars(){
        super.setBars();
        addBar("Fluid",(conduitBuilding build)->new Bar(
                ()->build.fluid.fluid==null?Core.bundle.get("bar.liquid"):build.fluid.fluid.localizedName,
                ()->build.fluid.fluid==null? Pal.gray:build.fluid.fluid.color,
                ()->build.fluid.amount * 1F / GTFluidCapacity
        ));
    }
    public class conduitBuilding extends Building {
        GTFluidStack fluid;
    }
}
