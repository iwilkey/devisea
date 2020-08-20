package com.iwilkey.designa.inventory.blueprints.sections;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.iwilkey.designa.assets.Assets;
import com.iwilkey.designa.inventory.blueprints.BlueprintSection;
import com.iwilkey.designa.inventory.blueprints.Blueprints;
import com.iwilkey.designa.inventory.blueprints.ItemBlueprint;
import com.iwilkey.designa.gfx.Text;

public class MachineSection extends BlueprintSection {

    public MachineSection(Blueprints workbench, int x, int y) {
        super("Machines", workbench, x, y);
    }

    @Override
    public void tick() {
        if(items.size() > 0) {
            for(ItemBlueprint ir : items) {
                ir.tick();
            }
        }

        input();
    }

    @Override
    public void render(Batch b) {
        b.draw(Assets.craftingTabs[2], tabX, tabY, w, h);

        if(isSelected) {
            Text.draw(b, "Machines", (tabX + 64) - 96 - 44, tabY - 24, 11);

            if (items.size() > 0) {
                for (ItemBlueprint ir : items) {
                    ir.render(b);
                }
            }
        }
    }
}