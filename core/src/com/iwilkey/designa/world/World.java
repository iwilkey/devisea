package com.iwilkey.designa.world;

import com.badlogic.gdx.graphics.g2d.Batch;

import com.iwilkey.designa.GameBuffer;
import com.iwilkey.designa.assets.Assets;
import com.iwilkey.designa.entities.EntityHandler;
import com.iwilkey.designa.entities.creature.Player;
import com.iwilkey.designa.gfx.LightManager;
import com.iwilkey.designa.tiles.Tile;
import com.iwilkey.designa.utils.Utils;

public class World {

    private GameBuffer gb;
    public static int w, h;

    // Rendering
    public int[][] tiles;
    public int[][] tileBreakLevel;
    public int[][] lightMap;
    public int[][] origLightMap;

    // Entities
    private EntityHandler entityHandler;

    // Light
    private LightManager lightManager;

    // Environment
    private AmbientCycle ambientCycle;


    public World(GameBuffer gb, String path) {
        this.gb = gb;
        entityHandler = new EntityHandler(gb, new Player(gb, 100, 500));
        lightManager = new LightManager(gb, this);
        ambientCycle = new AmbientCycle(this, gb);
        loadWorld(path);
    }

    float time = 0;
    public void tick() {
        time += 0.01f;
        entityHandler.tick();
        ambientCycle.tick();
    }

    public void render(Batch b) {

        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                int xx = x * Tile.TILE_SIZE;
                int yy = y * Tile.TILE_SIZE;

                // Layers need to be rendered here because the Layer class was too performance intensive
                ambientCycle.render(b, xx, yy);
                if(yy < (h - 16) * Tile.TILE_SIZE) b.draw(Assets.backDirt, xx, yy, 16, 16);
                getTile(x, y).render(b, xx, yy, tileBreakLevel[x][(h - y) - 1], getTile(x, y).getID());

                lightManager.renderLight(b, x, y); // Render light
            }
        }

        entityHandler.render(b); // Front

        entityHandler.getPlayer().getBuildingHandler().render(b);

    }

    public Tile getTile(int x, int y) {
        if(x < 0 || y < 0 || x >= w || y >= h) return Tile.airTile;
        Tile t = Tile.tiles[tiles[x][Math.abs(h - y) - 1]];
        if(t == null) return Tile.airTile;
        return t;
    }

    private void loadWorld(String path) {
        String file = Utils.loadFileAsString(path);
        String[] tokens = file.split("\\s+");

        w = Utils.parseInt(tokens[0]);
        h = Utils.parseInt(tokens[1]);

        tiles = new int[w][h];
        tileBreakLevel = new int[w][h];
        lightMap = new int[w][h];
        origLightMap = new int[w][h];

        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                int id = Utils.parseInt(tokens[x + y * w + 2]);
                tiles[x][y] = id;
                tileBreakLevel[x][y] = Tile.getStrength(id);

                // TODO: Whenever 16, or (16 - 1) is used, that has to do with the world ground level! That should be defined in all worlds for
                // ambient light to work properly.
                if(y > 15 && tiles[x][y] != 0) {
                    lightMap[x][h - y - 1] = 6 - (Math.abs(y - 16));
                    origLightMap[x][h - y - 1] = 6 - (Math.abs(y - 16));
                } else if (tiles[x][y] == 0) {
                    lightMap[x][h - y - 1] = 6;
                    origLightMap[x][h - y - 1] = 6;
                }

            }
        }
    }

    public GameBuffer getGameBuffer() {
        return gb;
    }
    public EntityHandler getEntityHandler() { return entityHandler; }
    public int[][] getLightMap() { return this.origLightMap; }
    public LightManager getLightManager() { return lightManager; }
    public AmbientCycle getAmbientCycle() { return ambientCycle; }
    public void setLightMap(int[][] nlm) { this.lightMap = nlm; }

}