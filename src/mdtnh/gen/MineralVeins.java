package mdtnh.gen;

import mindustry.content.Blocks;
import mindustry.world.blocks.environment.OreBlock;

public class MineralVeins {
    public static MineralVein test;
    public static void load(){
        test=new MineralVein("test-mineral-vein"){{
            density=0.8f;
            weight=2;
        }};
        test.ore.add(Blocks.oreCopper);
        test.ore.add(Blocks.oreLead);
        test.oreweight.add(3);
        test.oreweight.add(7);
        test.updateSum();
    }
}
