package mdtnh.hatch;

import mindustry.gen.Building;
import mindustry.type.Item;

/**
 * 物品输入仓 —— 用于向多方块结构提供原料。
 * 接受物品但不会自动输出，防止原料被传送带抽走。
 */
public class ItemInputHatch extends Hatch {
    public ItemInputHatch(String name) {
        super(name);
        buildType = ItemInputHatchBuild::new;
        // 可在此覆盖贴图等
    }

    public class ItemInputHatchBuild extends HatchBuild {
        @Override
        public boolean acceptItem(Building source, Item item) {
            // 只要仓内未满就接受（不限制物品类型，由核心配方决定）
            return items.get(item) < block.itemCapacity;
        }

        // 不重写 updateTile，因此不会自动 dump
    }
}