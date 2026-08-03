package mdtnh;

import arc.Core;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mdtnh.energy.EnergyState;
import mdtnh.energy.MdtEnergyNode;
import mdtnh.hatch.EnergyInputHatch;
import mdtnh.hatch.Hatch;
import mdtnh.hatch.ItemInputHatch;
import mdtnh.hatch.ItemOutputHatch;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.draw.*;

import java.util.*;

/**
 * 由核心方块、物品舱室和能源舱室共同组成的多方块生产结构。
 *
 * <p>核心定期检查周围方块是否匹配某个 {@link LevelStruct}。结构成形后，
 * 核心从指定输入仓汇总原料、从指定能源仓按 tick 消耗能量，并把完成的产物
 * 写入指定输出仓。核心自身不保存物品或能量。</p>
 */
public class MultiblockStructer extends Block {

    /** 核心方块在世界和建造预览中的绘制器。 */
    public DrawBlock drawer = new DrawDefault();

    /** 核心方块使用的图集区域。 */
    public TextureRegion region;

    /**
     * 多方块结构可执行的一条物品配方。
     *
     * <p>energyPerCraftJ 表示完整生产一次的总能耗，并按每 tick 的工作比例
     * 从结构中的能源输入仓扣除。</p>
     */
    public static class Recipe {
        /** 完成一次生产需要从所有输入仓合计取得的物品。 */
        public ItemStack[] inputItems;

        /** 完成一次生产后写入输出仓的物品。 */
        public ItemStack outputItem;

        /** 完成一次生产所需的基础 tick 数。 */
        public float craftTime;

        /** 完成一次生产所需的总能量，单位为焦耳。 */
        public float energyPerCraftJ;

        public Recipe(ItemStack[] inputItems, ItemStack outputItem, float craftTime) {
            this(inputItems, outputItem, craftTime, 0f);
        }

        public Recipe(ItemStack[] inputItems, ItemStack outputItem, float craftTime, float energyPerCraftJ) {
            this.inputItems = inputItems;
            this.outputItem = outputItem;
            this.craftTime = craftTime;
            this.energyPerCraftJ = energyPerCraftJ;
        }

        /**
         * 设置完成一次配方所需的总能量。
         *
         * @param joules 总能耗；负数会被限制为 0
         * @return 当前配方，便于链式配置
         */
        public Recipe energy(float joules) {
            this.energyPerCraftJ = Math.max(0f, joules);
            return this;
        }

        /** 创建一条物品输入、物品输出配方。 */
        public static Recipe items(ItemStack[] in, ItemStack out, float time) {
            return new Recipe(in, out, time);
        }
    }

    /**
     * 配置界面中的配方分组。
     *
     * <p>name 用于本地化键 {@code group.<name>}；Texture_name 是可选图标名称；
     * recipes 保存该组按顺序尝试的配方。</p>
     */
    public static class RecipeGroup {
        public String name;
        public TextureRegion icon;
        public String Texture_name;
        public Recipe[] recipes;

        public RecipeGroup(String name, Recipe[] recipes) {
            this.name = name;
            this.recipes = recipes;
        }
    }

    /**
     * 一种可识别的结构等级。
     *
     * <p>struct 把核心相对坐标映射到 Mapping 中的槽位类型下标；
     * Mapping 的每个元素则列出该槽位允许出现的方块。</p>
     */
    public static class LevelStruct {

        /** 核心相对坐标到槽位类型下标的映射。 */
        public Map<pos, Integer> struct;

        /** 每种槽位类型允许出现的方块列表。 */
        public List<List<Block>> Mapping;

        /**
         * 等级级配方字段。
         *
         * <p>当前生产逻辑统一使用外层 groups，不读取该字段。</p>
         */
        public Recipe recipe;
    }

    /** 按数组顺序定义的结构等级；后面的匹配等级会覆盖前面的等级编号。 */
    public List<LevelStruct> levels = new ArrayList<>();

    /** 所有等级共用的可选配方组。 */
    public RecipeGroup[] groups = new RecipeGroup[]{};

    public MultiblockStructer(String name) {
        super(name);
        rotate = true;
        update = true;
        solid = true;
        buildType = MultiblockStructerBuilding::new;
        drawer = new DrawDefault();

        configurable = true;
        saveConfig = true;
        copyConfig = true;

        /*
         * 配置值只保存配方组索引。切换组时重置当前配方和进度，
         * 避免不同配方组之间共享未完成进度。
         */
        config(Integer.class, (MultiblockStructerBuilding build, Integer groupIdx) -> {
            build.selectedGroup = groupIdx;
            build.currentRecipe = -1;
            build.progress = 0f;
        });
    }

    /** 加载核心方块图集区域。 */
    @Override
    public void load() {
        super.load();
        region = Core.atlas.find(name);
    }

    /** 在静态方块底图阶段绘制核心贴图。 */
    @Override
    public void drawBase(Tile tile) {
        Draw.rect(region, tile.worldx(), tile.worldy());
    }

    /** 使用 drawer 绘制建造预览；无绘制器时直接绘制核心区域。 */
    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        if (drawer != null) {
            drawer.drawPlan(this, plan, list);
        } else {
            Draw.rect(region, plan.drawx(), plan.drawy(), plan.rotation * 90);
        }
    }

    /**
     * 以核心 tile 为原点的整数相对坐标。
     *
     * <p>该对象作为 Map 键使用，因此 equals 与 hashCode 必须同时基于 x、y。</p>
     */
    public static class pos {
        public int x, y;
        public pos() {}
        public pos(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            pos pos = (pos) o;
            return x == pos.x && y == pos.y;
        }
        @Override
        public int hashCode() {
            return x * 31 + y;
        }
    }

    /** 注册结构等级和当前配方进度状态条。 */
    @Override
    public void setBars() {
        super.setBars();

        addBar("level", (MultiblockStructerBuilding build) -> new Bar(
                () -> build.Molded ? "level:" + build.level : Core.bundle.get("bar.unformed", "未成形"),
                () -> Pal.accent,
                () -> 1f
        ));

        addBar("progress", (MultiblockStructerBuilding build) -> new Bar(
                () -> {
                    if (!build.Molded || build.selectedGroup < 0 || build.selectedGroup >= groups.length)
                        return Core.bundle.get("bar.idle", "空闲");
                    RecipeGroup group = groups[build.selectedGroup];
                    String groupName = Core.bundle.get("group." + group.name, group.name);
                    if (build.currentRecipe >= 0 && build.currentRecipe < group.recipes.length) {
                        Recipe r = group.recipes[build.currentRecipe];
                        String itemName = r.outputItem != null ? r.outputItem.item.localizedName : "???";
                        return groupName + " - " + itemName + " " + (int)(build.progress * 100) + "%";
                    }
                    return groupName + " - 空闲";
                },
                () -> Pal.accent,
                () -> build.Molded ? build.progress : 0f
        ));
    }

    /**
     * 已放置的多方块核心建筑。
     *
     * <p>核心保存结构成形状态、当前等级、配方选择和生产进度；
     * 原料、产物与能量实际存放在结构周围的舱室建筑中。</p>
     */
    public class MultiblockStructerBuilding extends Building {
        /** 当前周围方块是否匹配至少一个结构等级。 */
        public boolean Molded;

        /** 当前匹配的最高结构等级；0 表示未成形。 */
        public int level;

        /** 当前配方进度，通常位于 0 到 1 之间。 */
        public float progress;

        /** 配置界面选中的配方组索引；-1 表示未选择。 */
        public int selectedGroup = -1;

        /** 当前组中正在执行的配方索引；-1 表示没有可执行配方。 */
        public int currentRecipe = -1;

        /** 舱室位置 */
        public pos[] currentInputs;
        public pos[] currentOutputs;
        public pos[] currentEnergyInputs;

        /**
         * 检查核心周围是否满足结构定义。
         *
         * <p>每个等级都会逐个验证相对坐标上的方块是否属于对应允许列表。
         * 方法不会在找到第一个匹配后退出，因此若多个等级同时匹配，最终采用
         * levels 中位置靠后的等级。</p>
         *
         * <p>同时动态更新舱室位置</p>
         */
        public void CheckStruct() {
            level = 0;
            Molded = false;

            for (int i = 1; i <= levels.size(); i++) {
                LevelStruct now = levels.get(i - 1);
                boolean accept = true;

                List<pos> foundInputs = new ArrayList<>();
                List<pos> foundOutputs = new ArrayList<>();
                List<pos> foundEnergyInputs = new ArrayList<>();

                for (Map.Entry<pos, Integer> ps : now.struct.entrySet()) {
                    int dx = ps.getKey().x;
                    int dy = ps.getKey().y;
                    Tile checkTile = Vars.world.tile(tile.x + dx, tile.y + dy);

                    if (checkTile == null) {
                        accept = false;
                        break;
                    }

                    Block blockThere = checkTile.block();
                    int typeIndex = ps.getValue();
                    List<Block> allowed = now.Mapping.get(typeIndex);

                    if (allowed == null || !allowed.contains(blockThere)) {
                        accept = false;
                        break;
                    }

                    if (blockThere instanceof Hatch) {
                        if (blockThere instanceof ItemInputHatch) {
                            foundInputs.add(new pos(dx, dy));
                        } else if (blockThere instanceof ItemOutputHatch) {
                            foundOutputs.add(new pos(dx, dy));
                        } else if (blockThere instanceof EnergyInputHatch) {
                            foundEnergyInputs.add(new pos(dx, dy));
                        }
                    }
                }

                if (accept) {
                    level = i;
                    Molded = true;

                    currentInputs = foundInputs.toArray(new pos[0]);
                    currentOutputs = foundOutputs.toArray(new pos[0]);
                    currentEnergyInputs = foundEnergyInputs.toArray(new pos[0]);
                    break;
                }
            }
            if (!Molded) {
                currentInputs = null;
                currentOutputs = null;
                currentEnergyInputs = null;
            }
        }

        /** @return 当前等级对应的结构定义；等级无效时返回 {@code null}。 */
        private LevelStruct currentLevel() {
            return (level > 0 && level <= levels.size()) ? levels.get(level - 1) : null;
        }

        /**
         * 按输入仓坐标顺序取出指定物品。
         *
         * @return 实际取出的数量；各输入仓合计不足时可能小于 amount
         */
        private int takeFromInputs(Item item, int amount) {
            LevelStruct lvl = currentLevel();
            if (lvl == null || currentInputs == null) return 0;

            int remaining = amount;
            for (pos offset : currentInputs) {
                if (remaining <= 0) break;
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasItems) {
                    int canTake = Math.min(t.build.items.get(item), remaining);
                    t.build.items.remove(item, canTake);
                    remaining -= canTake;
                }
            }
            return amount - remaining;
        }

        /**
         * 汇总所有输入仓中的物品，判断是否满足整条配方。
         *
         * <p>先按物品类型合并需求量，再跨全部输入仓累计库存，因而同一种原料
         * 可以分散存放在多个舱室中。</p>
         */
        private boolean inputsHave(ItemStack[] items) {
            if (items == null || items.length == 0) return true;
            LevelStruct lvl = currentLevel();
            if (lvl == null || currentInputs == null) return false;

            Map<Item, Integer> needed = new HashMap<>();
            for (ItemStack stack : items) {
                needed.merge(stack.item, stack.amount, Integer::sum);
            }

            Map<Item, Integer> available = new HashMap<>();
            for (pos offset : currentInputs) {
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasItems) {
                    for (Item item : needed.keySet()) {
                        available.merge(item, t.build.items.get(item), Integer::sum);
                    }
                }
            }

            for (Map.Entry<Item, Integer> entry : needed.entrySet()) {
                if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * 按输出仓坐标顺序写入产物。
         *
         * @return 实际写入数量；总空间不足时可能小于 amount
         */
        private int putToOutputs(Item item, int amount) {
            LevelStruct lvl = currentLevel();
            if (lvl == null || currentOutputs == null) return 0;

            int remaining = amount;
            for (pos offset : currentOutputs) {
                if (remaining <= 0) break;
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasItems) {
                    int space = t.build.block.itemCapacity - t.build.items.get(item);
                    int canPut = Math.min(space, remaining);
                    if (canPut > 0) {
                        t.build.items.add(item, canPut);
                        remaining -= canPut;
                    }
                }
            }
            return amount - remaining;
        }

        /**
         * 判断所有输出仓的合计剩余空间能否容纳完整产物。
         */
        private boolean outputsFullFor(Item item, int amount) {
            LevelStruct lvl = currentLevel();
            if (lvl == null || currentOutputs == null) return true;

            int totalSpace = 0;
            for (pos offset : currentOutputs) {
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasItems) {
                    totalSpace += Math.max(0, t.build.block.itemCapacity - t.build.items.get(item));
                }
            }
            return totalSpace < amount;
        }

        /**
         * 汇总同队能源输入仓当前可用的能量。
         *
         * <p>坐标上的建筑只要实现 {@link MdtEnergyNode} 就可作为能源来源，
         * 因此核心不依赖具体的 EnergyInputHatch 建筑类型。</p>
         */
        private float availableEnergyJ() {
            LevelStruct lvl = currentLevel();
            if (lvl == null || currentEnergyInputs == null) return 0f;

            float total = 0f;
            for (pos offset : currentEnergyInputs) {
                Tile target = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (target == null || target.build == null) continue;
                if (target.build.team != team) continue;
                if (target.build instanceof MdtEnergyNode) {
                    MdtEnergyNode node = (MdtEnergyNode) target.build;
                    total += node.energyState().energyJ;
                }
            }
            return total;
        }

        /**
         * 从能源输入仓中全额扣除指定能量。
         *
         * <p>先通过 availableEnergyJ() 确认总量足够，再按坐标顺序逐仓扣除，
         * 从而保证能量不足时不会产生部分支付。</p>
         *
         * @return 已完成全额扣除时返回 {@code true}
         */
        private boolean consumeEnergyJ(float amountJ) {
            if (amountJ <= 0f) return true;
            if (availableEnergyJ() + 0.0001f < amountJ) return false;

            LevelStruct lvl = currentLevel();
            float remaining = amountJ;

            for (pos offset : currentEnergyInputs) {
                if (remaining <= 0.0001f) break;
                Tile target = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (target == null || target.build == null) continue;
                if (target.build.team != team) continue;
                if (!(target.build instanceof MdtEnergyNode)) continue;

                MdtEnergyNode node = (MdtEnergyNode) target.build;
                EnergyState state = node.energyState();
                float taken = Math.min(state.energyJ, remaining);
                state.energyJ -= taken;
                remaining -= taken;
            }
            return remaining <= 0.0001f;
        }

        /**
         * 更新结构检测、配方选择、能源消耗和生产结算。
         *
         * <p>结构每 60 tick 检查一次。配方执行期间，能源按 tick 均匀消耗，
         * 原料与产物在进度达到 1 时一次性结算。能源不足时保留进度并暂停。</p>
         */
        @Override
        public void updateTile() {
            super.updateTile();

            // 结构检查无需每 tick 执行，每秒检查一次可降低地图扫描开销。
            if (timer(0, 60f)) {
                boolean wasMolded = Molded;
                int oldLevel = level;
                CheckStruct();
                if (!Molded || level != oldLevel) {
                    progress = 0f;
                    currentRecipe = -1;
                }
            }

            if (!Molded) return;
            if (groups.length == 0) return;
            if (selectedGroup < 0 || selectedGroup >= groups.length) return;

            Recipe[] activeRecipes = groups[selectedGroup].recipes;
            if (activeRecipes.length == 0) return;

            // 当前配方失去原料条件或完整输出空间后，重新选择配方。
            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                Recipe active = activeRecipes[currentRecipe];
                if (!inputsHave(active.inputItems) || outputsFullFor(active.outputItem.item, active.outputItem.amount))
                    currentRecipe = -1;
            }

            // 按配方数组顺序选择第一条当前可执行的配方。
            if (currentRecipe == -1) {
                for (int i = 0; i < activeRecipes.length; i++) {
                    Recipe r = activeRecipes[i];
                    if (inputsHave(r.inputItems) && !outputsFullFor(r.outputItem.item, r.outputItem.amount)) {
                        currentRecipe = i;
                        progress = 0f;
                        break;
                    }
                }
            }

            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                Recipe active = activeRecipes[currentRecipe];

                float workTicks = delta();

                // 本 tick 能耗按当前工作量占完整配方工时的比例计算。
                float requiredEnergyJ = active.energyPerCraftJ * workTicks / active.craftTime;

                if (consumeEnergyJ(requiredEnergyJ)) {
                    progress += workTicks / active.craftTime;
                }

                // 完成时再次验证资源，避免生产过程中结构内容变化导致非法结算。
                if (progress >= 1f) {
                    if (!inputsHave(active.inputItems) || outputsFullFor(active.outputItem.item, active.outputItem.amount)) {
                        progress = 0f;
                        currentRecipe = -1;
                        return;
                    }

                    // 原料和产物只在完整生产周期完成时结算。
                    for (ItemStack stack : active.inputItems)
                        takeFromInputs(stack.item, stack.amount);

                    putToOutputs(active.outputItem.item, active.outputItem.amount);

                    progress %= 1f;

                    if (!inputsHave(active.inputItems) || outputsFullFor(active.outputItem.item, active.outputItem.amount))
                        currentRecipe = -1;
                }
            } else {
                progress = 0f;
            }
        }

        /**
         * 构建配方组选择界面。
         *
         * <p>优先使用 Texture_name 指向的模组图集区域；缺失时回退到该组
         * 第一条配方的产物图标，仍不可用时显示 error 区域。</p>
         */
        @Override
        public void buildConfiguration(Table table) {
            table.clear();
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
                }

                if (icon == null || icon == errorRegion) icon = errorRegion;
                group.icon = icon;

                // 复制默认按钮样式，避免影响全局共享的 Styles.defaulti。
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

        /** @return 由 Mindustry 保存和同步的配方组索引。 */
        @Override
        public Object config() {
            return selectedGroup;
        }

        /** 存档格式版本 1 保存当前配方组索引。 */
        @Override
        public byte version() {
            return 1;
        }

        /** 将配方组选择追加到父类建筑存档数据。 */
        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(selectedGroup);
        }

        /** 从版本 1 及以上存档恢复配方组选择。 */
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            if (revision >= 1) selectedGroup = read.i();
        }

        /** 使用方块绘制器渲染核心建筑。 */
        @Override
        public void draw() {
            if (drawer != null) drawer.draw(this);
            else Draw.rect(region, x, y);
        }

        /** 将核心绘制器提供的光照效果交给 Mindustry 渲染。 */
        @Override
        public void drawLight() {
            if (drawer != null) drawer.drawLight(this);
        }
    }
}