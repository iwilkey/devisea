package dev.iwilkey.designa.gfx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class SpriteSheet {

    public static final int SLOT_SIZE = 8;
    private final Texture sheet;
    public SpriteSheet(Texture sheet) { this.sheet = sheet; }

    public TextureRegion crop(int row, int col, int w, int h) {
        return new TextureRegion(sheet, row * SLOT_SIZE, col * SLOT_SIZE, w, h);
    }

}
