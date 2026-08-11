package mdtnh;

import arc.struct.ObjectMap;
import arc.util.Nullable;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.modules.ItemModule;

public class FuelList {
    public static class FuelProp{
        @Nullable
        ItemStack slag;
        float burnTime;
        float slagProductChance;
        public FuelProp(ItemStack slag,float burnTime,float slagProductChance) {
            this.slag = slag;
            this.burnTime = burnTime;
            this.slagProductChance = slagProductChance;
        }
        public FuelProp(ItemStack slag,float burnTime){
            this(slag,burnTime,1f);
        }
        public FuelProp(float burnTime){
            this(null,burnTime,1f);
        }
    }
    public ObjectMap<Item,FuelProp> list = new ObjectMap<>();
    public FuelList(Object... values){
        list = ObjectMap.of(values);
    }
    //返回可用燃料之一
    @Nullable
    public Item check(ItemModule items){
        for(Item i : list.keys()){
            if(items.get(i)>0){
                return i;
            }
        }return null;
    }
}
