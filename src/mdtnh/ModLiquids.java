package mdtnh;

import arc.graphics.Color;
import mindustry.type.Liquid;

/** 注册模组使用的自定义液体与气体。 */
public class ModLiquids {

    /** 锅炉、蒸汽工厂和蒸汽能源仓使用的气态蒸汽。 */
    public static Liquid steam;

    public static void load() {
        steam = new Liquid("steam", Color.lightGray) {{
            gas = true;
            temperature = 0.9f;
            viscosity = 0.2f;
        }};
    }
}
