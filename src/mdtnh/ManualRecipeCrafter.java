package mdtnh;

/**
 * 不连接电网、不消耗蒸汽，只执行零能耗配方的手动多配方机器。
 *
 * <p>注册器只会向该机器加入 ULV 配方的手动版本，并把耗时设为
 * ULV 基准耗时的 4 倍、能耗设为 0。</p>
 */
public class ManualRecipeCrafter extends RecipeCrafter {

    public ManualRecipeCrafter(String name) {
        super(name);
        energySource = EnergySource.manual;

        energySpec.voltageV = 0f;
        energySpec.minInputVoltageV = 0f;
        energySpec.maxInputVoltageV = 0f;
        energySpec.capacityJ = 0f;
        energySpec.maxInputA = 0;
        energySpec.maxOutputA = 0;
    }
}
