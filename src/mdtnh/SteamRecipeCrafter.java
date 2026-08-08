package mdtnh;

/**
 * 使用蒸汽而不是外部电线为内部能源缓存充能的多配方工厂。
 *
 * <p>生产、配方分组、物品与液体处理全部复用 {@link RecipeCrafter}；本类只设置
 * 蒸汽能源模式。运行时建筑仍保存 {@code EnergyState}，但会拒绝加入 MDT 电网。</p>
 */
public class SteamRecipeCrafter extends RecipeCrafter {

    public SteamRecipeCrafter(String name) {
        super(name);
        energySource = EnergySource.steam;
        steamLiquid = ModLiquids.steam;
        hasLiquids = true;

        // 蒸汽机器不从导线网络取得电流包。
        energySpec.maxInputA = 0;
        energySpec.maxOutputA = 0;
    }
}
