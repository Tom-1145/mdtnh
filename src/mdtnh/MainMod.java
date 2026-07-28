package mdtnh;

import arc.*;
import arc.graphics.Color;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.production.GenericCrafter;
import  mindustry.content.Items;
import  mindustry.content.Liquids;
import mindustry.world.draw.*;

import static mdtnh.ModCrafters.test;


public class MainMod extends Mod{

    public MainMod(){
        Log.info("Loaded ExampleJavaMod constructor.");
    }

    @Override
    public void loadContent(){
        ModItems.load();
        ModLiquids.load();
        ModCrafters.load(this);
        Log.info("Test block registered: @", test == null ? "null" : test.name);
    }

}
