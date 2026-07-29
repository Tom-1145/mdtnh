package mdtnh;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;          // 新增：导入 Reads 和 Writes
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;
import mindustry.world.draw.*;

public class RecipeCrafter extends GenericCrafter {

    /** 旧版配方列表（仅在没有分组时使用） */
    public Recipe[] recipes = new Recipe[]{};

    /** 配方分组（优先级最高） */
    public RecipeGroup[] groups = new RecipeGroup[]{};

    public RecipeCrafter(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        buildType = MDTFactoryBuild::new;
        drawer = new DrawDefault();
        buildVisibility = BuildVisibility.shown;
        requirements(Category.crafting, ItemStack.with(Items.copper, 50));
        itemCapacity = 20;
        liquidCapacity = 20f;

        configurable = true;
        saveConfig = true;
        copyConfig = true;

        // 配置处理器：接收组索引
        config(Integer.class, (MDTFactoryBuild build, Integer groupIdx) -> {
            build.selectedGroup = groupIdx;
            build.currentRecipe = -1;
            build.progress = 0f;
        });
    }

    /** 获取最终使用的配方列表（兼容旧版 recipes） */
    public RecipeGroup[] getEffectiveGroups() {
        if (groups.length > 0) {
            return groups;
        } else if (recipes.length > 0) {
            // 自动包装成单一默认组
            return new RecipeGroup[]{
                    new RecipeGroup("default", null, recipes)
            };
        } else {
            return new RecipeGroup[]{};
        }
    }

    /** 配方 */
    public static class Recipe {
        public ItemStack[] inputItems;
        public LiquidStack[] inputLiquids;
        public ItemStack outputItem;
        public LiquidStack outputLiquid;
        public float craftTime;

        public Recipe(ItemStack[] inputItems, LiquidStack[] inputLiquids,
                      ItemStack outputItem, LiquidStack outputLiquid, float craftTime) {
            this.inputItems = inputItems;
            this.inputLiquids = inputLiquids;
            this.outputItem = outputItem;
            this.outputLiquid = outputLiquid;
            this.craftTime = craftTime;
        }

        public static Recipe items(ItemStack[] in, ItemStack out, float time) {
            return new Recipe(in, new LiquidStack[]{}, out, null, time);
        }

        public static Recipe withLiquid(ItemStack[] in, LiquidStack[] liqIn,
                                        ItemStack out, LiquidStack liqOut, float time) {
            return new Recipe(in, liqIn, out, liqOut, time);
        }
    }

    /** 配方分组 */
    public static class RecipeGroup {
        public String name;       // 内部名，用于本地化
        public TextureRegion icon; // 可选图标
        public Recipe[] recipes;

        public RecipeGroup(String name, TextureRegion icon, Recipe[] recipes) {
            this.name = name;
            this.icon = icon;
            this.recipes = recipes;
        }
    }

    @Override
    public void setBars() {
        super.setBars();

        addBar("progress", (MDTFactoryBuild build) -> new Bar(
                () -> {
                    RecipeGroup[] groups = getEffectiveGroups();
                    if (build.selectedGroup >= 0 && build.selectedGroup < groups.length) {
                        RecipeGroup group = groups[build.selectedGroup];
                        String groupName = Core.bundle.get("group." + group.name, group.name);
                        if (build.currentRecipe >= 0 && build.currentRecipe < group.recipes.length) {
                            Recipe r = group.recipes[build.currentRecipe];
                            String itemName = r.outputItem != null ? r.outputItem.item.localizedName :
                                    (r.outputLiquid != null ? r.outputLiquid.liquid.localizedName : "???");
                            return groupName + " - " + itemName + " " + (int)(build.progress * 100) + "%";
                        }
                        return groupName + " - 空闲";
                    }
                    return Core.bundle.get("bar.no-recipe", "无配方");
                },
                () -> Pal.accent,
                () -> build.progress
        ));
    }

    @Override
    public void setStats() {
        super.setStats();

        RecipeGroup[] groups = getEffectiveGroups();
        if (groups.length == 0) return;

        stats.add(Stat.output, table -> {
            for (RecipeGroup group : groups) {
                String groupName = Core.bundle.get("group." + group.name, group.name);
                table.add("[accent]" + groupName + "[]").padTop(8).colspan(2).left().row();
                for (Recipe r : group.recipes) {
                    if (r.outputItem != null) {
                        table.image(r.outputItem.item.uiIcon).size(24);        // 修正
                        table.add(r.outputItem.item.localizedName + " x" + r.outputItem.amount).left().padLeft(4).row();
                    }
                    if (r.outputLiquid != null) {
                        table.image(r.outputLiquid.liquid.uiIcon).size(24);    // 修正
                        table.add(r.outputLiquid.liquid.localizedName + " " + r.outputLiquid.amount + "单位").left().padLeft(4).row();
                    }
                }
            }
        });
    }

    // 核心建筑实体
    public class MDTFactoryBuild extends GenericCrafterBuild {
        public int selectedGroup = -1;
        public int currentRecipe = -1;

        @Override
        public void updateTile() {
            RecipeGroup[] groups = getEffectiveGroups();

            // 持续输出所有可能的产物（无论选择哪个组）
            for (RecipeGroup group : groups) {
                for (Recipe r : group.recipes) {
                    if (r.outputItem != null && items.has(r.outputItem.item)) {
                        dump(r.outputItem.item);
                    }
                    if (r.outputLiquid != null && liquids.get(r.outputLiquid.liquid) > 0.001f) {
                        dumpLiquid(r.outputLiquid.liquid);
                    }
                }
            }

            // 没有选中组则停止
            if (selectedGroup < 0 || selectedGroup >= groups.length) {
                progress = 0f;
                currentRecipe = -1;
                return;
            }

            Recipe[] activeRecipes = groups[selectedGroup].recipes;

            // 检查当前配方是否仍有效
            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                if (!hasAllMaterials(activeRecipes[currentRecipe]) || outputFull(activeRecipes[currentRecipe])) {
                    currentRecipe = -1;
                }
            }

            // 按顺序选择第一个可行的配方
            if (currentRecipe == -1) {
                for (int i = 0; i < activeRecipes.length; i++) {
                    if (hasAllMaterials(activeRecipes[i]) && !outputFull(activeRecipes[i])) {
                        currentRecipe = i;
                        progress = 0f;
                        break;
                    }
                }
            }

            // 生产逻辑
            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                Recipe active = activeRecipes[currentRecipe];
                craftTime = active.craftTime;
                if (shouldConsume()) {
                    progress += (1.0f / craftTime) * delta() * efficiency;
                }
                if (progress >= 1f) {
                    craft(active);
                    progress %= 1f;
                    // 即时输出
                    if (active.outputItem != null) dump(active.outputItem.item);
                    if (active.outputLiquid != null) dumpLiquid(active.outputLiquid.liquid);
                    // 检查是否还能继续
                    if (!hasAllMaterials(active) || outputFull(active)) {
                        currentRecipe = -1;
                    }
                }
            } else {
                progress = 0f;
            }
        }

        protected void craft(Recipe recipe) {
            if (recipe.inputItems != null) {
                for (ItemStack stack : recipe.inputItems) {
                    items.remove(stack.item, stack.amount);
                }
            }
            if (recipe.inputLiquids != null) {
                for (LiquidStack stack : recipe.inputLiquids) {
                    liquids.remove(stack.liquid, stack.amount);
                }
            }
            if (recipe.outputItem != null) {
                offload(recipe.outputItem.item);
            }
            if (recipe.outputLiquid != null) {
                handleLiquid(this, recipe.outputLiquid.liquid, recipe.outputLiquid.amount);
            }
        }

        private boolean hasAllMaterials(Recipe r) {
            if (r.inputItems != null) {
                for (ItemStack stack : r.inputItems) {
                    if (items.get(stack.item) < stack.amount) return false;
                }
            }
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
            for (RecipeGroup group : getEffectiveGroups()) {
                for (Recipe r : group.recipes) {
                    if (r.inputItems != null) {
                        for (ItemStack stack : r.inputItems) {
                            if (stack.item == item && items.get(item) < itemCapacity) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            for (RecipeGroup group : getEffectiveGroups()) {
                for (Recipe r : group.recipes) {
                    if (r.inputLiquids != null) {
                        for (LiquidStack stack : r.inputLiquids) {
                            if (stack.liquid == liquid && liquids.get(liquid) < liquidCapacity) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public void buildConfiguration(Table table) {
            table.clear();
            RecipeGroup[] groups = getEffectiveGroups();
            if (groups.length == 0) return;

            for (int i = 0; i < groups.length; i++) {
                final int idx = i;
                RecipeGroup group = groups[i];

                // 获取图标
                TextureRegion icon = group.icon;
                if (icon == null && group.recipes.length > 0) {
                    Recipe first = group.recipes[0];
                    if (first.outputItem != null) icon = first.outputItem.item.uiIcon;
                    else if (first.outputLiquid != null) icon = first.outputLiquid.liquid.uiIcon;
                }
                if (icon == null) icon = Core.atlas.find("error");

                // 创建按钮（必须使用 Drawable + Styles + size + Runnable）
                ImageButton btn = table.button(
                        new TextureRegionDrawable(icon),
                        Styles.defaulti,  // 或 Styles.flati
                        40,              // 按钮默认尺寸，会被外部 .size(50f) 覆盖
                        () -> configure(idx)
                ).size(50f).pad(4f).get();

                btn.setChecked(idx == selectedGroup);
                table.add(Core.bundle.get("group." + group.name, group.name)).pad(4f);
                table.row();
            }
        }

        @Override
        public Object config() {
            return selectedGroup;
        }

        // 存档读写
        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(selectedGroup);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            if (revision >= 1) {
                selectedGroup = read.i();
            }
        }
    }
}