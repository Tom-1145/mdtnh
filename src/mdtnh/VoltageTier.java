package mdtnh;

/**
 * MDT 离散电力系统使用的 15 个电压等级。
 *
 * <p>除 ULV 的下限为 0V 外，每一级的下限等于前一级上限，
 * 上限为前一级上限的 4 倍。区间边界按“低于下限欠压、
 * 高于上限过压、等于边界可接受”处理。</p>
 */
public enum VoltageTier {
    ULV("ULV", "ulv", 0f, 2f,32),
    LV("LV", "lv", 2f, 8f,128),
    MV("MV", "mv", 8f, 32f,512),
    HV("HV", "hv", 32f, 128f,2048),
    EV("EV", "ev", 128f, 512f,8192),
    IV("IV", "iv", 512f, 2048f,32768),
    LUV("LuV", "luv", 2048f, 8192f,131072),
    ZMP("ZMP", "zmp", 8192f, 32768f,524288),
    UV("UV", "uv", 32768f, 131072f,2097152),
    UHV("UHV", "uhv", 131072f, 524288f,8388608),
    UEV("UEV", "uev", 524288f, 2097152f,33554432),
    UIV("UIV", "uiv", 2097152f, 8388608f,134217728),
    UMV("UMV", "umv", 8388608f, 33554432f,2147483647),
    UXV("UXV", "uxv", 33554432f, 134217728f,2147483647),
    MAX("MAX", "max", 134217728f, 536870912f,2147483647);

    /** 面板和本地化中使用的等级名称。 */
    public final String displayName;

    /** 方块内部名称中使用的小写标识。 */
    public final String contentName;

    /** 可正常接收的最低输入电压，单位 V。 */
    public final float minVoltageV;

    /** 可正常接收的最高输入电压，同时作为倍率计算的标称电压，单位 V。 */
    public final float maxVoltageV;

    /**
     * 该等级机器的基础能源缓存容量，单位 J。
     *
     * <p>注册配方后，注册器仍可能根据实际配方功率把机器缓存扩充到更大值。</p>
     */
    public final int capacityJ;

    /**
     * 定义一个离散电压等级。
     *
     * @param displayName 面向玩家的等级名称
     * @param contentName 方块内部名称使用的标识
     * @param minVoltageV 可接受最低输入电压
     * @param maxVoltageV 可接受最高输入电压，同时作为倍率计算的标称电压
     * @param capacityJ   基础能源缓存容量
     */
    VoltageTier(String displayName, String contentName, float minVoltageV, float maxVoltageV,int capacityJ) {
        this.displayName = displayName;
        this.contentName = contentName;
        this.minVoltageV = minVoltageV;
        this.maxVoltageV = maxVoltageV;
        this.capacityJ=capacityJ;
    }

    /**
     * 判断当前等级是否满足配方最低等级要求。
     *
     * @param minimumTier 配方要求的最低电压等级
     * @return 当前等级不低于最低要求时返回 {@code true}
     */
    public boolean canProcess(VoltageTier minimumTier) {
        return minimumTier != null && ordinal() >= minimumTier.ordinal();
    }

    /**
     * 计算当前等级比配方最低等级高出的级数。
     *
     * @return 可执行时返回非负级差；等级不足时返回 {@code -1}
     */
    public int stepsAbove(VoltageTier minimumTier) {
        if (!canProcess(minimumTier)) return -1;
        return ordinal() - minimumTier.ordinal();
    }

    /**
     * 计算等级提升带来的速度倍率。
     *
     * <p>每高一级速度翻倍，因此高出 {@code n} 级时倍率为 {@code 2^n}；
     * 实际耗时等于基准耗时除以该倍率。</p>
     */
    public float speedMultiplierFrom(VoltageTier minimumTier) {
        int steps = stepsAbove(minimumTier);
        return steps < 0 ? 0f : (float)Math.pow(2d, steps);
    }

    /**
     * 计算等级提升带来的单次总能耗倍率。
     *
     * <p>当前规则中能耗倍率与速度倍率相同：每高一级总能耗翻倍。
     * 配合耗时减半和标称电压提升四倍，平均电流保持稳定。</p>
     */
    public float energyMultiplierFrom(VoltageTier minimumTier) {
        return speedMultiplierFrom(minimumTier);
    }
}
