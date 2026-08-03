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
     * 节点额定电压，单位为伏特。
     *
     * <p>发送一个持续一秒的 1A 离散电流包时，来源会支出
     * {@code voltageV} 焦耳。对接收节点而言，该值同时作为最大安全输入电压。</p>
     */
    public float voltageV = 12f;

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
}
