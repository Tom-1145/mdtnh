package mdtnh;

import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.consumers.ConsumeItemFlammable;
import mindustry.world.draw.DrawDefault;

public class ModCrafters {
    public static GenericCrafter Small_Coal_Fired_Boiler;

    public static void load(){
        Small_Coal_Fired_Boiler=new GenericCrafter("small-coal-fired-boiler"){{
            health = 100;
            size = 2;
            //使用此重载，Anuke会贴心地帮你设置成BuildVisibility.shown
            requirements(Category.crafting, ItemStack.with(Items.copper,50));
        }};
        Small_Coal_Fired_Boiler.outputLiquid=new LiquidStack(ModLiquids.steam, 1);
        Small_Coal_Fired_Boiler.craftTime=60;
        Small_Coal_Fired_Boiler.drawer=new DrawDefault();
        Small_Coal_Fired_Boiler.consume(new ConsumeItemFlammable());;
        Small_Coal_Fired_Boiler.consumeLiquid(Liquids.water,1);

    }

}
