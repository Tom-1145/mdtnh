package mdtnh.hatch;

import mindustry.gen.Building;
import mindustry.type.Liquid;

/**
 * 液体输入仓 —— 用于向多方块结构提供原料液体。
 *
 * <p>接受外部管道输入的液体但不会主动把内容输出到相邻方块，
 * 防止原料液体被导管抽走。多方块核心会根据当前配方从液体输入仓中取料。</p>
 */
public class LiquidInputHatch extends Hatch {

    public LiquidInputHatch(String name) {
        super(name);
        // 液体仓只保存液体，不启用 Hatch 基类提供的物品模块。
        hasItems = false;
        itemCapacity = 0;
        hasLiquids = true;
        liquidCapacity = 30f;
        buildType = LiquidInputHatchBuild::new;
    }

    public class LiquidInputHatchBuild extends HatchBuild {
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            // 只要仓内未满就接受（不限制液体类型，由核心配方决定）。
            return liquids.get(liquid) < block.liquidCapacity;
        }

        // 不重写 updateTile，因此不会自动向相邻管道 dump。
    }
}
