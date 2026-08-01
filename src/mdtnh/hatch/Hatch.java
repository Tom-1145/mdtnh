package mdtnh.hatch;

import arc.Core;
import arc.graphics.g2d.*;
import mindustry.content.Items;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * 舱室基类 —— 一个拥有物品存储的简单方块。
 * 默认大小为 1x1，成本低廉，无特殊能力。
 */
public class Hatch extends Block {
    public TextureRegion region;

    public Hatch(String name) {
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        itemCapacity = 10;          // 默认容量，可在 JSON 中覆盖
        buildType = HatchBuild::new;
        size = 1;
        health = 80;
        requirements(Category.distribution, ItemStack.with(Items.copper, 30, Items.lead, 15));
    }

    @Override
    public void load() {
        super.load();
        region = Core.atlas.find(name);
    }

    public class HatchBuild extends Building {
        @Override
        public void draw() {
            Draw.rect(region, x, y);
        }

        // 默认行为：不主动输出（由子类决定）
        @Override
        public void updateTile() {
            super.updateTile();
            // 什么都不做 —— 输入舱室保持此行为
        }
    }
}