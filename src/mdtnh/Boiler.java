package mdtnh;

import arc.Core;
import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.util.Strings;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;

public class Boiler extends Block {
    public float maxHeat,maxWaterAmount,maxSteamAmount;
    public ObjectMap<String,Color> stateToColor = ObjectMap.of(
            "Normal",Color.green,
            "Idle",Color.gray,
            "Low water",Color.yellow,
            "Dry-braised",Color.red
    );
    public FuelList fuelList;
    public ItemStack[] fuel,slag;
    public Boiler(String name) {
        super(name);
        this.hasItems=true;
        this.hasLiquids=true;
        this.update=true;
    }
    @Override
    public void setBars(){
        super.setBars();
        addBar("Heat",(BoilerBuilding build)->new Bar(
                ()->"Heat",
                ()->Color.red,
                ()->build.heat/maxHeat
        ));
        addBar("Water",(BoilerBuilding build)->new Bar(
                ()->build.water.liquid.localizedName,
                ()->build.water.liquid.color,
                ()->build.water.amount/maxWaterAmount
        ));
        addBar("Steam",(BoilerBuilding build)->new Bar(
                ()->ModLiquids.steam.localizedName,
                ()->ModLiquids.steam.color,
                ()->build.steamAmount/maxSteamAmount
        ));
        addBar("State",(BoilerBuilding build)->new Bar(
                ()->build.workState,
                ()->stateToColor.get(build.workState),
                ()->1F
        ));
    }
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.input,Table->{
            Table.row();
            for(Item i:fuelList.list.keys()) {
                Table.table(Styles.grayPanel,table->{
                    if(i.unlockedNow()) {
                        table.add(StatValues.displayItem(i,1,fuelList.list.get(i).burnTime,true)).left();
                        table.table(info->{
                            info.add(Core.bundle.get("recipe.burn_time")+": "+Strings.autoFixed(fuelList.list.get(i).burnTime/60f,1)+" "+ Core.bundle.get("unit.seconds")).left();
                            info.table(slag->{
                                slag.add(Core.bundle.get("recipe.slag")+":");
                                slag.row();
                                slag.add(StatValues.displayItem(fuelList.list.get(i).slag.item,fuelList.list.get(i).slag.amount,false));
                                slag.row();
                                slag.add(Core.bundle.get("recipe.product_chance")+": "+Strings.autoFixed(fuelList.list.get(i).slagProductChance*100,1)+"%");
                            });
                        }).right();
                    }else{
                        table.image(Icon.lock).color(Pal.darkerMetal).size(35);
                    }
                });
                Table.row();
            }Table.add(Core.bundle.get("recipe.max_water_amount")+": "+maxWaterAmount);
            Table.row();
            Table.add(Core.bundle.get("recipe.max_steam_amount")+": "+maxSteamAmount);
        });
    }
    public class BoilerBuilding extends Building {
        public float heat,steamAmount;
        public LiquidStack water;
        String workState;
    }
}
