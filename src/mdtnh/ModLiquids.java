package mdtnh;

import arc.graphics.Color;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.type.Liquid;

/** 注册模组使用的自定义液体与气体。 */
public class ModLiquids {

    /** 锅炉、蒸汽工厂和蒸汽能源仓使用的气态蒸汽。 */
    public static Liquid steam;
    /**蒸馏水，不会增加太阳能锅炉的钙化*/
    public static Liquid distilledWater;
    /**所有水，用于锅炉*/
    public static Liquid[] AllWater;
    public static void load() {
        steam = new Liquid("steam", Color.lightGray) {{
            gas = true;
            temperature = 0.9f;
            viscosity = 0.2f;
        }};
        distilledWater = new Liquid("distilled_water", Color.valueOf("6275cc")) {{
            heatCapacity = 0.45f;
            effect = StatusEffects.wet;
            boilPoint = 0.5f;
            gasColor = Color.grays(0.9f);
        }};
        AllWater=new Liquid[]{Liquids.water,distilledWater};
    }
}
