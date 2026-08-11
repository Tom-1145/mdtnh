package mdtnh;

import arc.Core;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.io.TypeIO;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;

public class Boiler extends Block {
    //productSpeed为每刻每摄氏度距沸点的温差产生的蒸汽量，1L水稳定产生160L蒸汽
    public float maxHeat,maxWaterAmount,maxSteamAmount,heatSpeed,heatLoseSpeed,productSpeed;
    public DrawBlock drawer = new DrawDefault();
    public FuelList fuelList;
    public int frame = 3;//工作动画帧数
    public TextureRegion[] textures;
    public Boiler(String name) {
        super(name);
        this.hasItems=true;
        this.hasLiquids=true;
        this.update=true;
        this.liquidCapacity=100f;//最大流体IO速度
        textures = new TextureRegion[frame+1];
    }
    @Override
    public void setBars(){
        //super.setBars(); 不好用，会显示流体缓存的那部分
        addBar("health", entity -> new Bar("stat.health", Pal.health, entity::healthf).blink(Color.white));
        addBar("Heat",(BoilerBuilding build)->new Bar(
                ()->Core.bundle.get("recipe.heat"),
                ()->Color.red,
                ()->build.heat/maxHeat
        ));
        addBar("Water",(BoilerBuilding build)->new Bar(
                ()->build.water.liquid == null ? Core.bundle.get("bar.liquid"):build.water.liquid.localizedName,
                ()->build.water.liquid == null? Color.gray:build.water.liquid.color,
                ()->build.water.amount/maxWaterAmount
        ));
        addBar("Steam",(BoilerBuilding build)->new Bar(
                ()->ModLiquids.steam.localizedName,
                ()->ModLiquids.steam.color,
                ()->build.steamAmount/maxSteamAmount
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
    @Override
    public void load(){
        super.load();
        textures[0]=Core.atlas.find(name+"-idle");
        for(int i = 1;i <= frame;i++){
            textures[i]=Core.atlas.find(name+"-burning-"+i);
        }
    }
    public class BoilerBuilding extends Building {
        public float heat,steamAmount,burnTime;
        int animationTick=0,ticker=0;
        public LiquidStack water = new LiquidStack(Liquids.water,0);
        boolean dryBraised;
        @Override
        public void draw(){
            if(burnTime>0){
                if(ticker == 7){
                    ticker=0;
                    animationTick= Math.toIntExact(Math.round(Math.random() * 2+1));
                }ticker++;
            }else{
                ticker=0;
                animationTick=0;
            }
            Draw.rect(textures[animationTick],x,y);
        }
        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }
        @Override
        public boolean shouldConsume(){
            if(fuelList.check(items) == null){
                return false;
            }
            if(fuelList.list.get(fuelList.check(items)).slag == null){
                return enabled;
            }
            if(fuelList.list.get(fuelList.check(items)).slag.amount + items.get(fuelList.list.get(fuelList.check(items)).slag.item) > itemCapacity){
                return false;
            }
            if(burnTime > 0){
                return false;
            }
            return enabled;
        }
        @Override
        public void updateTile(){
            if(dryBraised && water.amount > 0){
                //爆炸，但现在没接口
            }
            liquidCapacity = maxWaterAmount - water.amount;
            if(burnTime > 0){
                burnTime--;
                heat = Math.min(heat+heatSpeed-heatLoseSpeed,maxHeat);
            }else{
                if(fuelList.check(items)!=null){
                    burnTime+=fuelList.list.get(fuelList.check(items)).burnTime;
                    if(Math.random()<fuelList.list.get(fuelList.check(items)).slagProductChance&&shouldConsume()){
                        items.add(fuelList.list.get(fuelList.check(items)).slag.item,fuelList.list.get(fuelList.check(items)).slag.amount);
                    }
                    items.remove(fuelList.check(items),1);
                }
                heat = Math.max(heat-heatLoseSpeed,20);
            }
            if(water.amount > 0){
                if(heat > 100){
                    if(water.amount < (heat-100)*productSpeed/160F){
                        steamAmount=steamAmount+water.amount*160;
                        water.amount = 0;
                    }else{
                        water.amount -= (heat-100)*productSpeed/160F;
                        steamAmount = steamAmount+(heat-100)*productSpeed;
                    }
                }
            }
            if(water.amount == 0){
                water.liquid=null;
            }
            liquids.add(ModLiquids.steam,steamAmount);
            steamAmount = 0;
            dumpLiquid(ModLiquids.steam);
            steamAmount += liquids.get(ModLiquids.steam);
            liquids.remove(ModLiquids.steam,liquids.get(ModLiquids.steam));
            if(steamAmount > maxSteamAmount){
                MainMod.IdToSound.get(1).at(x,y,1f,5f);
                steamAmount = maxSteamAmount * 0.75f;
            }
            for(Item i:fuelList.list.keys()){
                dump(fuelList.list.get(i).slag.item);
            }
            dryBraised = (heat > 100 && water.amount == 0);
        }
        @Override
        public boolean acceptItem(Building source,Item item){
            return fuelList.list.containsKey(item) && items.get(item) + 1 <= itemCapacity;
        }
        @Override
        public boolean acceptLiquid(Building source,Liquid liquid){
            if(water.liquid != null){
                return water.liquid.equals(liquid);
            }else {
                for (Liquid i : ModLiquids.AllWater) {
                    if (liquids.get(i) > 0) {
                        return i.equals(liquid);
                    }
                }
            }for(Liquid i:ModLiquids.AllWater){
                if(i.equals(liquid)){
                    return true;
                }
            }return false;
        }
        @Override
        public void handleLiquid(Building source,Liquid liquid,float amount){
            if(water.liquid == null)water.liquid=liquid;
            water.amount += amount;
        }
        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
            write.f(steamAmount);
            write.f(burnTime);
            TypeIO.writeLiquidStacks(write,new LiquidStack[]{water});
            write.i(animationTick);
        }
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
            steamAmount = read.f();
            burnTime = read.f();
            water = TypeIO.readLiquidStacks(read)[0];
            animationTick = read.i();
        }
    }
}
