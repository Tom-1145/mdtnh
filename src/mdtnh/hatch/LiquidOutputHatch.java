package mdtnh.hatch;

import mindustry.gen.Building;
import mindustry.type.Liquid;

/**
 * 液体输出仓 —— 接收多方块核心产出的液体，并自动输出到相邻管道/容器。
 */
public class LiquidOutputHatch extends Hatch {

    public LiquidOutputHatch(String name) {
        super(name);
        // 液体仓只保存液体，不启用 Hatch 基类提供的物品模块。
        hasItems = false;
        itemCapacity = 0;
        hasLiquids = true;
        liquidCapacity = 30f;
        buildType = LiquidOutputHatchBuild::new;
    }

    public class LiquidOutputHatchBuild extends HatchBuild {

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquids.get(liquid) < block.liquidCapacity;
        }

        @Override
        public void updateTile() {
            super.updateTile();
            // 持续尝试把仓内所有液体输出到相邻管道/容器。
            liquids.each((liquid, amount) -> {
                if (amount > 0.001f) dumpLiquid(liquid);
            });
        }
    }
}
