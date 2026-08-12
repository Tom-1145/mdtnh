package mdtnh;

import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatUnit;

public class ModStat{
    public static final StatCat
            GTPipeProp=new StatCat("GTPipeProp"),
            GTFluidProp = new StatCat("GTFluidProp");
    public static final Stat
            GTPipeFluidCapacity = new Stat("GTPipeFluidCapacity",GTPipeProp),
            GTPipeIngredient = new Stat("GTPipeIngredient",GTPipeProp),
            GTPipeHeatLimit = new Stat("GTPipeHeatLimit",GTPipeProp),
            GTFluidHeat = new Stat("GTFluidHeat",GTFluidProp),
            GTFluidState = new Stat("GTFluidState",GTFluidProp);
    public static final StatUnit
            GTL = new StatUnit("GTL"),
            GTK = new StatUnit("GTK");
}
