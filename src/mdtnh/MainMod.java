package mdtnh;

import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.struct.ObjectMap;
import arc.util.Log;

import mdtnh.energy.MdtEnergyBlocks;
import mdtnh.energy.MdtEnergySystem;
import mdtnh.gen.MineralVeins;
import mdtnh.gen.ModPlanet;
import mdtnh.gen.ModSectors;
import mdtnh.fluidSystem.GT.GTFluid;
import mdtnh.gen.block.ModOre;
import mdtnh.graphics.MdtMaterialDraw;
import mdtnh.modui.buildui.MdtBuildMenuContent;
import mdtnh.modui.buildui.MdtBuildMenuFragment;
import mdtnh.modui.itemui.MdtCoreItemsQuickBar;

import mdtnh.turret.MdtAmmoTurretsExample;
import mdtnh.turret.MdtImplementedTurrets;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;

public class MainMod extends Mod {
    public static ObjectMap<Integer, Sound> IdToSound = new ObjectMap<>();
    private MdtBuildMenuFragment buildMenu;

    // 新增
    private MdtCoreItemsQuickBar itemQuickBar;

    public MainMod() {

        Events.on(ClientLoadEvent.class, event -> Core.app.post(() -> {
            MdtMaterialDraw.load();

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
                Log.info("Loading MDT core item quick rod...");
                itemQuickBar =new MdtCoreItemsQuickBar();
                itemQuickBar.install();
                Log.info("MDT core item quick rod installed.");
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
        ModOre.load();
        MineralVeins.load();
        Component.load();
        ModCrafters.load();
        MdtAmmoTurretsExample.load();
        MdtImplementedTurrets.load();
        VoltageExampleMachines.load();

        ModPlanet.load();
        ModSectors.load();

        MdtEnergyBlocks.load();
        MdtEnergySystem.install();
        Core.assets.load("sounds/steamOverFlow.ogg",Sound.class).loaded=a->{
            IdToSound.put(1,a);
        };
        Log.info(
                "All content loaded."
        );
    }
}
