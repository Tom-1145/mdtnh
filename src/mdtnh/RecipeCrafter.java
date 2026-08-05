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

import java.util.*;

/**
 * 支持配方分组、物品/液体输入输出和 MDT 能源消耗的单方块工厂。
 *
 * <p>方块定义保存可选配方组与能源规格；每个已放置建筑保存当前选中的配方组、
 * 正在执行的配方、生产进度和独立能源缓存。建筑通过 {@link MdtEnergyNode}
 * 接入离散能源网络，不使用 Mindustry 原生电力模块。</p>
 */
public class RecipeCrafter extends GenericCrafter {
    /** 预留的编程电路图标数组；当前配方组界面主要通过 Texture_name 动态读取图标。 */
    public TextureRegion[] Programming_circuit = new TextureRegion[24];

    /**
     * 未使用分组时的默认配方数组。
     *
     * <p>{@link #load()} 会在 groups 为空时把该数组包装成名为 default 的配方组。</p>
     */
    public Recipe[] recipes = new Recipe[]{};

    /** 配方分组；配置界面通过组索引切换当前可执行配方集合。 */
    public RecipeGroup[] groups = new RecipeGroup[]{};

    /** 该工厂类型共享的输出电压、输入电压区间、内部容量和电流上限。 */
    public final EnergySpec energySpec = new EnergySpec();

    /** 新放置工厂的初始能源缓存比例；通常为 0，由外部网络充电。 */
    public float initialEnergyFraction = 0f;

    public RecipeCrafter(String name) {
        super(name);
        // 默认作为只接收能量的消费者，正常输入范围为 10V 到 14V。
        energySpec.role = EnergySpec.Role.consumer;
        energySpec.voltageV = 12f;
        energySpec.minInputVoltageV = 10f;
        energySpec.maxInputVoltageV = 14f;
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

        /*
         * 配置值只保存配方组索引。切换组时清除当前配方与进度，
         * 防止把上一组尚未完成的生产进度带入另一组。
         */
        config(Integer.class, (MDTFactoryBuild build, Integer groupIdx) -> {
            build.selectedGroup = groupIdx;
            build.currentRecipe = -1;
            build.progress = 0f;
        });
    }

    /**
     * 整理配方组并为未指定图标名称的组分配默认图集名称。
     *
     * <p>当调用方只填写 recipes 而没有填写 groups 时，会自动生成一个 default 组，
     * 使单数组配置也能走统一的分组逻辑。</p>
     */
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

    /** @return 运行时实际使用的配方组数组。 */
    public RecipeGroup[] getEffectiveGroups() {
        return groups;
    }

    /**
     * 一条可执行配方。
     *
     * <p>energyPerCraftJ 表示从进度 0 到 1 完整执行一次所需的总能量。
     * 运行时会按本 tick 完成的工作比例均匀扣除，而不是完成时一次性扣除。</p>
     */
    public static class Recipe {
        public ItemStack[] inputItems;
        public LiquidStack[] inputLiquids;
        public ItemStack outputItem;
        public LiquidStack outputLiquid;
        /** 完成一次配方所需的基础 tick 数。 */
        public float craftTime;

        /** 完成一次配方所需的总能量，单位为焦耳。 */
        public float energyPerCraftJ;

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

        /**
         * 设置该配方完成一次所需的总能量。
         *
         * @param joules 总能耗；负数会被限制为 0
         * @return 当前配方，便于使用链式配置
         */
        public Recipe energy(float joules) {
            this.energyPerCraftJ = Math.max(0f, joules);
            return this;
        }

        /** 创建只有物品输入和物品输出的配方。 */
        public static Recipe items(ItemStack[] in, ItemStack out, float time) {
            return new Recipe(in, new LiquidStack[]{}, out, null, time);
        }

        /** 创建同时支持物品与液体输入输出的配方。 */
        public static Recipe withLiquid(ItemStack[] in, LiquidStack[] liqIn,
                                        ItemStack out, LiquidStack liqOut, float time) {
            return new Recipe(in, liqIn, out, liqOut, time);
        }
    }

    /**
     * 配方选择界面中的一个分组。
     *
     * <p>name 用于本地化键 {@code group.<name>}；Texture_name 指向模组图集中的
     * 可选图标；recipes 保存该组按顺序尝试的配方。</p>
     */
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

        public void addRecipe(Recipe recipe){
            List<Recipe> x = new ArrayList<>(List.of(recipes));
            x.add(recipe);
            recipes=x.toArray(new Recipe[0]);
        }
    }

    /**
     * 注册能源缓存、每秒输入输出电流和当前配方进度三种状态条。
     */
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
                            + build.energyState.outputA + " A out | "
                            + Math.round(build.energyState.lastInputVoltageV * 10f) / 10f + " V"
                            + " [" + energySpec.minInputVoltageV + "~"
                            + energySpec.maxInputVoltageV + " V]"
                            + (build.energyState.ignoredInputA > 0
                            ? " | ignored " + build.energyState.ignoredInputA : ""),
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

    /**
     * 在方块信息面板中按配方组列出所有可能的物品和液体产物。
     */
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

    /**
     * 单个已放置工厂的运行实体。
     *
     * <p>继承 {@link GenericCrafterBuild} 以复用物品、液体、效率和输出接口，
     * 同时实现 {@link MdtEnergyNode} 以接收 MDT 能源网络传入的离散电流包。</p>
     */
    public class MDTFactoryBuild extends GenericCrafterBuild implements MdtEnergyNode {
        /** 该工厂实例的内部能源缓存及上一秒电流统计。 */
        public final EnergyState energyState = new EnergyState();

        /** 配置界面选中的配方组索引；-1 表示尚未选择。 */
        public int selectedGroup = -1;

        /** 当前组中正在执行的配方索引；-1 表示当前没有可执行配方。 */
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

        /**
         * 按 initialEnergyFraction 初始化新建筑的能源缓存。
         */
        @Override
        public void created() {
            super.created();
            float fraction = Math.max(0f, Math.min(1f, initialEnergyFraction));
            energyState.energyJ = energySpec.capacityJ * fraction;
        }

        /**
         * 每 tick 选择可执行配方、按工作比例扣除能量并推进生产进度。
         *
         * <p>材料只在配方完成时扣除；能源则随进度逐 tick 消耗。能量不足时保留
         * 当前进度并暂停，因此下一次获得能量后可以继续生产。</p>
         */
        @Override
        public void updateTile() {
            RecipeGroup[] groups = getEffectiveGroups();

            // 持续尝试输出库存中的所有可能产物，避免切换配方组后旧产物滞留。
            for (RecipeGroup group : groups) {
                for (Recipe r : group.recipes) {
                    if (r.outputItem != null && items.has(r.outputItem.item)) dump(r.outputItem.item);
                    if (r.outputLiquid != null && liquids.get(r.outputLiquid.liquid) > 0.001f) dumpLiquid(r.outputLiquid.liquid);
                }
            }

            // 未选择有效配方组时清除生产状态，但仍允许已存在产物继续输出。
            if (selectedGroup < 0 || selectedGroup >= groups.length) {
                progress = 0f;
                currentRecipe = -1;
                return;
            }

            Recipe[] activeRecipes = groups[selectedGroup].recipes;

            // 当前配方失去原料条件或产物空间后，重新进入配方选择流程。
            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                if (!hasAllMaterials(activeRecipes[currentRecipe]) || outputFull(activeRecipes[currentRecipe])) {
                    currentRecipe = -1;
                }
            }

            // 按数组顺序选择第一条同时满足原料与输出空间条件的配方。
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

                // 本 tick 能耗 = 配方总能耗 × 本 tick 有效工作量 / 配方总工时。
                if (shouldConsume()) {
                    float workTicks = delta() * efficiency;
                    float requiredEnergyJ = active.energyPerCraftJ * workTicks / active.craftTime;

                    if (energyState.consume(requiredEnergyJ)) {
                        progress += workTicks / active.craftTime;
                    }
                    // consume 返回 false 时不扣除部分能量，也不推进进度。
                }

                // 进度达到 1 后一次性结算原料与产物，并保留可能溢出的进度小数。
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

        /**
         * 结算一次已完成配方的物品、液体输入与输出。
         */
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

        /** 判断内部物品和液体模块是否包含配方要求的全部原料。 */
        private boolean hasAllMaterials(Recipe r) {
            if (r.inputItems != null) {
                for (ItemStack stack : r.inputItems) if (items.get(stack.item) < stack.amount) return false;
            }
            if (r.inputLiquids != null) {
                for (LiquidStack stack : r.inputLiquids) if (liquids.get(stack.liquid) < stack.amount) return false;
            }
            return true;
        }

        /**
         * 判断任一产物对应的内部存储是否已经达到方块容量。
         *
         * <p>该判断按当前产物类型检查已有数量，不预留完整产出数量。</p>
         */
        private boolean outputFull(Recipe r) {
            if (r.outputItem != null && items.get(r.outputItem.item) >= itemCapacity) return true;
            if (r.outputLiquid != null && liquids.get(r.outputLiquid.liquid) >= liquidCapacity) return true;
            return false;
        }

        /**
         * 只接收至少一条已注册配方会使用的物品。
         */
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

        /**
         * 只接收至少一条已注册配方会使用的液体。
         */
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

        /**
         * 构建配方组选择界面。
         *
         * <p>图标优先使用 Texture_name 指向的模组图集区域；读取失败时回退到该组
         * 第一条配方的产物图标，仍无法获得图标时显示 error 区域。</p>
         */
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

                // 复制默认按钮样式，避免影响 Mindustry 全局共享样式对象。
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

        /** @return 需要由 Mindustry 保存和同步的配方组索引。 */
        @Override
        public Object config() {
            return selectedGroup;
        }

        /**
         * 存档格式版本 2 依次保存配方组索引和当前能源缓存。
         */
        @Override
        public byte version() {
            return 2;
        }

        /** 将配方组选择和能源状态追加到父类存档数据。 */
        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(selectedGroup);
            energyState.write(write);
        }

        /**
         * 按存档版本恢复配置和能源状态。
         *
         * <p>版本 1 只包含配方组索引；缺少能源字段时按当前初始荷电比例初始化。</p>
         */
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