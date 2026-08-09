# MDT 15 级电压配方注册系统

## 生成的机器

创建：

```java
VoltageRecipeRegistry machines = new VoltageRecipeRegistry("example-processor");
```

会注册 17 个 2x2 方块：

- 15 台电力机器：`example-processor-ulv` 到 `example-processor-max`；
- 1 台 ULV 蒸汽机：`example-processor-ulv-steam`；
- 1 台 ULV 手动机：`example-processor-ulv-manual`。

电力机器的输入电压区间为：

| 等级 | 输入电压 |
|---|---:|
| ULV | 0~2V |
| LV | 2~8V |
| MV | 8~32V |
| HV | 32~128V |
| EV | 128~512V |
| IV | 512~2048V |
| LuV | 2048~8192V |
| ZMP | 8192~32768V |
| UV | 32768~131072V |
| UHV | 131072~524288V |
| UEV | 524288~2097152V |
| UIV | 2097152~8388608V |
| UMV | 8388608~33554432V |
| UXV | 33554432~134217728V |
| MAX | 134217728~536870912V |

边界沿用当前能源系统规则：低于最低电压时忽略能量包，高于最高电压时摧毁机器，等于边界时可接收。

## 注册配方

最简写法：

```java
public static VoltageRecipeRegistry processors;

public static void load() {
    processors = new VoltageRecipeRegistry("example-processor");

    processors.register(
        "metal",
        VoltageTier.LV,
        RecipeCrafter.Recipe.items(
            ItemStack.with(Items.copper, 2),
            new ItemStack(Items.lead, 1),
            120f                 // 在 LV 上执行需要 120 tick
        ).energy(240f)           // 在 LV 上执行总耗能 240J
    );
}
```

也可以把最低等级耗时和能耗作为独立参数传入：

```java
processors.register(
    "metal",
    VoltageTier.LV,
    RecipeCrafter.Recipe.items(
        ItemStack.with(Items.copper, 2),
        new ItemStack(Items.lead, 1),
        1f                       // 此处会被重载参数替换
    ),
    240f,                        // LV 总能耗
    120f                         // LV 耗时
);
```

对上述 LV 配方，自动生成：

```text
LV  : 120 tick, 240J
MV  :  60 tick, 480J
HV  :  30 tick, 960J
EV  :  15 tick, 1920J
...
```

低于 LV 的 ULV 电力机、蒸汽机和手动机都不会获得这条配方。

若最低等级为 ULV：

- ULV 蒸汽机获得相同耗时、相同内部焦耳消耗的版本；
- ULV 手动机获得耗时 x4、能耗为 0 的版本；
- ULV 到 MAX 的全部电力机器获得对应超频版本。

## 恒电流关系

注册器用每级电压上限作为标称电压：

```text
I = E / (t秒 * V)
```

每升一级：

```text
电压 x4
耗时 /2
单次能耗 x2
功率 x4
平均电流不变
```

机器的 `maxInputA` 会根据已注册配方的最大平均电流自动上调并向上取整。内部缓存容量会至少扩展到“该配方满速运行一秒”所需能量，以适配当前按秒输送电流包的能源系统。

## 蒸汽机

蒸汽机仍使用 `SteamRecipeCrafter` 的蒸汽转焦耳机制。注册 ULV 配方时，注册器会自动提高：

```java
ulvSteamMachine.maxSteamUsePerSecond
```

使默认转换效率下的蒸汽吞吐量足以覆盖已注册 ULV 配方的最大功率。可以在注册配方前调整：

```java
processors.ulvSteamMachine.joulesPerSteamUnit = 120f;
```

## 自定义机器

注册器创建后，可以通过下列字段继续配置方块需求、容量或绘制器：

```java
for (VoltageTier tier : VoltageTier.values()) {
    RecipeCrafter machine = processors.machine(tier);
    machine.itemCapacity = 60;
    machine.liquidCapacity = 40f;
}

processors.ulvSteamMachine.itemCapacity = 60;
processors.ulvManualMachine.itemCapacity = 60;
```

机器必须在模组内容加载阶段创建并注册配方，不要在游戏已经进入地图后再新增内容。

## 生产循环修改

高等级配方的耗时可能低于 1 tick。修改后的 `RecipeCrafter` 会把一个 tick 的工作量拆成若干段，并允许在同一 tick 内完成多次生产；每次完成前都会重新检查材料、输出空间和能源。否则原实现每 tick 只结算一次，高等级速度会被错误限制。
