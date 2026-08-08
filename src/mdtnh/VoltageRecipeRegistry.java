package mdtnh;

import java.util.*;

/**
 * 为一整套电压等级机器注册并派生配方。
 *
 * <p>创建一个注册器会生成 15 台 2x2 电力机器，并额外生成 ULV 蒸汽机和
 * ULV 手动机。注册一条配方时，传入配方的 craftTime 与 energyPerCraftJ
 * 被视为它在最低电压等级运行时的基准值。</p>
 */
public class VoltageRecipeRegistry {
    /** 15 个电压等级各自对应的电力机器。 */
    public final EnumMap<VoltageTier, RecipeCrafter> electricMachines =
            new EnumMap<>(VoltageTier.class);

    /** 只能执行 ULV 配方、以蒸汽给内部缓存充能的 2x2 机器。 */
    public final SteamRecipeCrafter ulvSteamMachine;

    /** 只能执行 ULV 配方、零能耗且耗时为基准 4 倍的 2x2 手动机器。 */
    public final ManualRecipeCrafter ulvManualMachine;

    /** 所有生成方块内部名称的前缀。 */
    public final String contentPrefix;

    /**
     * 创建完整机器族。
     *
     * @param contentPrefix 方块内部名称前缀，例如传入 alloy-smelter 后会生成
     *                      alloy-smelter-ulv、alloy-smelter-lv 等方块
     */
    public VoltageRecipeRegistry(String contentPrefix) {
        if (contentPrefix == null || contentPrefix.isEmpty()) {
            throw new IllegalArgumentException("contentPrefix 不能为空");
        }
        this.contentPrefix = contentPrefix;

        for (VoltageTier tier : VoltageTier.values()) {
            RecipeCrafter machine = new RecipeCrafter(contentPrefix + "-" + tier.contentName);
            configureElectricMachine(machine, tier);
            electricMachines.put(tier, machine);
        }

        ulvSteamMachine = new SteamRecipeCrafter(contentPrefix + "-ulv-steam");
        configureSpecialMachine(ulvSteamMachine, VoltageTier.ULV);

        ulvManualMachine = new ManualRecipeCrafter(contentPrefix + "-ulv-manual");
        configureSpecialMachine(ulvManualMachine, VoltageTier.ULV);
    }

    /** 配置一台 2x2 电力机器的输入电压区间。 */
    protected void configureElectricMachine(RecipeCrafter machine, VoltageTier tier) {
        machine.size = 2;
        machine.groups = new RecipeCrafter.RecipeGroup[]{};

        machine.energySpec.voltageV = tier.maxVoltageV;
        machine.energySpec.minInputVoltageV = tier.minVoltageV;
        machine.energySpec.maxInputVoltageV = tier.maxVoltageV;
        machine.energySpec.capacityJ = tier.capacityJ;
        machine.energySpec.maxInputA = 1;
        machine.energySpec.maxOutputA = 0;
    }

    /** 配置 ULV 蒸汽或手动变体的共同尺寸与配方组初始值。 */
    protected void configureSpecialMachine(RecipeCrafter machine, VoltageTier tier) {
        machine.size = 2;
        machine.groups = new RecipeCrafter.RecipeGroup[]{};
        machine.energySpec.voltageV = tier.maxVoltageV;
        machine.energySpec.minInputVoltageV = tier.minVoltageV;
        machine.energySpec.maxInputVoltageV = tier.maxVoltageV;
        if (!machine.usesManualEnergy()) {
            machine.energySpec.capacityJ = 0f;
        }
    }

    /** @return 指定电压等级的电力机器。 */
    public RecipeCrafter machine(VoltageTier tier) {
        return electricMachines.get(tier);
    }

    /**
     * 注册一条分级配方。
     *
     * <p>recipeAtMinimum.craftTime 和 recipeAtMinimum.energyPerCraftJ 分别表示
     * 在 minimumTier 上执行时的耗时与总能耗。高一级机器的耗时减半、
     * 单次能耗翻倍；低于 minimumTier 的机器不获得该配方。</p>
     *
     * <p>当 minimumTier 为 ULV 时，还会生成：</p>
     * <ul>
     *     <li>蒸汽版：耗时和能耗与 ULV 电力版相同；</li>
     *     <li>手动版：耗时为 ULV 的 4 倍，能耗为 0。</li>
     * </ul>
     */
    public void register(String groupName, VoltageTier minimumTier,
                         RecipeCrafter.Recipe recipeAtMinimum) {
        if (minimumTier == null) throw new IllegalArgumentException("minimumTier 不能为空");
        if (recipeAtMinimum == null) throw new IllegalArgumentException("recipeAtMinimum 不能为空");
        if (recipeAtMinimum.craftTime <= 0f) throw new IllegalArgumentException("配方耗时必须大于 0 tick");
        if (recipeAtMinimum.energyPerCraftJ < 0f) throw new IllegalArgumentException("配方能耗不能为负数");

        String actualGroup = groupName == null || groupName.isEmpty() ? "default" : groupName;

        for (VoltageTier machineTier : VoltageTier.values()) {
            if (!machineTier.canProcess(minimumTier)) continue;

            float multiplier = machineTier.speedMultiplierFrom(minimumTier);
            float derivedTime = recipeAtMinimum.craftTime / multiplier;
            float derivedEnergy = recipeAtMinimum.energyPerCraftJ
                    * machineTier.energyMultiplierFrom(minimumTier);

            RecipeCrafter.Recipe derived = recipeAtMinimum.copyWith(derivedTime, derivedEnergy);
            derived.minimumVoltageTier = minimumTier;
            derived.executionVoltageTier = machineTier;

            RecipeCrafter machine = electricMachines.get(machineTier);
            addRecipe(machine, actualGroup, derived);
            updateElectricLimits(machine, machineTier, derived);
        }

        if (minimumTier == VoltageTier.ULV) {
            RecipeCrafter.Recipe steam = recipeAtMinimum.copyWith(
                    recipeAtMinimum.craftTime,
                    recipeAtMinimum.energyPerCraftJ
            );
            steam.minimumVoltageTier = VoltageTier.ULV;
            steam.executionVoltageTier = VoltageTier.ULV;
            addRecipe(ulvSteamMachine, actualGroup, steam);
            updateBufferCapacity(ulvSteamMachine, steam);
            updateSteamThroughput(ulvSteamMachine, steam);

            RecipeCrafter.Recipe manual = recipeAtMinimum.copyWith(
                    recipeAtMinimum.craftTime * 4f,
                    0f
            );
            manual.minimumVoltageTier = VoltageTier.ULV;
            manual.executionVoltageTier = VoltageTier.ULV;
            addRecipe(ulvManualMachine, actualGroup, manual);
        }
    }

    /**
     * 显式传入最低等级耗时与能耗的便捷重载。
     */
    public void register(String groupName, VoltageTier minimumTier,
                         RecipeCrafter.Recipe recipeDefinition,
                         float energyAtMinimumJ, float timeAtMinimumTicks) {
        if (recipeDefinition == null) throw new IllegalArgumentException("recipeDefinition 不能为空");
        RecipeCrafter.Recipe base = recipeDefinition.copyWith(timeAtMinimumTicks, energyAtMinimumJ);
        register(groupName, minimumTier, base);
    }

    /** 向指定机器的同名配方组追加一条独立配方。 */
    protected void addRecipe(RecipeCrafter machine, String groupName,
                             RecipeCrafter.Recipe recipe) {
        RecipeCrafter.RecipeGroup group = findGroup(machine, groupName);
        if (group == null) {
            group = new RecipeCrafter.RecipeGroup(groupName, new RecipeCrafter.Recipe[]{});
            RecipeCrafter.RecipeGroup[] old = machine.groups;
            machine.groups = Arrays.copyOf(old, old.length + 1);
            machine.groups[old.length] = group;
        }
        group.addRecipe(recipe);
    }

    protected RecipeCrafter.RecipeGroup findGroup(RecipeCrafter machine, String groupName) {
        for (RecipeCrafter.RecipeGroup group : machine.groups) {
            if (Objects.equals(group.name, groupName)) return group;
        }
        return null;
    }

    /**
     * 按该配方的平均功率自动扩充机器输入电流上限和一秒能源缓存。
     *
     * <p>平均电流使用等级上限电压作为标称电压：
     * I = E / (t秒 * V)。由于每升一级 E x2、t /2、V x4，计算出的 I 不变。</p>
     */
    protected void updateElectricLimits(RecipeCrafter machine, VoltageTier tier,
                                        RecipeCrafter.Recipe recipe) {
        float seconds = recipe.craftTime / 60f;
        if (seconds > 0f && tier.maxVoltageV > 0f) {
            float amperage = recipe.energyPerCraftJ / (seconds * tier.maxVoltageV);
            int requiredInputA = Math.max(1, (int)Math.ceil(amperage - 0.000001f));
            machine.energySpec.maxInputA = Math.max(machine.energySpec.maxInputA, requiredInputA);
        }
        updateBufferCapacity(machine, recipe);
    }

    /**
     * 能源网络按秒发送电流包，因此缓存至少容纳该配方满速运行一秒所需的能量。
     */
    protected void updateBufferCapacity(RecipeCrafter machine, RecipeCrafter.Recipe recipe) {
        float energyPerSecond = recipe.craftTime <= 0f
                ? recipe.energyPerCraftJ
                : recipe.energyPerCraftJ * 60f / recipe.craftTime;
        float requiredCapacity = Math.max(recipe.energyPerCraftJ, energyPerSecond);
        machine.energySpec.capacityJ = Math.max(machine.energySpec.capacityJ, requiredCapacity);
    }
    /** 让 ULV 蒸汽机的最大蒸汽吞吐量至少能够支撑已注册配方的满速功率。 */
    protected void updateSteamThroughput(SteamRecipeCrafter machine,
                                         RecipeCrafter.Recipe recipe) {
        if (machine.joulesPerSteamUnit <= 0f || recipe.craftTime <= 0f) return;
        float energyPerSecond = recipe.energyPerCraftJ * 60f / recipe.craftTime;
        float requiredSteamPerSecond = energyPerSecond / machine.joulesPerSteamUnit;
        machine.maxSteamUsePerSecond = Math.max(
                machine.maxSteamUsePerSecond,
                requiredSteamPerSecond
        );
    }

}
