package mdtnh.energy;

import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.math.Mathf;

/**
 * 保存单个能源建筑实例的可变状态。
 *
 * <p>{@link EnergySpec} 描述同类方块共享的固定参数，本类则保存每个已放置建筑
 * 各自的能量和上一模拟秒的电流统计。</p>
 */
public class EnergyState {

    /** 当前储存的可用能量，单位为焦耳。 */
    public float energyJ;

    /**
     * 上一个模拟秒接收的电流包数量。
     *
     * <p>每个包代表 1A，因此该值也可直接作为上一秒输入电流的安培数显示。</p>
     */
    public int inputA;

    /** 上一个模拟秒发送的电流包数量，单位等价于安培。 */
    public int outputA;

    /**
     * 上一个模拟秒经过导线节点的电流包数量。
     *
     * <p>该字段主要供导线使用；非导线节点通常保持为 0。</p>
     */
    public int currentA;

    /**
     * 上一个模拟秒最后一个到达该节点的电流包电压。
     *
     * <p>该值只用于状态显示和调试，不参与存档。</p>
     */
    public float lastInputVoltageV;

    /**
     * 上一个模拟秒因输入电压过低或容量不足而被丢弃的电流包数量。
     *
     * <p>包虽然没有进入缓存，但仍会计入 {@link #inputA}、来源输出和导线电流。</p>
     */
    public int ignoredInputA;

    /**
     * 上一个模拟秒触发过压摧毁的电流包数量。
     *
     * <p>通常最多为 1，因为第一个过压包就会摧毁接收建筑。</p>
     */
    public int overvoltageA;

    /**
     * 判断缓存能否一次性支付指定能量。
     *
     * @param amountJ 需要支付的能量，单位为焦耳
     * @return 请求量非正数或当前能量足够时返回 {@code true}
     */
    public boolean has(float amountJ) {
        // 微小容差用于避免浮点累计误差导致“理论上刚好足够”却判定失败。
        return amountJ <= 0f || energyJ + 0.0001f >= amountJ;
    }

    /**
     * 从缓存中全额扣除指定能量。
     *
     * <p>能量不足时不会进行部分扣除。</p>
     *
     * @param amountJ 需要消耗的能量，单位为焦耳
     * @return 成功扣除或请求量非正数时返回 {@code true}；能量不足时返回 {@code false}
     */
    public boolean consume(float amountJ) {
        if (amountJ <= 0f) return true;
        if (!has(amountJ)) return false;

        energyJ -= amountJ;
        return true;
    }

    /**
     * 向缓存加入能量，并自动受容量上限约束。
     *
     * @param amountJ 尝试加入的能量，单位为焦耳
     * @param spec 当前节点的固定能源规格
     * @return 实际被缓存接受的能量
     */
    public float add(float amountJ, EnergySpec spec) {
        if (amountJ <= 0f) return 0f;

        float accepted = Math.min(amountJ, spec.capacityJ - energyJ);
        energyJ += accepted;
        return accepted;
    }

    /**
     * 计算当前能量占容量的比例，结果限制在 0 到 1。
     *
     * @param spec 当前节点的固定能源规格
     * @return 荷电比例；容量不大于 0 时返回 0
     */
    public float fraction(EnergySpec spec) {
        return spec.capacityJ <= 0f
                ? 0f
                : Mathf.clamp(energyJ / spec.capacityJ);
    }

    /**
     * 将需要持久化的状态写入 Mindustry 存档流。
     *
     * <p>电流统计每秒都会重新计算，因此只保存当前能量。</p>
     */
    public void write(Writes write) {
        write.f(energyJ);
    }

    /**
     * 从存档读取能量，并限制在当前规格允许的容量范围内。
     *
     * @param read 存档读取器
     * @param spec 当前节点的能源规格
     */
    public void read(Reads read, EnergySpec spec) {
        energyJ = Mathf.clamp(read.f(), 0f, spec.capacityJ);
    }
}
