package mdtnh.fluidSystem.GT;

import arc.graphics.Color;
import mdtnh.ModStat;
import mindustry.type.Liquid;

public class GTFluid extends Liquid {
    public int heat;
    public GTFluid(String name,int heat, Color color){
        super(name,color);
        this.heat = heat;
    }
    public GTFluid(String name, Color color){
        this(name,273,color);
    }
    public GTFluid(String name,int heat){
        this(name,heat,new Color(0));
    }
    public GTFluid(String name){
        this(name,new Color(0));
    }

    @Override
    public void setStats() {
        stats.add(ModStat.GTFluidHeat,heat,ModStat.GTK);
        stats.add(ModStat.GTFluidState,gas?"Gas":"Liquid");
    }

    public static GTFluid water;
    public static void ModLoad(){
        water = new GTFluid("water",300);
    }
}
