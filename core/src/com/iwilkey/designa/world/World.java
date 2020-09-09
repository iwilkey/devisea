package com.iwilkey.designa.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;

import com.iwilkey.designa.Game;
import com.iwilkey.designa.GameBuffer;
import com.iwilkey.designa.assets.Assets;
import com.iwilkey.designa.entities.EntityHandler;
import com.iwilkey.designa.entities.creature.passive.Player;
import com.iwilkey.designa.gfx.Camera;
import com.iwilkey.designa.gfx.LightManager;
import com.iwilkey.designa.inventory.Inventory;
import com.iwilkey.designa.inventory.InventorySlot;
import com.iwilkey.designa.items.Item;
import com.iwilkey.designa.items.ItemHandler;
import com.iwilkey.designa.machines.MachineHandler;
import com.iwilkey.designa.tiles.Tile;
import com.iwilkey.designa.utils.Utils;

import java.io.IOException;
import java.io.Writer;

public class World {

    private final GameBuffer gb;
    public static int w, h;
    public static String dirpath = "";

    // Rendering
    public static int[][] tiles;
    public static int[][] backTiles;
    public static int[][] pipeMap;
    public static int[][] tileBreakLevel;
    public static int[][] backTileBreakLevel;
    public static int[][] lightMap;
    public int[][] origLightMap;
    public static int[] origHighTiles;
    public static int[] origHighBackTiles;

    // Entities
    private static EntityHandler entityHandler = null;

    // Items
    private static ItemHandler itemHandler;

    // Light
    private final LightManager lightManager;

    // Environment
    private final AmbientCycle ambientCycle;

    // Machines
    private final MachineHandler machineHandler;

    public World(GameBuffer gb, String path) {
        this.gb = gb;
        lightManager = new LightManager(gb, this);
        ambientCycle = new AmbientCycle(this, gb);
        entityHandler = new EntityHandler(new Player(gb, 0,
                0));
        itemHandler = new ItemHandler(gb);
        machineHandler = new MachineHandler(gb);

        loadWorld(path);

        // entityHandler.addEntity(new Npc(gb, ((w / 2f) + 1) * Tile.TILE_SIZE, (LightManager.highestTile[((w / 2) + 1)]) * Tile.TILE_SIZE));
        // entityHandler.addEntity(new TerraBot(gb, ((w / 2f) + 2) * Tile.TILE_SIZE, (LightManager.highestTile[((w / 2) + 2)]) * Tile.TILE_SIZE));
        // entityHandler.addEntity(new TerraBot(gb, ((w / 2f) - 2) * Tile.TILE_SIZE, (LightManager.highestTile[((w / 2) - 2)]) * Tile.TILE_SIZE));
    }

    public void tick() {
        ambientCycle.tick();
        itemHandler.tick();
        entityHandler.tick();
        machineHandler.tick();
        for(WorldGeneration.Cloud cloud : WorldGeneration.clouds) cloud.tick();
    }

    private void giveItem(Item item) {
        Inventory inv = entityHandler.getPlayer().getInventory();
        inv.addItem(item);
    }

    private void giveItem(Item item, int amount) {
        Inventory inv = entityHandler.getPlayer().getInventory();
        for(int ii = 0; ii < amount; ii++) inv.addItem(item);
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

        // Draw environment
        for(WorldGeneration.Cloud cloud : WorldGeneration.clouds) cloud.render(b);
        // for(WorldGeneration.Mountain mountain : WorldGeneration.mountains) mountain.render(b);

        for(int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                int xx = x * Tile.TILE_SIZE;
                int yy = y * Tile.TILE_SIZE;
                if(yy < (origHighTiles[x]) * Tile.TILE_SIZE) b.draw(Assets.backDirt, xx, yy, 16, 16);
                if(yy < (origHighBackTiles[x]) * Tile.TILE_SIZE) b.draw(Assets.backDirt, xx, yy, 16, 16);
                getBackTile(x, y).renderBackTile(b, xx, yy, backTileBreakLevel[x][(h - y) - 1], getBackTile(x, y).getID());
            }
        }

        entityHandler.staticRender(b);

        for(int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                int xx = x * Tile.TILE_SIZE;
                int yy = y * Tile.TILE_SIZE;
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
        machineHandler.render(b);

    }

    public static Tile getTile(int x, int y) {
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
        dirpath = path;

        // Init front tiles
        String ft = Utils.loadFileAsString(path + "ft.dsw");
        String[] ftTokens = ft.split("\\s+");
        w = Utils.parseInt(ftTokens[0]);
        h = Utils.parseInt(ftTokens[1]);
        tiles = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int id = Utils.parseInt(ftTokens[x + y * w + 2]);
                tiles[x][y] = id;
                if(id == Tile.torchTile.getID()) lightManager.addLight(x, h - y - 1, 8);
            }
        }

        // Init front back tiles
        String ftblf = Utils.loadFileAsString(path + "ftbl.dsw");
        String[] ftblTokens = ftblf.split("\\s+");
        tileBreakLevel = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int s = Utils.parseInt(ftblTokens[x + y * w + 2]);
                tileBreakLevel[x][y] = s;
            }
        }

        // Init back tiles
        String btf = Utils.loadFileAsString(path + "bt.dsw");
        String[] btTokens = btf.split("\\s+");
        backTiles = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int bid = Utils.parseInt(btTokens[x + y * w + 2]);
                backTiles[x][y] = bid;
            }
        }

        // Init back tile break level
        String btblf = Utils.loadFileAsString(path + "btbl.dsw");
        String[] btblTokens = btblf.split("\\s+");
        backTileBreakLevel = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int btbl = Utils.parseInt(btblTokens[x + y * w + 2]);
                backTileBreakLevel[x][y] = btbl;
            }
        }

        // If it exits, init front high tiles
        FileHandle fhtf = Gdx.files.local(path + "fht.dsw");
        origHighTiles = new int[w];
        if(fhtf.exists()) {
            // Init from file
            String fht = Utils.loadFileAsString(path + "fht.dsw");
            String[] fhtTokens = fht.split("\\s+");
            for(int x = 0; x < World.w; x++) {
                int t = Utils.parseInt(fhtTokens[x]);
                origHighTiles[x] = t;
                LightManager.highestTile = new int[World.w];
                LightManager.highestTile[x] = t;
            }
        } else {
            // Init high tiles normally
            origHighTiles = LightManager.findHighestTiles();
        }

        // Same with back
        FileHandle bhtf = Gdx.files.local(path + "bht.dsw");
        origHighBackTiles = new int[w];
        if(bhtf.exists()) {
            // Init from file
            String bht = Utils.loadFileAsString(path + "bht.dsw");
            String[] bhtTokens = bht.split("\\s+");
            for(int x = 0; x < World.w; x++) {
                int t = Utils.parseInt(bhtTokens[x]);
                origHighBackTiles[x] = t;
                LightManager.highestBackTile = new int[World.w];
                LightManager.highestBackTile[x] = t;
            }
        } else {
            // Init high tiles normally
            origHighBackTiles = LightManager.findHighestBackTiles();
        }

        // Init light map
        lightMap = new int[w][h];
        origLightMap = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                lightMap[x][y] = 0;
                origLightMap[x][y] = 0;
            }
        }

        // Init pipe map
        String pmf = Utils.loadFileAsString(path + "pm.dsw");
        String[] pmTokens = pmf.split("\\s+");
        pipeMap = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pmi = Utils.parseInt(pmTokens[x + y * w + 2]);
                pipeMap[x][y] = pmi;
            }
        }

        lightMap = lightManager.buildAmbientLight(lightMap);
        lightManager.bakeLighting();

        // Continue generating world, if it's the first time
        String invPath = path + "inv.dsw";
        FileHandle invf = Gdx.files.local(invPath);
        if(!invf.exists()) {
            WorldGeneration.EnvironmentGeneration(gb, entityHandler);
            WorldGeneration.OreGeneration(); // If we're loading a world, we don't need to generate ores.
            entityHandler.getPlayer().setX((w / 2f) * Tile.TILE_SIZE);
            entityHandler.getPlayer().setY((LightManager.highestTile[(w / 2)]) * Tile.TILE_SIZE);
        } else {
            // Load in inventory, if it's not the first time
            String inv = Utils.loadFileAsString(path + "inv.dsw");
            String[] invTokens = inv.split("\\s+");
            for(int s = 0; s < invTokens.length; s++) {
                if(s > 1) {
                    String[] further = invTokens[s].split("-");
                    int itemID = Utils.parseInt(further[0]);
                    int count = Utils.parseInt(further[1]);
                    giveItem(Item.getItemByID(itemID), count);
                } else {
                    System.out.println(invTokens[0]);
                    entityHandler.getPlayer().setX(Utils.parseInt(invTokens[0]) * Tile.TILE_SIZE);
                    entityHandler.getPlayer().setY(Utils.parseInt(invTokens[1]) * Tile.TILE_SIZE);
                }
            }
        }
    }

    public void saveWorld() {
        // Save front tiles
        String ftPath = dirpath + "ft.dsw";
        FileHandle fh = Gdx.files.local(ftPath);
        fh.delete();
        FileHandle ftf = Gdx.files.local(ftPath);
        if(!writeFT(ftf, w, h)) System.exit(-1);

        // Save front tile break level
        String ftblPath = dirpath + "ftbl.dsw";
        FileHandle fhbl = Gdx.files.local(ftblPath);
        fhbl.delete();
        FileHandle ftblf = Gdx.files.local(ftblPath);
        if(!writeFTBL(ftblf, w, h)) System.exit(-1);

        // Save back tiles
        String btPath = dirpath + "bt.dsw";
        FileHandle bt = Gdx.files.local(btPath);
        bt.delete();
        FileHandle btf = Gdx.files.local(btPath);
        if(!writeBT(btf, w, h)) System.exit(-1);

        // Save back tiles break level
        String btblPath = dirpath + "btbl.dsw";
        FileHandle btbl = Gdx.files.local(btblPath);
        btbl.delete();
        FileHandle btblf = Gdx.files.local(btblPath);
        if(!writeBTBL(btblf, w, h)) System.exit(-1);

        // Save front high tiles
        String fhtPath = dirpath + "fht.dsw";
        FileHandle fhtf = Gdx.files.local(fhtPath);
        if(fhtf.exists()) {
            fhtf.delete();
            FileHandle fhtf2 = Gdx.files.local(fhtPath);
            if(!writeFHT(fhtf2, w)) System.exit(-1);
        } else {
            if(!writeFHT(fhtf, w)) System.exit(-1);
        }

        // Save back high tiles
        String bhtPath = dirpath + "bht.dsw";
        FileHandle bhtf = Gdx.files.local(bhtPath);
        if(bhtf.exists()) {
            bhtf.delete();
            FileHandle bhtf2 = Gdx.files.local(bhtPath);
            if(!writeBHT(bhtf2, w)) System.exit(-1);
        } else {
            if(!writeBHT(bhtf, w)) System.exit(-1);
        }

        // Save pipe map
        String pmPath = dirpath + "pm.dsw";
        FileHandle pm = Gdx.files.local(pmPath);
        pm.delete();
        FileHandle pmf = Gdx.files.local(pmPath);
        if(!writePM(pm, w, h)) System.exit(-1);

        // Save inventory
        String invPath = dirpath + "inv.dsw";
        FileHandle invf = Gdx.files.local(invPath);
        if(invf.exists()) {
            invf.delete();
            FileHandle invf2 = Gdx.files.local(invPath);
            if(!writeINV(invf2)) System.exit(-1);
        } else {
            if(!writeINV(invf)) System.exit(-1);
        }
    }

    private boolean writeFT(FileHandle fh, int width, int height) {
        try {
            Writer w = fh.writer(true);
            w.write(width + " " + height + "\n");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int num = tiles[x][y];
                    w.write(num + " ");
                }
                w.write("\n");
            }

            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean writeFTBL(FileHandle ftblf, int width, int height) {
        try {
            Writer w = ftblf.writer(true);
            w.write(width + " " + height + "\n");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int num = tileBreakLevel[x][y];
                    w.write(num + " ");
                }
                w.write("\n");
            }

            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean writeBT(FileHandle ftf, int width, int height) {
        try {
            Writer w = ftf.writer(true);
            w.write(width + " " + height + "\n");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int num = backTiles[x][y];
                    w.write(num + " ");
                }
                w.write("\n");
            }

            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean writeBTBL(FileHandle ftblf, int width, int height) {
        try {
            Writer w = ftblf.writer(true);
            w.write(width + " " + height + "\n");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int num = backTileBreakLevel[x][y];
                    w.write(num + " ");
                }
                w.write("\n");
            }

            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean writeFHT(FileHandle ftblf, int width) {
        try {
            Writer w = ftblf.writer(true);

            for (int x = 0; x < width; x++) {
                int num = origHighTiles[x];
                w.write(num + " ");
            }

            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean writeBHT(FileHandle ftblf, int width) {
        try {
            Writer w = ftblf.writer(true);

            for (int x = 0; x < width; x++) {
                int num = origHighBackTiles[x];
                w.write(num + " ");
            }

            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean writePM(FileHandle ftblf, int width, int height) {
        try {
            Writer w = ftblf.writer(true);
            w.write(width + " " + height + "\n");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int num = pipeMap[x][y];
                    w.write(num + " ");
                }
                w.write("\n");
            }

            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean writeINV(FileHandle ftblf) {
        try {
            Writer w = ftblf.writer(true);
            w.write((int)(entityHandler.getPlayer().getX() / Tile.TILE_SIZE) + " " +
                    (int)(entityHandler.getPlayer().getY() / Tile.TILE_SIZE));
            w.write("\n");

            for(int y = 0; y < 400 / InventorySlot.SLOT_HEIGHT; y++) {
                for (int x = 0; x < 200 / InventorySlot.SLOT_WIDTH; x++) {
                    Item i = null;
                    int count = 0;
                    try {
                        if (Inventory.slots[x][y].getItem() != null) {
                            i = Inventory.slots[x][y].getItem();
                            count = Inventory.slots[x][y].itemCount;
                            w.write(i.getItemID() + "-" + count + " ");
                        } else {
                            w.write("0-0 ");
                        }
                    } catch (ArrayIndexOutOfBoundsException ignored) {}
                }
                w.write("\n");
            }

            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void bake(int[][] lm) { lightMap = lm; }
    public GameBuffer getGameBuffer() { return gb; }
    public static ItemHandler getItemHandler() { return itemHandler; }
    public static EntityHandler getEntityHandler() { return entityHandler; }
    public int[][] getOrigLightMap() { return this.origLightMap; }
    public LightManager getLightManager() { return lightManager; }
    public AmbientCycle getAmbientCycle() { return ambientCycle; }

}
