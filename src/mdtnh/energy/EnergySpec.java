package mdtnh.energy;

public class EnergySpec {
    public enum Role {
        generator,
        consumer,
        battery,
        wire
    }

    public Role role = Role.consumer;

    /** 额定电压，也是最大安全输入电压。 */
    public float voltageV = 12f;

    /** 内部能量缓存容量。 */
    public float capacityJ = 120f;

    /** 每模拟秒最大输入电流。 */
    public int maxInputA = 1;

    /** 每模拟秒最大输出电流。 */
    public int maxOutputA = 0;

    /** 仅导线使用。 */
    public int maxWireCurrentA = 0;

    /** 每个 1A 包通过本导线格时损失的电压。 */
    public float wireLossV = 0f;

    public boolean isWire() {
        return role == Role.wire;
    }
}
