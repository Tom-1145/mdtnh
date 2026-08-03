package mdtnh;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mdtnh.energy.EnergySpec;
import mdtnh.energy.EnergyState;
import mdtnh.energy.MdtEnergyNode;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;
import mindustry.world.draw.*;

public class RecipeCrafter extends GenericCrafter {
    public TextureRegion[] Programming_circuit = new TextureRegion[24];

    public Recipe[] recipes = new Recipe[]{};
    public RecipeGroup[] groups = new RecipeGroup[]{};

    /** 能源规格。 */
    public final EnergySpec energySpec = new EnergySpec();

    /** 新放置工厂的初始缓存比例；通常保持 0，由网络充电。 */
    public float initialEnergyFraction = 0f;

    public RecipeCrafter(String name) {
        super(name);
        // 能源默认值
        energySpec.role = EnergySpec.Role.consumer;
        energySpec.voltageV = 12f;
        energySpec.capacityJ = 360f;
        energySpec.maxInputA = 6;
        energySpec.maxOutputA = 0;

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

        config(Integer.class, (MDTFactoryBuild build, Integer groupIdx) -> {
            build.selectedGroup = groupIdx;
            build.currentRecipe = -1;
            build.progress = 0f;
        });
    }

    @Override
    public void load() {
        super.load();
        if (groups.length == 0 && recipes.length > 0) {
            groups = new RecipeGroup[]{ new RecipeGroup("default", recipes) };
        }
        for (int i = 0; i < groups.length; i++) {
            groups[i].Texture_name = "programming-circuit" + (i + 1);
        }
    }

    public RecipeGroup[] getEffectiveGroups() {
        return groups;
    }

    /** 配方（增加能源消耗） */
    public static class Recipe {
        public ItemStack[] inputItems;
        public LiquidStack[] inputLiquids;
        public ItemStack outputItem;
        public LiquidStack outputLiquid;
        public float craftTime;
        public float energyPerCraftJ; // 新增

        public Recipe(ItemStack[] inputItems, LiquidStack[] inputLiquids,
                      ItemStack outputItem, LiquidStack outputLiquid, float craftTime) {
            this(inputItems, inputLiquids, outputItem, outputLiquid, craftTime, 0f);
        }

        public Recipe(ItemStack[] inputItems, LiquidStack[] inputLiquids,
                      ItemStack outputItem, LiquidStack outputLiquid, float craftTime, float energyPerCraftJ) {
            this.inputItems = inputItems;
            this.inputLiquids = inputLiquids;
            this.outputItem = outputItem;
            this.outputLiquid = outputLiquid;
            this.craftTime = craftTime;
            this.energyPerCraftJ = energyPerCraftJ;
        }

        public Recipe energy(float joules) {
            this.energyPerCraftJ = Math.max(0f, joules);
            return this;
        }

        public static Recipe items(ItemStack[] in, ItemStack out, float time) {
            return new Recipe(in, new LiquidStack[]{}, out, null, time);
        }

        public static Recipe withLiquid(ItemStack[] in, LiquidStack[] liqIn,
                                        ItemStack out, LiquidStack liqOut, float time) {
            return new Recipe(in, liqIn, out, liqOut, time);
        }
    }

    /** 配方组 */
    public static class RecipeGroup {
        public String name;
        public TextureRegion icon;
        public String Texture_name;
        public Recipe[] recipes;

        public RecipeGroup(String name, Recipe[] recipes) {
            this.name = name;
            this.recipes = recipes;
            this.icon = null;
        }
    }

    @Override
    public void setBars() {
        super.setBars();

        addBar("mdt-energy", (MDTFactoryBuild build) -> new Bar(
                () -> "Energy: " + Math.round(build.energyState.energyJ)
                        + " / " + Math.round(energySpec.capacityJ) + " J",
                () -> Color.valueOf("ffd37f"),
                () -> energySpec.capacityJ <= 0f
                        ? 0f
                        : Math.min(1f, build.energyState.energyJ / energySpec.capacityJ)
        ));

        addBar("mdt-energy-io", (MDTFactoryBuild build) -> {
            int maximum = Math.max(1, Math.max(energySpec.maxInputA, energySpec.maxOutputA));
            return new Bar(
                    () -> "I/O: " + build.energyState.inputA + " A in, "
                            + build.energyState.outputA + " A out",
                    () -> Color.valueOf("84f491"),
                    () -> Math.min(1f,
                            Math.max(build.energyState.inputA, build.energyState.outputA)
                                    / (float) maximum)
            );
        });

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
                        table.image(r.outputItem.item.uiIcon).size(24);
                        table.add(r.outputItem.item.localizedName + " x" + r.outputItem.amount).left().padLeft(4).row();
                    }
                    if (r.outputLiquid != null) {
                        table.image(r.outputLiquid.liquid.uiIcon).size(24);
                        table.add(r.outputLiquid.liquid.localizedName + " " + r.outputLiquid.amount + "单位").left().padLeft(4).row();
                    }
                }
            }
        });
    }

    // 核心建筑实体，实现能源节点
    public class MDTFactoryBuild extends GenericCrafterBuild implements MdtEnergyNode {
        public final EnergyState energyState = new EnergyState();
        public int selectedGroup = -1;
        public int currentRecipe = -1;

        @Override
        public Building energyBuilding() {
            return this;
        }

        @Override
        public EnergySpec energySpec() {
            return RecipeCrafter.this.energySpec;
        }

        @Override
        public EnergyState energyState() {
            return energyState;
        }

        @Override
        public void created() {
            super.created();
            float fraction = Math.max(0f, Math.min(1f, initialEnergyFraction));
            energyState.energyJ = energySpec.capacityJ * fraction;
        }

        @Override
        public void updateTile() {
            RecipeGroup[] groups = getEffectiveGroups();

            // 输出产物（无关能源）
            for (RecipeGroup group : groups) {
                for (Recipe r : group.recipes) {
                    if (r.outputItem != null && items.has(r.outputItem.item)) dump(r.outputItem.item);
                    if (r.outputLiquid != null && liquids.get(r.outputLiquid.liquid) > 0.001f) dumpLiquid(r.outputLiquid.liquid);
                }
            }

            if (selectedGroup < 0 || selectedGroup >= groups.length) {
                progress = 0f;
                currentRecipe = -1;
                return;
            }

            Recipe[] activeRecipes = groups[selectedGroup].recipes;

            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                if (!hasAllMaterials(activeRecipes[currentRecipe]) || outputFull(activeRecipes[currentRecipe])) {
                    currentRecipe = -1;
                }
            }

            if (currentRecipe == -1) {
                for (int i = 0; i < activeRecipes.length; i++) {
                    if (hasAllMaterials(activeRecipes[i]) && !outputFull(activeRecipes[i])) {
                        currentRecipe = i;
                        progress = 0f;
                        break;
                    }
                }
            }

            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                Recipe active = activeRecipes[currentRecipe];
                craftTime = active.craftTime;

                // 材料足够，检查能量消耗
                if (shouldConsume()) {
                    float workTicks = delta() * efficiency;
                    float requiredEnergyJ = active.energyPerCraftJ * workTicks / active.craftTime;

                    if (energyState.consume(requiredEnergyJ)) {
                        progress += workTicks / active.craftTime;
                    }
                    // 否则暂停，不消耗材料，保留进度
                }

                if (progress >= 1f) {
                    craft(active);
                    progress %= 1f;
                    if (active.outputItem != null) dump(active.outputItem.item);
                    if (active.outputLiquid != null) dumpLiquid(active.outputLiquid.liquid);

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
                for (ItemStack stack : recipe.inputItems) items.remove(stack.item, stack.amount);
            }
            if (recipe.inputLiquids != null) {
                for (LiquidStack stack : recipe.inputLiquids) liquids.remove(stack.liquid, stack.amount);
            }
            if (recipe.outputItem != null) offload(recipe.outputItem.item);
            if (recipe.outputLiquid != null) handleLiquid(this, recipe.outputLiquid.liquid, recipe.outputLiquid.amount);
        }

        private boolean hasAllMaterials(Recipe r) {
            if (r.inputItems != null) {
                for (ItemStack stack : r.inputItems) if (items.get(stack.item) < stack.amount) return false;
            }
            if (r.inputLiquids != null) {
                for (LiquidStack stack : r.inputLiquids) if (liquids.get(stack.liquid) < stack.amount) return false;
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
                            if (stack.item == item && items.get(item) < itemCapacity) return true;
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
                            if (stack.liquid == liquid && liquids.get(liquid) < liquidCapacity) return true;
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

            TextureRegion errorRegion = Core.atlas.find("error");
            String modName = Vars.mods.getMod(MainMod.class).name;

            for (int i = 0; i < groups.length; i++) {
                final int idx = i;
                RecipeGroup group = groups[i];

                TextureRegion icon = null;
                if (group.Texture_name != null && !group.Texture_name.isEmpty()) {
                    String atlasName = modName + "-" + group.Texture_name;
                    TextureRegion loaded = Core.atlas.find(atlasName);
                    if (loaded != null && loaded != errorRegion) {
                        icon = loaded;
                        Log.info("Group @ loaded custom icon: @", i, atlasName);
                    } else {
                        Log.warn("Group @ failed to load icon '@'; atlas has region: @", i, atlasName, Core.atlas.has(atlasName));
                    }
                }

                if (icon == null && group.recipes != null && group.recipes.length > 0) {
                    Recipe first = group.recipes[0];
                    if (first.outputItem != null) icon = first.outputItem.item.uiIcon;
                    else if (first.outputLiquid != null) icon = first.outputLiquid.liquid.uiIcon;
                }

                if (icon == null || icon == errorRegion) icon = errorRegion;
                group.icon = icon;

                TextureRegionDrawable drawable = new TextureRegionDrawable(icon);
                ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle(Styles.defaulti);
                style.imageUp = drawable;
                style.imageChecked = drawable;
                style.imageDisabled = drawable;

                ImageButton button = new ImageButton(style);
                button.clicked(() -> configure(idx));
                button.setChecked(idx == selectedGroup);

                table.add(button).size(50f).pad(4f);
                table.add(Core.bundle.get("group." + group.name, group.name)).pad(4f);
                table.row();
            }
        }

        @Override
        public Object config() {
            return selectedGroup;
        }

        // 存档版本升级：版本 2 保存 energyState
        @Override
        public byte version() {
            return 2;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(selectedGroup);
            energyState.write(write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            if (revision >= 1) {
                selectedGroup = read.i();
            }
            if (revision >= 2) {
                energyState.read(read, energySpec());
            } else {
                float fraction = Math.max(0f, Math.min(1f, initialEnergyFraction));
                energyState.energyJ = energySpec.capacityJ * fraction;
            }
        }
    }
}