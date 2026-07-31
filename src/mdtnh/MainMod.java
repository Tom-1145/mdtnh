package mdtnh;

import arc.util.Log;
import mindustry.mod.Mod;

public class MainMod extends Mod {
    @Override
    public void loadContent() {
        Log.info("MainMod.loadContent() started");
        Log.info("Loading ModItems...");
        ModItems.load();          // 加载所有物品
        Log.info("Loading ModLiquids...");
        ModLiquids.load();        // 加载流体（如果有）
        Log.info("Loading BasicFactory...");
        BasicFactory.load();      // 加载工厂
        Log.info("All content loaded.");
    }
}