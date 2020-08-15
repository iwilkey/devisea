package com.iwilkey.designa.world;

import com.badlogic.gdx.graphics.g2d.Batch;

import com.iwilkey.designa.Game;
import com.iwilkey.designa.GameBuffer;
import com.iwilkey.designa.assets.Assets;
import com.iwilkey.designa.entities.EntityHandler;
import com.iwilkey.designa.entities.creature.Npc;
import com.iwilkey.designa.entities.creature.Player;
import com.iwilkey.designa.gfx.Camera;
import com.iwilkey.designa.gfx.LightManager;
import com.iwilkey.designa.items.Item;
import com.iwilkey.designa.items.ItemHandler;
import com.iwilkey.designa.tiles.Tile;
import com.iwilkey.designa.utils.Utils;

public class World {

    private final GameBuffer gb;
    public static int w, h;

    // Rendering
    public static int[][] tiles;
    public static int[][] backTiles;
    public int[][] tileBreakLevel;
    public int[][] backTileBreakLevel;
    public static int[][] lightMap;
    public int[][] origLightMap;
    public static int[] origHighTiles;

    // Entities
    private final EntityHandler entityHandler;

    // Items
    private static ItemHandler itemHandler;

    // Light
    private final LightManager lightManager;

    // Environment
    private final AmbientCycle ambientCycle;

    public World(GameBuffer gb, String path) {
        this.gb = gb;
        lightManager = new LightManager(gb, this);
        ambientCycle = new AmbientCycle(this, gb);
        entityHandler = new EntityHandler(new Player(gb, 0,
                0));
        itemHandler = new ItemHandler(gb);

        loadWorld(path);

        for(int i = 0; i < 99; i++) entityHandler.getPlayer().getInventory().addItem(Item.oakWoodItem);
        for(int i = 0; i < 32; i++) entityHandler.getPlayer().getInventory().addItem(Item.stoneItem);
        for(int i = 0; i < 10; i++) entityHandler.getPlayer().getInventory().addItem(Item.oakWoodItem);

        for(int i = 1; i < 12; i++) {
            entityHandler.addEntity(new Npc(gb, ((w / 2f) + (i * 2)) * Tile.TILE_SIZE, (LightManager.highestTile[((w / 2) + 1)]) * Tile.TILE_SIZE));
            entityHandler.addEntity(new Npc(gb, ((w / 2f) - (i * 2)) * Tile.TILE_SIZE, (LightManager.highestTile[((w / 2) - 1)]) * Tile.TILE_SIZE));
        }
    }

    public void tick() {
        ambientCycle.tick();
        itemHandler.tick();
        entityHandler.tick();

    }

    public void render(Batch b) {

        int xStart = (int) Math.max(0, ((-Camera.position.x / Camera.scale.x) / Tile.TILE_SIZE) - 1);
        int xEnd = (int) Math.min(w, ((((-Camera.position.x + Game.w) / Camera.scale.x) / Tile.TILE_SIZE) + 4));
        int yStart = (int) Math.max(0, ((-Camera.position.y / Camera.scale.y) / Tile.TILE_SIZE) - 1);
        int yEnd = (int) Math.min(h, ((((-Camera.position.y + Game.h) / Camera.scale.y) / Tile.TILE_SIZE) + 4));

        for(int y = yStart; y < yEnd; y++) {
            for(int x = xStart; x < xEnd; x++) {
                int xx = x * Tile.TILE_SIZE;
                int yy = y * Tile.TILE_SIZE;

                ambientCycle.render(b, xx, yy);
            }
        }

        entityHandler.staticRender(b);

        for(int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                int xx = x * Tile.TILE_SIZE;
                int yy = y * Tile.TILE_SIZE;
                if(yy < (origHighTiles[x]) * Tile.TILE_SIZE) b.draw(Assets.backDirt, xx, yy, 16, 16);
                getBackTile(x, y).renderBackTile(b, xx, yy, backTileBreakLevel[x][(h - y) - 1], getBackTile(x, y).getID());
                getTile(x, y).renderAmbientLight(b, xx, yy);
                getTile(x, y).render(b, xx, yy, tileBreakLevel[x][(h - y) - 1], getTile(x, y).getID());
                getTile(x, y).tick();
            }
        }

        entityHandler.npcRender(b);
        entityHandler.playerRender(b);

        for(int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                lightManager.renderLight(b, x, y);
            }
        }

        itemHandler.render(b);

        entityHandler.getPlayer().getBuildingHandler().render(b);

    }

    public Tile getTile(int x, int y) {
        if(x < 0 || y < 0 || x >= w || y >= h) return Tile.airTile;
        Tile t = Tile.tiles[tiles[x][Math.abs(h - y) - 1]];
        if(t == null) return Tile.airTile;
        return t;
    }

    public Tile getBackTile(int x, int y) {
        if(x < 0 || y < 0 || x >= w || y >= h) return Tile.airTile;
        Tile t = Tile.tiles[backTiles[x][Math.abs(h - y) - 1]];
        if(t == null) return Tile.airTile;
        return t;
    }

    private void loadWorld(String path) {
        String file = Utils.loadFileAsString(path);
        String[] tokens = file.split("\\s+");

        w = Utils.parseInt(tokens[0]);
        h = Utils.parseInt(tokens[1]);

        tiles = new int[w][h];
        backTiles = new int[w][h];
        tileBreakLevel = new int[w][h];
        backTileBreakLevel = new int[w][h];
        lightMap = new int[w][h];
        origLightMap = new int[w][h];
        origHighTiles = new int[w];

        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                int id = Utils.parseInt(tokens[x + y * w + 2]);
                tiles[x][y] = id;
                tileBreakLevel[x][y] = Tile.getStrength(id);
                lightMap[x][h - y - 1] = 0;
                origLightMap[x][h - y - 1] = 0;
            }
        }

        origHighTiles = LightManager.findHighestTiles();
        lightMap = lightManager.buildAmbientLight(lightMap);

        WorldGeneration.EnvironmentGeneration(gb, entityHandler);

        entityHandler.getPlayer().setX((w / 2f) * Tile.TILE_SIZE);
        entityHandler.getPlayer().setY((LightManager.highestTile[(w / 2)]) * Tile.TILE_SIZE);
    }

    public static void bake(int[][] lm) { lightMap = lm; }
    public GameBuffer getGameBuffer() { return gb; }
    public static ItemHandler getItemHandler() { return itemHandler; }
    public EntityHandler getEntityHandler() { return entityHandler; }
    public int[][] getOrigLightMap() { return this.origLightMap; }
    public LightManager getLightManager() { return lightManager; }
    public AmbientCycle getAmbientCycle() { return ambientCycle; }

}
