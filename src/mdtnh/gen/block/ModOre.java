package mdtnh.gen.block;

import mindustry.type.Item;
import mindustry.world.blocks.environment.OreBlock;

public class ModOre {
    public static OreBlock testOre;
    public static Item testOreItem;
    public static void load(){
        testOreItem=new Item("test-ore"){{
            hardness=1;
        }};
        testOre=new OreBlock(testOreItem){{
            variants = 1;
        }};
    }
}
