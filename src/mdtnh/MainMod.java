package mdtnh;

import arc.Core;
import arc.Events;
import arc.util.Log;

import mdtnh.energy.MdtEnergyBlocks;
import mdtnh.energy.MdtEnergySystem;
import mdtnh.modui.buildui.MdtBuildMenuContent;
import mdtnh.modui.buildui.MdtBuildMenuFragment;
import mdtnh.modui.itemui.MdtCoreItemsQuickBar;

import mdtnh.turret.MdtAmmoTurretsExample;
import mdtnh.turret.MdtImplementedTurrets;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;

public class MainMod extends Mod {

    private MdtBuildMenuFragment buildMenu;

    // 新增
    private MdtCoreItemsQuickBar itemQuickBar;

    public MainMod() {

        Events.on(ClientLoadEvent.class, event -> Core.app.post(() -> {

            /*
             * ==============================
             * 自定义建造菜单
             * ==============================
             */
            if (buildMenu == null) {
                Log.info("Loading MDT build menu...");

                MdtBuildMenuContent.load();

                buildMenu =new MdtBuildMenuFragment(
                                MdtBuildMenuContent.registry
                        );
                buildMenu.install();
                Log.info("MDT build menu installed.");
            }
            if (itemQuickBar == null) {
                Log.info("Loading MDT core item quick bar...");
                itemQuickBar =new MdtCoreItemsQuickBar();
                itemQuickBar.install();
                Log.info("MDT core item quick bar installed.");
            }
        }));
    }

    @Override
    public void loadContent() {

        Log.info(
                "MainMod.loadContent() started"
        );

        ModItems.load();
        ModLiquids.load();
        ModCrafters.load();
        MdtAmmoTurretsExample.load();
        MdtImplementedTurrets.load();
        VoltageExampleMachines.load();

        MdtEnergyBlocks.load();
        MdtEnergySystem.install();

        Log.info(
                "All content loaded."
        );
    }
}
