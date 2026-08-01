package mdtnh.hatch;

import mindustry.gen.Building;
import mindustry.type.Item;

/**
 * 物品输出仓 —— 接收核心产出的物品，并自动将其输出到相邻传送带/容器。
 */
public class ItemOutputHatch extends Hatch {
    public ItemOutputHatch(String name) {
        super(name);
        buildType = ItemOutputHatchBuild::new;
    }

    public class ItemOutputHatchBuild extends HatchBuild {
        @Override
        public boolean acceptItem(Building source, Item item) {
            // 输出仓只接受物品（但理论上只会由核心的 putToOutputs 放入，也可限制）
            return items.get(item) < block.itemCapacity;
        }

        @Override
        public void updateTile() {
            super.updateTile();
            // 持续尝试将仓内所有物品输出到相邻方块
            dump();
        }
    }
}