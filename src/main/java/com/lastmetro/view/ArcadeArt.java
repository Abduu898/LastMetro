package com.lastmetro.view;

import com.lastmetro.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Small props drawn from a fixed pixel grid and restrained, flat palettes. */
public final class ArcadeArt {
    /** Shared life icon for the HUD and world pickups. */
    public static void heart(GraphicsContext g, double x, double y, double size, boolean full) {
        g.save(); g.translate(Math.rint(x),Math.rint(y));g.scale(size/30,size/30);g.setGlobalAlpha(full?1:.25);
        g.setFill(Color.web("#492b35"));g.fillRect(2,0,10,4);g.fillRect(18,0,10,4);g.fillRect(0,4,30,14);
        g.fillRect(4,18,22,4);g.fillRect(8,22,14,4);g.fillRect(12,26,6,4);
        g.setFill(Color.web("#d65358"));g.fillRect(4,4,8,10);g.fillRect(18,4,8,10);g.fillRect(2,8,26,8);
        g.fillRect(6,16,18,4);g.fillRect(10,20,10,4);g.fillRect(14,24,2,2);
        g.setFill(Color.web("#f1a486"));g.fillRect(5,5,5,3);g.restore();
    }
    public void hearts(GraphicsContext g, GameWorld world, double time, boolean reducedMotion) {
        for(var pickup:world.getHeartPickups()) {
            double bob=reducedMotion?0:Math.rint(Math.sin(time*4+pickup.getX()*.01)*2)*2;
            double x=Math.rint(pickup.getX()/2)*2,y=Math.rint(pickup.getY()/2)*2+bob;
            g.setFill(Color.rgb(242,143,142,.17));g.fillRect(x-4,y-4,36,36);
            heart(g,x,y,28,true);
            if(!reducedMotion){g.setFill(Color.web("#f7d8bc"));g.fillRect(x+32,y+4,2,8);g.fillRect(x+29,y+7,8,2);}
        }
    }
    public boolean hasProps() { return true; }
    private void rect(GraphicsContext g, String c, double x, double y, double w, double h) {
        g.setFill(Color.web(c)); g.fillRect(x,y,w,h);
    }
    public void prop(GraphicsContext g, int index, double x, double bottom, double width, double height) {
        g.save(); g.setImageSmoothing(false);
        g.translate(Math.rint(x/2)*2, Math.rint((bottom-height)/2)*2); g.scale(width/64, height/64);
        switch (index) {
            case 0 -> {
                rect(g,"#8f591f",16,0,32,64); rect(g,"#8f591f",8,6,48,52); rect(g,"#8f591f",2,16,60,32);
                rect(g,"#e6a636",16,3,32,56); rect(g,"#e6a636",7,13,50,38); rect(g,"#e6a636",12,7,40,50);
                rect(g,"#ffe080",18,6,28,4); rect(g,"#fff0b5",12,14,4,18); rect(g,"#b87527",48,24,5,25);
                rect(g,"#bd7a28",20,22,5,24); rect(g,"#bd7a28",39,22,5,24);
                rect(g,"#bd7a28",25,26,5,7); rect(g,"#bd7a28",30,32,4,7); rect(g,"#bd7a28",34,26,5,7);
                rect(g,"#fff0b5",20,17,21,3);
            }
            case 1 -> {
                rect(g,"#222b30",8,24,8,40); rect(g,"#222b30",48,24,8,40);
                rect(g,"#84614a",10,25,4,36); rect(g,"#84614a",50,25,4,36);
                rect(g,"#263037",0,10,64,27); rect(g,"#e2d5b5",2,13,60,21);
                g.setFill(Color.web("#b95748"));
                for(int i=0;i<4;i++) g.fillPolygon(new double[]{2+i*16,10+i*16,22+i*16,14+i*16},new double[]{32,32,14,14},4);
                rect(g,"#f5e6c8",3,13,58,3); rect(g,"#263037",0,32,64,3);
                rect(g,"#323438",4,61,17,3);rect(g,"#323438",44,61,17,3);
            }
            case 2 -> {
                rect(g,"#382b24",0,0,64,64); rect(g,"#8b6544",3,3,58,58);
                for(int i=0;i<4;i++){rect(g,"#b48b59",5+i*14,5,10,54);rect(g,"#6b4d37",15+i*14,5,2,54);}
                rect(g,"#d0a56c",4,4,56,5);rect(g,"#684936",4,55,56,5);
                g.setFill(Color.web("#684936"));g.fillPolygon(new double[]{7,14,57,50},new double[]{54,59,10,5},4);
                g.setFill(Color.web("#c39b65"));g.fillPolygon(new double[]{8,12,56,52},new double[]{51,55,12,8},4);
                for(int px:new int[]{8,52})for(int py:new int[]{9,52})rect(g,"#40362d",px,py,3,3);
            }
            default -> {
                rect(g,"#252d31",2,56,60,8);rect(g,"#43474a",7,53,50,7);
                g.setFill(Color.web("#d5813a"));g.fillPolygon(new double[]{26,38,54,10},new double[]{3,3,55,55},4);
                rect(g,"#f3d9ae",22,17,20,7);rect(g,"#f3d9ae",17,35,30,8);
                rect(g,"#f4b667",27,4,4,10);rect(g,"#9d572e",37,7,4,10);
            }
        }
        g.restore();
    }
    public void coins(GraphicsContext g, GameWorld world, double time, boolean reducedMotion) {
        g.save();g.setImageSmoothing(false);
        for(Coin coin:world.getCoins()) {
            double x=Math.rint((coin.getX()+12)/2)*2, y=Math.rint((coin.getY()+14)/2)*2;
            double width=reducedMotion?26:Math.rint((12+16*Math.abs(Math.cos(time*3.5+coin.getY()*.02)))/2)*2;
            prop(g,0,x-width/2,y+14,width,28);
            if(!reducedMotion && Math.sin(time*5+coin.getX()*.018)>.4) {
                g.setFill(Color.web("#fff0b5"));g.fillRect(x-16,y-20,2,10);g.fillRect(x-20,y-16,10,2);
            }
        }
        g.restore();
    }
}
