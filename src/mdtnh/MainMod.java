package mdtnh;

import arc.util.Log;
import mdtnh.energy.MdtEnergyBlocks;
import mdtnh.energy.MdtEnergySystem;
import mindustry.mod.Mod;

public class MainMod extends Mod {
    @Override
    public void loadContent() {
        Log.info("MainMod.loadContent() started");

        Log.info("Loading ModItems...");
        ModItems.load();

        Log.info("Loading ModLiquids...");
        ModLiquids.load();

        Log.info("Loading BasicFactory...");
        ModCrafters.load();

        Log.info("Loading MDT discrete energy examples...");
        MdtEnergyBlocks.load();
        MdtEnergySystem.install();

        Log.info("All content loaded.");
    }
}
