package com.iwilkey.designa.gfx;

import com.iwilkey.designa.physics.Vector2;

public class Light {

    private int strength;
    public int x, y;

    public Light(int x, int y, int strength) {
        this.x = x; this.y = y;
        this.strength = strength;
    }

    public int[][] buildLightMap(int[][] oldLm, int w, int h) {
        int[][] newLm = oldLm;

        for(int yl = y - strength; yl < y + strength + 1; yl++) {
            for(int xl = x - strength; xl < x + strength + 1; xl++) {

                try {

                    Vector2 vec = new Vector2((x - xl), (y - yl));
                    float mag = Vector2.magnitude(vec);
                    int orig = oldLm[xl][yl];
                    if(!(6 - (mag) < orig))
                        newLm[xl][yl] = 6 - Math.round(mag);

                    if(yl == y + strength && xl == x + strength)
                        newLm[xl][yl] = orig;
                    if(yl == y - strength && xl == x + strength)
                        newLm[xl][yl] = orig;
                    if(yl == y + strength && xl == x - strength)
                        newLm[xl][yl] = orig;
                    if(yl == y - strength && xl == x - strength)
                        newLm[xl][yl] = orig;

                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }
            }
        }

        return newLm;

    }

}