package mdtnh;

import arc.graphics.Color;
import mindustry.type.Liquid;

public class ModLiquids {
    public static Liquid steam;

    public static void load(){
        steam=new mindustry.type.Liquid("steam", Color.lightGray){{gas=true;temperature=0.9f;viscosity=0.2f;}};
    }
}
