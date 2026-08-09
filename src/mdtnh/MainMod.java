package mdtnh;

import arc.Core;
import arc.Events;
import arc.util.Log;

import mdtnh.energy.MdtEnergyBlocks;
import mdtnh.energy.MdtEnergySystem;
import mdtnh.modui.buildui.MdtBuildMenuContent;
import mdtnh.modui.buildui.MdtBuildMenuFragment;

import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;

public class MainMod extends Mod {
    private MdtBuildMenuFragment buildMenu;

    public MainMod() {
        Events.on(ClientLoadEvent.class, event -> Core.app.post(() -> {
            if (buildMenu != null) return;
            Log.info("Loading MDT build menu...");
            MdtBuildMenuContent.load();
            buildMenu = new MdtBuildMenuFragment(MdtBuildMenuContent.registry);
            buildMenu.install();
            Log.info("MDT build menu installed.");
        }));
    }

    @Override
    public void loadContent() {
        Log.info("MainMod.loadContent() started");
        ModItems.load();
        ModLiquids.load();
        ModCrafters.load();
        VoltageExampleMachines.load();
        MdtEnergyBlocks.load();
        MdtEnergySystem.install();
        Log.info("All content loaded.");
    }
}