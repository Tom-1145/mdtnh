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

    public final int capacityJ;

    VoltageTier(String displayName, String contentName, float minVoltageV, float maxVoltageV,int capacityJ) {
        this.displayName = displayName;
        this.contentName = contentName;
        this.minVoltageV = minVoltageV;
        this.maxVoltageV = maxVoltageV;
        this.capacityJ=capacityJ;
    }

    /** @return 当前等级是否能够执行要求 minimumTier 或更低等级的配方。 */
    public boolean canProcess(VoltageTier minimumTier) {
        return minimumTier != null && ordinal() >= minimumTier.ordinal();
    }

    /** @return 当前等级比最低要求高出的级数。 */
    public int stepsAbove(VoltageTier minimumTier) {
        if (!canProcess(minimumTier)) return -1;
        return ordinal() - minimumTier.ordinal();
    }

    /**
     * 高出一级速度翻倍，因此配方时间除以 2；高出 n 级时除以 2^n。
     */
    public float speedMultiplierFrom(VoltageTier minimumTier) {
        int steps = stepsAbove(minimumTier);
        return steps < 0 ? 0f : (float)Math.pow(2d, steps);
    }

    /**
     * 高出一级单次总能耗翻倍；结合时间减半与电压翻 4 倍，平均电流保持不变。
     */
    public float energyMultiplierFrom(VoltageTier minimumTier) {
        return speedMultiplierFrom(minimumTier);
    }
}
