package mdtnh.gen;

import arc.math.Rand;
import mindustry.content.Blocks;
import mindustry.world.Block;

import java.util.ArrayList;
import java.util.List;

public class MineralVein {
    float density = 1.0f;
    String name;
    List<Block>ore=new ArrayList<>();
    List<Integer>oreweight=new ArrayList<>();
    int weight=1;
    int sumWeight = 0;
    public MineralVein(String n){
        name=n;
    }
    public void updateSum(){
        sumWeight=0;
        for(var i:oreweight)sumWeight+=i;
    }
    public Block generateOre(Rand rnd){
        int x=rnd.nextInt(sumWeight)+1;
        int sum = 0;
        for(int i = 0; i < ore.size(); i++){
            sum+=oreweight.get(i);
            if(x<=sum){
                return ore.get(i);
            }
        }
        return null;
    }
}
