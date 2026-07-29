package mdtnh;


import arc.Core;
import arc.struct.*;
import arc.util.*;
import mindustry.content.Items;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.Bar;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.DrawDefault;
import mindustry.world.meta.*;

/**
 * 单一工厂，按顺序遍历配方，执行第一个材料充足的配方。
 */
public class RecipeCrafter extends GenericCrafter {
    /** 按优先级排列的配方列表 */
    public Recipe[] recipes = new Recipe[]{};

    public RecipeCrafter(String name) {
        super(name);
        // 默认值，可在 JSON 中覆盖
        update = true;
        solid = true;
        hasItems = true;
        this.buildType = MDTFactoryBuild::new;
        this.drawer = new DrawDefault();         // 必须：指定绘制器
        // 必须：设置为可见（否则菜单中不会出现）
        this.buildVisibility = BuildVisibility.shown;
        this.requirements(Category.crafting, ItemStack.with(Items.copper, 50));
        itemCapacity = 20;
        liquidCapacity = 20f;
    }

    /**
     * 自定义配方类，也可以直接使用 GenericCrafter.Recipe 并添加多个配方处理逻辑。
     * 这里为了清晰独立定义。
     */
    public static class Recipe {
        public ItemStack[] inputItems;
        public LiquidStack[] inputLiquids;
        public ItemStack outputItem;
        public LiquidStack outputLiquid;
        public float craftTime;
        // 可扩展：产出概率、电力消耗等

        public Recipe(ItemStack[] inputItems, LiquidStack[] inputLiquids,
                      ItemStack outputItem, LiquidStack outputLiquid, float craftTime) {
            this.inputItems = inputItems;
            this.inputLiquids = inputLiquids;
            this.outputItem = outputItem;
            this.outputLiquid = outputLiquid;
            this.craftTime = craftTime;
        }

        /** 快速创建仅含物品的配方 */
        public static Recipe items(ItemStack[] in, ItemStack out, float time) {
            return new Recipe(in, LiquidStack.empty, out, null, time);
        }

        /** 快速创建含液体的配方 */
        public static Recipe withLiquid(ItemStack[] in, LiquidStack[] liqIn,
                                        ItemStack out, LiquidStack liqOut, float time) {
            return new Recipe(in, liqIn, out, liqOut, time);
        }
    }

    /**
     * 覆盖方块设置，根据当前配方的最大需求动态显示材料图标（非必须，但让界面更友好）
     */
    @Override
    public void setBars() {
        super.setBars();

        addBar("progress", (MDTFactoryBuild build) -> new Bar(
                // 进度条显示的文本：配方名 + 百分比
                () -> {
                    if (build.currentRecipe >= 0 && build.currentRecipe < recipes.length) {
                        Recipe r = recipes[build.currentRecipe];
                        String itemName = r.outputItem != null ? r.outputItem.item.localizedName : "???";
                        return itemName + " " + (int)(build.progress * 100) + "%";
                    }
                    return Core.bundle.get("bar.no-recipe");
                },
                // 进度条颜色（可自定义，这里用原版制造进度颜色）
                () -> Pal.accent,
                // 进度值（0~1）
                () -> build.progress
        ));
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.output, table -> {
            table.row();
            for (Recipe r : recipes) {
                if (r.outputItem != null) {
                    table.add(r.outputItem.item.emoji() + " " + r.outputItem.item.localizedName);
                }
                if (r.outputLiquid != null) {
                    table.add(r.outputLiquid.liquid.emoji() + " " + r.outputLiquid.liquid.localizedName);
                }
                table.row();
            }
        });
    }

    // 核心构建实体类
    public class MDTFactoryBuild extends GenericCrafterBuild {
        protected int currentRecipe = -1;

        @Override
        public void updateTile() {
            // 若有正在进行的生产且配方仍有效则继续，否则尝试切换配方
            if (currentRecipe >= 0 && currentRecipe < recipes.length) {
                Recipe r = recipes[currentRecipe];
                if (!hasAllMaterials(r) || outputFull(r)) {
                    // 材料不足或输出满，放弃当前配方
                    currentRecipe = -1;
                }
            }

            // 如果没有正在处理的配方，按顺序查找第一个可行的配方
            if (currentRecipe == -1) {
                for (int i = 0; i < recipes.length; i++) {
                    Recipe r = recipes[i];
                    boolean materials = hasAllMaterials(r);
                    boolean full = outputFull(r);
                    if (hasAllMaterials(r) && !outputFull(r)) {
                        currentRecipe = i;
                        progress = 0f; // 切换配方时重置进度
                        break;
                    }
                }
            }

            // 执行原版生产逻辑（但只针对当前配方）
            if (currentRecipe >= 0 && currentRecipe < recipes.length) {
                Recipe active = recipes[currentRecipe];
                // 临时将原版 craftTime 设为当前配方的制作时间，使进度条正常
                craftTime = active.craftTime;
                // 使用父类的生产推进
                if (shouldConsume()) {
                    progress +=(1.0f / craftTime) *  delta() * efficiency;
                }
                // 进度完成则执行生产
                if (progress >= 1f) {
                    craft(active);
                    progress %= 1f;
                }
                // 生产完成后检查是否仍能继续该配方，否则在下一帧切换
                if (!hasAllMaterials(active) || outputFull(active)) {
                    currentRecipe = -1;
                }
            } else {
                // 无可用配方，进度归零
                progress = 0f;
            }

            // 对于每种可能产出的物品，都尝试推出
            for (Recipe r : recipes) {
                if (r.outputItem != null && items.has(r.outputItem.item)) {
                    dump(r.outputItem.item);
                }
                if (r.outputLiquid != null && liquids.get(r.outputLiquid.liquid) > 0) {
                    dumpLiquid(r.outputLiquid.liquid);
                }
            }
        }

        /**
         * 执行一次配方的生产：消耗物品/液体，产出物品/液体。
         */
        protected void craft(Recipe recipe) {
            // 消耗物品
            if (recipe.inputItems != null) {
                for (ItemStack stack : recipe.inputItems) {
                    items.remove(stack.item, stack.amount);
                }
            }
            // 消耗液体
            if (recipe.inputLiquids != null) {
                for (LiquidStack stack : recipe.inputLiquids) {
                    liquids.remove(stack.liquid, stack.amount);
                }
            }
            // 产出物品
            if (recipe.outputItem != null) {
                offload(recipe.outputItem.item);
            }
            // 产出液体
            if (recipe.outputLiquid != null) {
                handleLiquid(this, recipe.outputLiquid.liquid, recipe.outputLiquid.amount);
            }
        }

        private boolean hasAllMaterials(Recipe r) {
            // 检查物品
            if (r.inputItems != null) {
                for (ItemStack stack : r.inputItems) {
                    if (items.get(stack.item) < stack.amount) return false;
                }
            }
            // 检查液体
            if (r.inputLiquids != null) {
                for (LiquidStack stack : r.inputLiquids) {
                    if (liquids.get(stack.liquid) < stack.amount) return false;
                }
            }
            return true;
        }

        private boolean outputFull(Recipe r) {
            if (r.outputItem != null && items.get(r.outputItem.item) >= itemCapacity) return true;
            if (r.outputLiquid != null && liquids.get(r.outputLiquid.liquid) >= liquidCapacity) return true;
            return false;
        }
        @Override
        public boolean acceptItem(Building source, Item item) {
            // 遍历所有配方，只要任意配方需要该物品就允许输入
            for (Recipe r : recipes) {
                if (r.inputItems != null) {
                    for (ItemStack stack : r.inputItems) {
                        if (stack.item == item && items.get(item) < itemCapacity) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            for (Recipe r : recipes) {
                if (r.inputLiquids != null) {
                    for (LiquidStack stack : r.inputLiquids) {
                        if (stack.liquid == liquid && liquids.get(liquid) < liquidCapacity) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
