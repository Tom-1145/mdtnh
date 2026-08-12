package mdtnh;

import mindustry.type.Item;

public class Component {
    public enum ComponentType{
        motor("motor"),
        piston("piston"),
        pomp("pomp"),
        robotic_arm("robotic-arm"),
        conveyor_belt("conveyor-belt"),
        emitter("emitter"),
        sensor("sensor"),
        force_filed_generator("force-filed-generator");

        public final String displayName;

        ComponentType(String name){
            displayName=name;
        }
    };
    public static Item[][] ComponentList=new Item[8][15];
    public static Item getComponentList(ComponentType type,VoltageTier tier){
        return ComponentList[type.ordinal()][tier.ordinal()];
    }
    public static void load(){
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 15; j++){
                ComponentList[i][j]=new Item(ComponentType.values()[i].displayName+'-'+VoltageTier.values()[j].contentName);
            }
        }
    }
}
