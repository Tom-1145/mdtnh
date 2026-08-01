package mdtnh;

public enum FactoryTire {
    // 举个例子，不一定正确
    BASIC(1.0f, 1.0f, 2, 120),      // 基础：速度1倍，耗电1倍，尺寸2，生命120 LV
    ADVANCED(1.5f, 1.2f, 2, 180),    // 进阶：速度1.5倍，耗电1.2倍  MV
    EXPERT(2.0f, 1.5f, 3, 250);      // 高级：速度2倍，耗电1.5倍，尺寸3  HV

    public final float craftTimeMultiplier;   // 速度倍率（值越小越快）
    public final float powerMultiplier;       // 耗电倍率
    public final int size;                    // 方块尺寸
    public final int health;                  // 生命值

    FactoryTire(float craftTimeMultiplier, float powerMultiplier, int size, int health) {
        this.craftTimeMultiplier = craftTimeMultiplier;
        this.powerMultiplier = powerMultiplier;
        this.size = size;
        this.health = health;
    }
}