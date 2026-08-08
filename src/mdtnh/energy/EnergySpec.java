package mdtnh.energy;

/**
 * 描述一个能源节点在网络中的固定参数。
 *
 * <p>该对象属于方块级配置：同一种方块的所有建筑实例共享同一套规格。
 * 运行时不断变化的储能量和电流统计由 {@link EnergyState} 保存。</p>
 */
public class EnergySpec {

    /**
     * 节点在自动调度中的角色。
     *
     * <ul>
     *     <li>{@code generator}：能够向网络输出能量的发电节点。</li>
     *     <li>{@code consumer}：只接收能量的负载节点。</li>
     *     <li>{@code battery}：既可接收也可输出能量的储能节点。</li>
     *     <li>{@code wire}：只负责连接和传输，不保存可用能量。</li>
     * </ul>
     */
    public enum Role {
        generator,
        consumer,
        battery,
        wire
    }

    /** 节点参与网络调度时采用的角色。 */
    public Role role = Role.consumer;

    /**
     * 节点发送电流包时使用的输出电压，单位为伏特。
     *
     * <p>一个电流包表示 1A 持续一秒，因此发送一个包会从来源缓存扣除
     * {@code voltageV} 焦耳。包经过导线后的到达电压等于该值减去路径总压降。</p>
     */
    public float voltageV = 12f;

    /**
     * 能够被节点正常接受的最低输入电压，单位为伏特。
     *
     * <p>到达电压严格小于该值时，电流包已经通过导线并消耗来源能量，
     * 但接收端不会把它写入能源缓存。</p>
     */
    public float minInputVoltageV = 10f;

    /**
     * 能够被节点正常接受的最高输入电压，单位为伏特。
     *
     * <p>到达电压严格大于该值时，接收建筑会被摧毁。该判断发生在包已经完成
     * 线路传输之后，因此来源能量和导线载流量不会回退。</p>
     */
    public float maxInputVoltageV = 14f;

    /**
     * 节点内部能够保存的最大能量，单位为焦耳。
     *
     * <p>导线通常将该值设为 0，因为导线不承担储能职责。</p>
     */
    public float capacityJ = 120f;

    /**
     * 每个模拟秒允许接收的最大离散电流包数量。
     *
     * <p>每个电流包代表 1A，因此该整数同时表示最大输入电流，单位为安培。</p>
     */
    public int maxInputA = 1;

    /**
     * 每个模拟秒允许发送的最大离散电流包数量。
     *
     * <p>仅发电机和电池通常需要大于 0 的输出上限。</p>
     */
    public int maxOutputA = 0;

    /**
     * 导线每个模拟秒允许通过的最大电流包数量。
     *
     * <p>该字段只对 {@link Role#wire} 生效，其他角色通常保持为 0。</p>
     */
    public int maxWireCurrentA = 0;

    /**
     * 一个 1A 电流包经过该导线格时产生的电压损失，单位为伏特。
     *
     * <p>路径总线损等于沿途所有导线格的该值之和。</p>
     */
    public float wireLossV = 0f;

    /** @return 当前规格是否表示导线节点。 */
    public boolean isWire() {
        return role == Role.wire;
    }

    /**
     * 判断到达电压是否低于正常工作区间。
     *
     * @param inputVoltageV 到达节点的电压
     */
    public boolean isUndervoltage(float inputVoltageV) {
        return inputVoltageV < minInputVoltageV;
    }

    /**
     * 判断到达电压是否高于正常工作区间。
     *
     * @param inputVoltageV 到达节点的电压
     */
    public boolean isOvervoltage(float inputVoltageV) {
        return inputVoltageV > maxInputVoltageV;
    }
}
