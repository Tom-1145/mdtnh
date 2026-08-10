package mdtnh;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.*;
import arc.input.KeyBind;
import arc.input.KeyCode;
import arc.struct.Seq;
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
import mdtnh.hatch.LiquidInputHatch;
import mdtnh.hatch.LiquidOutputHatch;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
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

    /**
     * 打开或关闭多方块结构预览的可重绑定按键。
     *
     * <p>该按键会自动出现在游戏的“设置 → 控制”列表中，默认值为 K。
     * 绑定名称和分类名称可通过语言包中的
     * {@code keybind.mdtnh_multiblock_preview.name} 与
     * {@code category.mdtnh.name} 本地化。</p>
     */
    public static final KeyBind structurePreviewKey =
            KeyBind.add("mdtnh_multiblock_preview", KeyCode.k, "mdtnh");

    /** 新建筑首次显示结构预览时使用的等级。 */
    public int defaultPreviewLevel = 1;

    /** 结构幽灵方块的不透明度。 */
    public float previewAlpha = 0.55f;

    /**
     * 预览只绘制缺失/不匹配的槽位，还是绘制整个结构的全部槽位。
     *
     * <p>结构已经完全成形时，所有槽位都已满足，若只绘制缺失槽位则幽灵方块
     * 列表为空，预览将什么都看不见；此时应设为 false 以显示整个结构轮廓。</p>
     */
    public boolean showMissingOnly = false;

    /** 是否输出结构幽灵方块预览相关的调试日志。 */
    public boolean debugPreview = true;

    /** 核心方块在世界和建造预览中的绘制器。 */
    public DrawBlock drawer = new DrawDefault();

    /** 核心方块使用的图集区域。 */
    public TextureRegion region;

    /** 并行数 */
    public int parallel=8;
    /**
     * 多方块结构可执行的一条物品配方。
     *
     * <p>energyPerCraftJ 表示完整生产一次的总能耗，并按每 tick 的工作比例
     * 从结构中的能源输入仓扣除。</p>
     */
    public static class Recipe {
        /** 完成一次生产需要从所有输入仓合计取得的物品。 */
        public ItemStack[] inputItems;

        /** 完成一次生产需要从所有液体输入仓合计取得的液体。 */
        public LiquidStack[] inputLiquids;

        /** 完成一次生产后写入物品输出仓的物品。 */
        public ItemStack[] outputItems;

        /** 完成一次生产后写入液体输出仓的液体。 */
        public LiquidStack[] outputLiquids;

        /** 完成一次生产所需的基础 tick 数。 */
        public float craftTime;

        /** 完成一次生产所需的总能量，单位为焦耳。 */
        public float energyPerCraftJ;

        public Recipe(ItemStack[] inputItems, LiquidStack[] inputLiquids,
                      ItemStack[] outputItems, LiquidStack[] outputLiquids, float craftTime) {
            this(inputItems, inputLiquids, outputItems, outputLiquids, craftTime, 0f);
        }

        public Recipe(ItemStack[] inputItems, LiquidStack[] inputLiquids,
                      ItemStack[] outputItems, LiquidStack[] outputLiquids,
                      float craftTime, float energyPerCraftJ) {
            this.inputItems = inputItems;
            this.inputLiquids = inputLiquids;
            this.outputItems = outputItems;
            this.outputLiquids = outputLiquids;
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

        /** 创建一条物品输入、单物品输出配方。 */
        public static Recipe items(ItemStack[] in, ItemStack out, float time) {
            return new Recipe(in, null, out == null ? null : new ItemStack[]{out}, null, time);
        }

        /** 创建一条物品输入、多种物品输出配方。 */
        public static Recipe items(ItemStack[] in, ItemStack[] out, float time) {
            return new Recipe(in, null, out, null, time);
        }

        /** 创建同时包含物品与液体输入、多种物品与多种液体输出的配方。 */
        public static Recipe withLiquid(ItemStack[] in, LiquidStack[] liqIn,
                                        ItemStack[] out, LiquidStack[] liqOut, float time) {
            return new Recipe(in, liqIn, out, liqOut, time);
        }

        /** @return 用于状态条显示的默认产物名称；没有产物时返回 null。 */
        public String primaryOutputName() {
            if (outputItems != null && outputItems.length > 0) return outputItems[0].item.localizedName;
            if (outputLiquids != null && outputLiquids.length > 0) return outputLiquids[0].liquid.localizedName;
            return null;
        }

        /**
         * 创建按指定并行数缩放后的独立配方副本。
         *
         * <p>不会修改原配方中的 ItemStack 或 LiquidStack。并行检测应当是只读操作，
         * 否则每次检测都会永久改变后续生产所需的物品数量。</p>
         */
        public Recipe times(int count) {
            int multiplier = Math.max(0, count);

            return new Recipe(
                    scaleItems(inputItems, multiplier),
                    scaleLiquids(inputLiquids, multiplier),
                    scaleItems(outputItems, multiplier),
                    scaleLiquids(outputLiquids, multiplier),
                    craftTime,
                    energyPerCraftJ * multiplier
            );
        }

        private static ItemStack[] scaleItems(ItemStack[] stacks, int multiplier) {
            if (stacks == null) return null;
            ItemStack[] scaled = new ItemStack[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                scaled[i] = new ItemStack(stacks[i].item, safeMultiply(stacks[i].amount, multiplier));
            }
            return scaled;
        }

        private static LiquidStack[] scaleLiquids(LiquidStack[] stacks, int multiplier) {
            if (stacks == null) return null;
            LiquidStack[] scaled = new LiquidStack[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                scaled[i] = new LiquidStack(stacks[i].liquid, stacks[i].amount * multiplier);
            }
            return scaled;
        }

        private static int safeMultiply(int amount, int multiplier) {
            long result = (long) amount * multiplier;
            return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
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
            build.currentParallel = 0;
            build.progress = 0f;
        });

        // 预览在全局 postDraw 阶段绘制，而不是依赖 Building.draw()。
        ensurePreviewDrawHook();
    }

    /**
     * 是否已经注册过全局预览绘制钩子。
     *
     * <p>同一进程内只注册一次；块实例是 Content 单例，因此静态标记足够。</p>
     */
    private static boolean previewDrawHookRegistered;

    /**
     * 注册全局渲染钩子，在每帧所有世界实体绘制完成后绘制幽灵方块。
     *
     * <p>Mindustry v159 中 Building 实体并不实现 {@code Drawc}，不会进入
     * {@code Groups.draw}，因此建筑的 {@code draw()} 不一定被调用；改用
     * {@link EventType.Trigger#postDraw} 保证在方块与建筑之后绘制预览。</p>
     */
    private void ensurePreviewDrawHook() {
        if (previewDrawHookRegistered) return;
        previewDrawHookRegistered = true;

        Events.run(EventType.Trigger.postDraw, () -> {
            if (Vars.state.isMenu()) return;
            for (Building b : Groups.build) {
                if (b instanceof MultiblockStructerBuilding mb
                        && mb.structurePreviewVisible
                        && mb.tile != null) {
                    mb.drawStructurePreview();
                }
            }
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
                        String itemName = r.primaryOutputName();
                        if (itemName == null) itemName = "???";
                        return groupName + " - " + itemName +"*"+ build.currentParallel +" " + (int)(build.progress * 100) + "%";
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

        public int currentParallel = 0;

        /** 舱室位置 */
        public pos[] currentInputs;
        public pos[] currentOutputs;
        public pos[] currentEnergyInputs;
        public pos[] currentLiquidInputs;
        public pos[] currentLiquidOutputs;

        /** 当前是否绘制结构幽灵方块；仅影响本地客户端显示。 */
        public boolean structurePreviewVisible;

        /**
         * 当前预览等级。
         *
         * <p>该值从 {@link MultiblockStructer#defaultPreviewLevel} 初始化，
         * 实际绘制时始终限制在 1 到 levels.size() 之间。</p>
         */
        public int structurePreviewLevel = defaultPreviewLevel;

        /** 当前一次预览键按住期间是否已经执行过等级调整。 */
        private boolean previewLevelAdjustedDuringHold;

        /** 是否已经在本建筑上开始处理一次预览键按压。 */
        private boolean previewKeyPressActive;

        /** 上一次逻辑 tick 时预览键是否处于按下状态，用于可靠的按键边沿检测。 */
        private boolean previewKeyDownLastFrame;

        /** 本次按压开始前预览是否已经可见，松开键时据此恢复切换语义。 */
        private boolean previewWasVisibleBeforePress;

        /** 上一次输出诊断日志时的 (可见, 等级, 计划数) 签名，仅在签名变化时输出，避免刷屏。 */
        private int lastLoggedPreviewSignature = -1;

        /** 上一次输出“进入绘制”日志时的 (可见, 等级) 签名。 */
        private int lastPreviewEntrySignature = -1;

        /** 上一次执行结构检查时的旋转值；旋转变化时立即重新检查结构。 */
        private int lastCheckedRotation = -1;

        /** 复用的绘制计划列表，避免每帧创建临时集合。 */
        private final Seq<BuildPlan> structurePreviewPlans = new Seq<>();

        /**
         * 将结构定义中的相对坐标按核心当前旋转方向变换到世界坐标。
         *
         * <p>Mindustry 的 rotation 从 0 到 3，每加一为顺时针旋转 90°；
         * 定义坐标固定为 rotation=0（上方）时的朝向，因此结构会随核心旋转。</p>
         *
         * @return 新的 pos 实例，不会修改结构定义中的原对象
         */
        private pos rotateOffset(pos offset) {
            int dx = offset.x;
            int dy = offset.y;
            switch (rotation & 3) {
                case 1:  return new pos(dy, -dx);
                case 2:  return new pos(-dx, -dy);
                case 3:  return new pos(-dy, dx);
                default: return new pos(dx, dy);
            }
        }

        /**
         * 检查核心周围是否满足结构定义。
         *
         * <p>每个等级都会逐个验证相对坐标上的方块是否属于对应允许列表。
         * 方法不会在找到第一个匹配后退出，因此若多个等级同时匹配，最终采用
         * levels 中位置靠后的等级。</p>
         *
         * <p>坐标会先按核心旋转方向变换，因此结构判定随核心旋转；同时动态更新
         * 舱室位置。</p>
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
                List<pos> foundLiquidInputs = new ArrayList<>();
                List<pos> foundLiquidOutputs = new ArrayList<>();

                for (Map.Entry<pos, Integer> ps : now.struct.entrySet()) {
                    pos worldOffset = rotateOffset(ps.getKey());
                    int dx = worldOffset.x;
                    int dy = worldOffset.y;
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
                        } else if (blockThere instanceof LiquidInputHatch) {
                            foundLiquidInputs.add(new pos(dx, dy));
                        } else if (blockThere instanceof LiquidOutputHatch) {
                            foundLiquidOutputs.add(new pos(dx, dy));
                        }
                    }
                }

                if (accept) {
                    level = i;
                    Molded = true;

                    currentInputs = foundInputs.toArray(new pos[0]);
                    currentOutputs = foundOutputs.toArray(new pos[0]);
                    currentEnergyInputs = foundEnergyInputs.toArray(new pos[0]);
                    currentLiquidInputs = foundLiquidInputs.toArray(new pos[0]);
                    currentLiquidOutputs = foundLiquidOutputs.toArray(new pos[0]);
                    break;
                }
            }
            if (!Molded) {
                currentInputs = null;
                currentOutputs = null;
                currentEnergyInputs = null;
                currentLiquidInputs = null;
                currentLiquidOutputs = null;
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
         * 按液体输入仓坐标顺序取出指定液体。
         *
         * @return 实际取出的数量；各输入仓合计不足时可能小于 amount
         */
        private float takeLiquidFromInputs(Liquid liquid, float amount) {
            LevelStruct lvl = currentLevel();
            if (lvl == null || currentLiquidInputs == null) return 0f;

            float remaining = amount;
            for (pos offset : currentLiquidInputs) {
                if (remaining <= 0.001f) break;
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasLiquids) {
                    float canTake = Math.min(t.build.liquids.get(liquid), remaining);
                    if (canTake > 0.001f) {
                        t.build.liquids.remove(liquid, canTake);
                        remaining -= canTake;
                    }
                }
            }
            return amount - remaining;
        }

        /**
         * 按液体输出仓坐标顺序写入产物液体。
         *
         * @return 实际写入数量；总空间不足时可能小于 amount
         */
        private float putLiquidToOutputs(Liquid liquid, float amount) {
            LevelStruct lvl = currentLevel();
            if (lvl == null || currentLiquidOutputs == null) return 0f;

            float remaining = amount;
            for (pos offset : currentLiquidOutputs) {
                if (remaining <= 0.001f) break;
                Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                if (t != null && t.build != null && t.build.block.hasLiquids) {
                    float space = t.build.block.liquidCapacity - t.build.liquids.get(liquid);
                    float canPut = Math.min(space, remaining);
                    if (canPut > 0.001f) {
                        t.build.liquids.add(liquid, canPut);
                        remaining -= canPut;
                    }
                }
            }
            return amount - remaining;
        }

        /**
         * 判断指定并行数所需的全部原料是否已经存在。
         *
         * <p>使用 long 计算“单次用量 × 并行数”，不创建临时配方，也不修改
         * 原配方中的 ItemStack。</p>
         */
        private boolean inputsHaveForParallel(Recipe recipe, int parallelCount) {
            if (parallelCount <= 0) return false;
            if (currentLevel() == null) return false;

            boolean needItems = recipe.inputItems != null && recipe.inputItems.length > 0;
            boolean needLiquids = recipe.inputLiquids != null && recipe.inputLiquids.length > 0;
            if (!needItems && !needLiquids) return true;
            if (needItems && currentInputs == null) return false;
            if (needLiquids && currentLiquidInputs == null) return false;

            if (needItems) {
                Map<Item, Long> needed = new HashMap<>();
                for (ItemStack stack : recipe.inputItems) {
                    long required = (long) stack.amount * parallelCount;
                    needed.merge(stack.item, required, Long::sum);
                }

                Map<Item, Long> available = new HashMap<>();
                for (pos offset : currentInputs) {
                    Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                    if (t == null || t.build == null || !t.build.block.hasItems) continue;

                    for (Item item : needed.keySet()) {
                        available.merge(item, (long) t.build.items.get(item), Long::sum);
                    }
                }

                for (Map.Entry<Item, Long> entry : needed.entrySet()) {
                    if (available.getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                        return false;
                    }
                }
            }

            if (needLiquids) {
                Map<Liquid, Double> needed = new HashMap<>();
                for (LiquidStack stack : recipe.inputLiquids) {
                    double required = (double) stack.amount * parallelCount;
                    needed.merge(stack.liquid, required, Double::sum);
                }

                Map<Liquid, Double> available = new HashMap<>();
                for (pos offset : currentLiquidInputs) {
                    Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                    if (t == null || t.build == null || !t.build.block.hasLiquids) continue;

                    for (Liquid liquid : needed.keySet()) {
                        available.merge(liquid, (double) t.build.liquids.get(liquid), Double::sum);
                    }
                }

                for (Map.Entry<Liquid, Double> entry : needed.entrySet()) {
                    if (available.getOrDefault(entry.getKey(), 0d) + 0.0001 < entry.getValue()) {
                        return false;
                    }
                }
            }
            return true;
        }

        /** 判断所有输出仓是否能容纳指定并行数产生的完整产物。 */
        private boolean outputsHaveSpaceForParallel(Recipe recipe, int parallelCount) {
            if (parallelCount <= 0) return false;
            if (currentLevel() == null) return false;

            boolean needItems = recipe.outputItems != null && recipe.outputItems.length > 0;
            boolean needLiquids = recipe.outputLiquids != null && recipe.outputLiquids.length > 0;
            if (!needItems && !needLiquids) return true;
            if (needItems && currentOutputs == null) return false;
            if (needLiquids && currentLiquidOutputs == null) return false;

            if (needItems) {
                for (ItemStack out : recipe.outputItems) {
                    long requiredSpace = (long) out.amount * parallelCount;
                    long totalSpace = 0L;

                    for (pos offset : currentOutputs) {
                        Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                        if (t != null && t.build != null && t.build.block.hasItems) {
                            totalSpace += Math.max(0, t.build.block.itemCapacity - t.build.items.get(out.item));
                        }
                    }
                    if (totalSpace < requiredSpace) return false;
                }
            }

            if (needLiquids) {
                for (LiquidStack out : recipe.outputLiquids) {
                    double requiredSpace = (double) out.amount * parallelCount;
                    double totalSpace = 0d;

                    for (pos offset : currentLiquidOutputs) {
                        Tile t = Vars.world.tile(tile.x + offset.x, tile.y + offset.y);
                        if (t != null && t.build != null && t.build.block.hasLiquids) {
                            totalSpace += Math.max(
                                    0,
                                    t.build.block.liquidCapacity - t.build.liquids.get(out.liquid)
                            );
                        }
                    }
                    if (totalSpace + 0.0001 < requiredSpace) return false;
                }
            }
            return true;
        }

        /** 判断某条配方能否以指定并行数完整结算。 */
        private boolean canRunParallel(Recipe recipe, int parallelCount) {
            return inputsHaveForParallel(recipe, parallelCount)
                    && outputsHaveSpaceForParallel(recipe, parallelCount);
        }

        /**
         * 在 1 到方块并行上限之间寻找当前可执行的最大并行数。
         *
         * <p>原料需求和输出空间都随并行数单调增加，因此可以使用二分查找。</p>
         */
        private int findMaximumParallel(Recipe recipe) {
            int low = 1;
            int high = Math.max(0, parallel);
            int best = 0;

            while (low <= high) {
                int middle = low + ((high - low) >> 1);
                if (canRunParallel(recipe, middle)) {
                    best = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            return best;
        }

        /** 将单次物品数量安全换算为并行结算数量。 */
        private int parallelAmount(int amount, int parallelCount) {
            long result = (long) amount * parallelCount;
            return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
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

            // 客户端每 tick 读取一次按键，用 keyDown 边沿检测避免快速点击被吞掉。
            handleStructurePreviewInput();

            // 结构检查每秒一次以降低扫描开销；旋转变化时立即重新检查，
            // 使结构判定随核心旋转即时更新。
            if (timer(0, 60f) || rotation != lastCheckedRotation) {
                lastCheckedRotation = rotation;
                boolean wasMolded = Molded;
                int oldLevel = level;
                CheckStruct();
                if (!Molded || level != oldLevel) {
                    progress = 0f;
                    currentRecipe = -1;
                    currentParallel = 0;
                }
            }

            if (!Molded) return;
            if (groups.length == 0) return;
            if (selectedGroup < 0 || selectedGroup >= groups.length) return;

            Recipe[] activeRecipes = groups[selectedGroup].recipes;
            if (activeRecipes.length == 0) return;

            // 当前配方连一并行都无法完成时，重新选择配方。
            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                Recipe active = activeRecipes[currentRecipe];
                if (!canRunParallel(active, 1)) {
                    currentRecipe = -1;
                    currentParallel = 0;
                }
            }

            // 按配方数组顺序选择第一条至少能以一并行执行的配方。
            if (currentRecipe == -1) {
                for (int i = 0; i < activeRecipes.length; i++) {
                    Recipe recipe = activeRecipes[i];
                    int maximum = findMaximumParallel(recipe);
                    if (maximum > 0) {
                        currentRecipe = i;
                        currentParallel = maximum;
                        progress = 0f;
                        break;
                    }
                }
            }

            if (currentRecipe >= 0 && currentRecipe < activeRecipes.length) {
                Recipe active = activeRecipes[currentRecipe];

                /*
                 * 并行数在一个生产周期内保持不变，使该周期的能耗、原料和产物倍率一致。
                 * 若当前并行数因库存或输出空间变化而失效，则重新计算并重启本周期。
                 */
                if (currentParallel <= 0 || !canRunParallel(active, currentParallel)) {
                    int maximum = findMaximumParallel(active);
                    if (maximum <= 0) {
                        currentRecipe = -1;
                        currentParallel = 0;
                        progress = 0f;
                        return;
                    }

                    currentParallel = maximum;
                    progress = 0f;
                }

                float workTicks = delta();

                // 本 tick 能耗 = 单次配方能耗 × 工作比例 × 当前并行数。
                float requiredEnergyJ = active.energyPerCraftJ
                        * workTicks
                        / active.craftTime
                        * currentParallel;

                if (consumeEnergyJ(requiredEnergyJ)) {
                    progress += workTicks / active.craftTime;
                }

                // 完成时再次验证锁定的并行数，避免舱室内容在生产期间被外部改变。
                if (progress >= 1f) {
                    if (!canRunParallel(active, currentParallel)) {
                        progress = 0f;
                        currentParallel = 0;
                        return;
                    }

                    // 原料和产物按本周期锁定的并行数一次性结算。
                    if (active.inputItems != null) {
                        for (ItemStack stack : active.inputItems) {
                            takeFromInputs(
                                    stack.item,
                                    parallelAmount(stack.amount, currentParallel)
                            );
                        }
                    }

                    if (active.inputLiquids != null) {
                        for (LiquidStack stack : active.inputLiquids) {
                            takeLiquidFromInputs(stack.liquid, stack.amount * currentParallel);
                        }
                    }

                    if (active.outputItems != null) {
                        for (ItemStack stack : active.outputItems) {
                            putToOutputs(
                                    stack.item,
                                    parallelAmount(stack.amount, currentParallel)
                            );
                        }
                    }

                    if (active.outputLiquids != null) {
                        for (LiquidStack stack : active.outputLiquids) {
                            putLiquidToOutputs(stack.liquid, stack.amount * currentParallel);
                        }
                    }

                    /*
                     * 一个周期结束后重新选择并行数。新补充的原料或新腾出的输出空间
                     * 可以从下一周期开始提高并行，而不会中途改变当前周期倍率。
                     */
                    progress = 0f;
                    currentParallel = 0;
                }
            } else {
                currentParallel = 0;
                progress = 0f;
            }
        }

        /**
         * 返回鼠标当前是否悬浮在本核心占据的任意方格上。
         */
        private boolean mouseHoveredOverCore() {
            if (Vars.headless || Core.input == null || tile == null) return false;

            Building hovered = Vars.world.buildWorld(
                    Core.input.mouseWorldX(),
                    Core.input.mouseWorldY()
            );
            return hovered == this;
        }

        /**
         * 将预览等级限制到有效范围。
         *
         * @return 没有结构等级时返回 0，否则返回 1 到最大等级之间的值
         */
        private int effectiveStructurePreviewLevel() {
            int maximum = levels == null ? 0 : levels.size();
            if (maximum <= 0) return 0;

            structurePreviewLevel = Math.max(
                    1,
                    Math.min(structurePreviewLevel, maximum)
            );
            return structurePreviewLevel;
        }

        /**
         * 处理结构预览快捷键。
         *
         * <p>按住预览键期间立即显示幽灵方块；单独按下并松开预览键时切换显示状态；
         * 按住预览键期间按加号或减号则调整等级，发生等级调整后松开预览键不会再次
         * 切换显示，从而保留调整后的等级。</p>
         *
         * <p>按键按下与松开采用 keyDown 轮询的边沿检测，而不是仅持续一帧的
         * keyTap / keyRelease，避免快速点击落在逻辑 tick 与渲染帧之间的空隙而丢失。</p>
         */
        private void handleStructurePreviewInput() {
            if (Vars.headless || Core.input == null
                    || !Vars.state.isGame()
                    || Core.scene != null && (Core.scene.hasField() || Core.scene.hasDialog())) {
                previewKeyDownLastFrame = false;
                return;
            }

            boolean keyDown = Core.input.keyDown(structurePreviewKey);
            boolean justPressed = keyDown && !previewKeyDownLastFrame;
            boolean justReleased = !keyDown && previewKeyDownLastFrame;
            previewKeyDownLastFrame = keyDown;

            if (debugPreview && (justPressed || justReleased)) {
                Log.info("[mdtnh-preview] 按键边沿 @:@ 按下=@ 按住=@", tile.x, tile.y, justPressed, keyDown);
            }

            // 在核心上按下预览键：记录按压前可见状态，并立即显示幽灵方块（按住期间可见）。
            if (justPressed && mouseHoveredOverCore()) {
                previewKeyPressActive = true;
                previewLevelAdjustedDuringHold = false;
                previewWasVisibleBeforePress = structurePreviewVisible;
                structurePreviewVisible = true;

                if (debugPreview) {
                    Log.info("[mdtnh-preview] 开始按压 @:@ 按压前可见=@", tile.x, tile.y, previewWasVisibleBeforePress);
                }
            }

            // 按住期间按加号或减号调整等级。
            if (previewKeyPressActive && keyDown) {
                /*
                 * 部分键盘把“+”报告为 plus，部分键盘则报告为 Shift + equals；
                 * 两种形式都接受。
                 */
                boolean increase = Core.input.keyTap(KeyCode.plus)
                        || (Core.input.shift() && Core.input.keyTap(KeyCode.equals));
                boolean decrease = Core.input.keyTap(KeyCode.minus);

                if (increase || decrease) {
                    int maximum = levels == null ? 0 : levels.size();
                    if (maximum > 0) {
                        int delta = increase ? 1 : -1;
                        structurePreviewLevel = Math.max(
                                1,
                                Math.min(structurePreviewLevel + delta, maximum)
                        );
                        structurePreviewVisible = true;

                        if (debugPreview) {
                            Log.info("[mdtnh-preview] 调整等级 @:@ @ -> @/@",
                                    tile.x, tile.y,
                                    delta > 0 ? "+1" : "-1",
                                    structurePreviewLevel, maximum);
                        }
                    } else if (debugPreview) {
                        Log.warn("[mdtnh-preview] 尝试调整等级但未定义任何结构等级 @:@", tile.x, tile.y);
                    }

                    previewLevelAdjustedDuringHold = true;
                }
            }

            // 松开预览键：未调整过等级时按“切换显示”恢复，否则保留调整后的预览。
            if (previewKeyPressActive && justReleased) {
                if (!previewLevelAdjustedDuringHold) {
                    structurePreviewVisible = !previewWasVisibleBeforePress;
                }

                if (debugPreview) {
                    Log.info("[mdtnh-preview] 结束按压 @:@ 可见=@ 曾调级=@",
                            tile.x, tile.y, structurePreviewVisible, previewLevelAdjustedDuringHold);
                }

                previewKeyPressActive = false;
                previewLevelAdjustedDuringHold = false;
            }
        }

        /**
         * 根据指定结构等级生成幽灵方块计划。
         *
         * <p>每个结构槽位使用其 Mapping 匹配列表中的第一个方块作为幽灵方块。
         * 空气槽位与核心自身位置不会绘制。仅当 {@link MultiblockStructer#showMissingOnly}
         * 为 true 时，才会跳过世界中的任意允许方块已经满足的槽位；默认绘制整个结构，
         * 从而在结构完全成形时也能看到完整的结构轮廓。</p>
         */
        private void rebuildStructurePreviewPlans(int previewLevel) {
            structurePreviewPlans.clear();
            if (previewLevel <= 0 || previewLevel > levels.size()) {
                logPreviewDiagnostic("等级无效", previewLevel, 0);
                return;
            }

            LevelStruct definition = levels.get(previewLevel - 1);
            if (definition == null || definition.struct == null || definition.Mapping == null) {
                logPreviewDiagnostic("等级缺少结构定义", previewLevel, 0);
                return;
            }

            for (Map.Entry<pos, Integer> entry : definition.struct.entrySet()) {
                pos offset = entry.getKey();
                Integer mappingIndex = entry.getValue();

                if (offset == null || mappingIndex == null
                        || mappingIndex < 0
                        || mappingIndex >= definition.Mapping.size()) {
                    continue;
                }

                List<Block> candidates = definition.Mapping.get(mappingIndex);
                if (candidates == null || candidates.isEmpty()) continue;

                Block previewBlock = candidates.get(0);
                if (previewBlock == null || previewBlock.isAir()) continue;

                // 核心已经存在，不需要在自身位置再覆盖一层幽灵贴图。
                if (offset.x == 0 && offset.y == 0
                        && previewBlock == MultiblockStructer.this) {
                    continue;
                }

                // 结构定义坐标随核心旋转变换到世界位置。
                pos worldOffset = rotateOffset(offset);
                int planX = tile.x + worldOffset.x;
                int planY = tile.y + worldOffset.y;
                Tile existing = Vars.world.tile(planX, planY);

                // 仅“只显示缺失”模式才跳过已被任意允许方块满足的槽位。
                if (showMissingOnly && slotSatisfied(existing, candidates)) continue;

                BuildPlan plan = new BuildPlan(planX, planY, 0, previewBlock, null);
                plan.worldContext = true;
                plan.animScale = 1f;
                structurePreviewPlans.add(plan);
            }

            logPreviewDiagnostic("生成幽灵方块", previewLevel, structurePreviewPlans.size);
        }

        /**
         * 判断某槽位当前是否已经被任意一个允许方块满足。
         */
        private boolean slotSatisfied(Tile existing, List<Block> candidates) {
            if (existing == null || existing.block() == null) return false;
            for (Block allowed : candidates) {
                if (existing.block() == allowed) return true;
            }
            return false;
        }

        /**
         * 输出预览绘制侧诊断日志。
         *
         * <p>仅在可见状态、等级、旋转与计划数构成的签名发生变化时输出一次，
         * 避免预览持续可见时每帧刷屏。</p>
         */
        private void logPreviewDiagnostic(String message, int level, int count) {
            if (!debugPreview) return;

            int rot = rotation & 3;
            int signature = (structurePreviewVisible ? 1 : 0) * 100000 + level * 1000 + rot * 100 + count;
            if (signature == lastLoggedPreviewSignature) return;
            lastLoggedPreviewSignature = signature;

            Log.info("[mdtnh-preview] @ @:@ 可见=@ 等级=@ 旋转=@ 计划数=@",
                    message,
                    tile == null ? -1 : tile.x,
                    tile == null ? -1 : tile.y,
                    structurePreviewVisible,
                    level,
                    rot,
                    count);
        }

        /**
         * 绘制当前等级的整个结构（或仅缺失槽位，取决于 {@link #showMissingOnly}）。
         *
         * <p>使用各方块自己的 drawPlan，因此带有专用 drawer 的方块也能显示正确
         * 预览贴图；drawPlan 内部会叠加原版一致的白色脉冲混合色与半透明效果，
         * 并保留方块本身的颜色，便于辨认该位置需要放置哪种方块。幽灵方块绘制在
         * 原版放置预览图层之上，因此即使结构已经成形也会叠加显示完整轮廓。</p>
         */
        private void drawStructurePreview() {
            // 在任何早退之前记录一次进入状态：即使 visible 为 false 也输出，便于定位“按了键却没绘制”。
            logPreviewEntry();

            if (!structurePreviewVisible || tile == null) return;

            int previewLevel = effectiveStructurePreviewLevel();
            if (previewLevel <= 0) {
                logPreviewDiagnostic("预览已开启但有效等级为 0", previewLevel, 0);
                return;
            }

            rebuildStructurePreviewPlans(previewLevel);

            // 计划数为 0 时也记录一次，便于区分“结构完整无缺失”与“绘制失败”。
            logPreviewDiagnostic("绘制幽灵方块", previewLevel, structurePreviewPlans.size);

            float alpha = Math.max(0f, Math.min(1f, previewAlpha));

            // 幽灵方块绘制在原版放置预览的图层，保证叠加在已放置的方块之上。
            try {
                float previousZ = Draw.z();
                Draw.z(Layer.plans);
                for (BuildPlan plan : structurePreviewPlans) {
                    if (plan == null || plan.block == null) continue;

                    // 有效位置、统一透明度的原版风格幽灵方块。
                    plan.block.drawPlan(plan, structurePreviewPlans, true, alpha);
                }
                Draw.z(previousZ);
            } catch (Throwable t) {
                Log.err("[mdtnh-preview] 绘制幽灵方块时发生异常 @:@", tile.x, tile.y);
                Log.err(t);
            }

            Draw.reset();

            // 鼠标仍在核心上时显示当前有效预览等级，便于确认加减键调整结果。
            if (mouseHoveredOverCore()) {
                Drawf.text(
                        Core.bundle.get("mdtnh.multiblock-preview-level", "结构等级")
                                + " " + previewLevel + " / " + levels.size(),
                        x,
                        y + block.size * Vars.tilesize / 2f + 10f,
                        Pal.accent,
                        0.75f
                );
            }
        }

        /**
         * 记录进入幽灵方块绘制时点的状态，位于所有早退判断之前。
         *
         * <p>仅在可见状态与等级构成的签名变化时输出一次，避免每帧刷屏。</p>
         */
        private void logPreviewEntry() {
            if (!debugPreview || tile == null) return;

            int level = effectiveStructurePreviewLevel();
            int rot = rotation & 3;
            int signature = (structurePreviewVisible ? 1 : 0) * 10000 + level * 100 + rot;
            if (signature == lastPreviewEntrySignature) return;
            lastPreviewEntrySignature = signature;

            Log.info("[mdtnh-preview] 进入绘制 @:@ 可见=@ 等级=@ 旋转=@ 计划数=@",
                    tile.x, tile.y, structurePreviewVisible, level, rot, structurePreviewPlans.size);
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
                    if (first.outputItems != null && first.outputItems.length > 0) {
                        icon = first.outputItems[0].item.uiIcon;
                    } else if (first.outputLiquids != null && first.outputLiquids.length > 0) {
                        icon = first.outputLiquids[0].liquid.uiIcon;
                    }
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

            // 预览不在此处绘制：v159 中 Building.draw() 不一定被调用，
            // 统一由外层的 Trigger.postDraw 全局钩子在所有实体绘制完成后绘制。
        }

        /** 将核心绘制器提供的光照效果交给 Mindustry 渲染。 */
        @Override
        public void drawLight() {
            if (drawer != null) drawer.drawLight(this);
        }
    }
}