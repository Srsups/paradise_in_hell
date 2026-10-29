import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Technical crop, root registration and atlas packing only; artwork comes from generated RGBA PNGs. */
class MilestoneFiveAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/m5"),OUT=Path.of("core/assets/visual");
    static final String[] DIRS={"down","up","left","up-left","down-left"};
    static final LinkedHashMap<String,BufferedImage> TILES=new LinkedHashMap<>();
    static final StringBuilder LOG=new StringBuilder("clip,direction,frame,shift_x,shift_y,registration_score\n");
    static class Sheet {
        final BufferedImage image; final int[] xs,ys; final double scale;
        Sheet(String name,int columns)throws Exception {
            image=read(name);xs=EquippedAtlasBaker.cuts(image,columns,false);ys=EquippedAtlasBaker.cuts(image,5,true);
            double[] heights=new double[columns*5];int index=0;
            for(int row=0;row<5;row++)for(int col=0;col<columns;col++)
                heights[index++]=body(cell(col,row)).height;
            Arrays.sort(heights);scale=42/heights[heights.length/2];
        }
        Rectangle cell(int col,int row){return EquippedAtlasBaker.crop(xs,ys,col,row);}
        Rectangle body(Rectangle cell){return EquippedAtlasBaker.alpha(image,cell,96,true);}
        BufferedImage tile(int col,int row){
            Rectangle c=cell(col,row),b=body(c);double sum=0,pixels=0;
            for(int y=b.y;y<b.y+Math.max(2,(int)(b.height*.25));y++)for(int x=(int)(c.width*.36);x<c.width*.64;x++)
                if((image.getRGB(c.x+x,c.y+y)>>>24)>=96){sum+=x;pixels++;}
            double center=pixels>0?sum/pixels:c.width*.5;
            Rectangle island=bodyIsland(image,c,b);
            Rectangle clipped=new Rectangle(c.x+island.x,c.y+island.y,island.width,island.height);
            return PolishedFrames.render(image,clipped,scale,(int)Math.round(32-(center-island.x)*scale),
                (int)Math.round(55-(b.y+b.height-island.y)*scale));
        }
    }
    static BufferedImage read(String name)throws Exception {
        return readFile(name+"-original.png");
    }
    static BufferedImage readFile(String name)throws Exception {
        BufferedImage image=ImageIO.read(SOURCE.resolve(name).toFile());
        if(image==null||!image.getColorModel().hasAlpha()||(image.getRGB(0,0)>>>24)>16)
            throw new IllegalStateException("Generated source needs transparent RGBA: "+name);
        return image;
    }
    /** Crop to this authored character's connected opaque island; adjacent-cell fragments stay outside the crop. */
    static Rectangle bodyIsland(BufferedImage image,Rectangle cell,Rectangle body){
        int length=cell.width*cell.height,best=-1;double distance=Double.POSITIVE_INFINITY;
        for(int y=body.y;y<body.y+body.height;y++)for(int x=body.x;x<body.x+body.width;x++)
            if((image.getRGB(cell.x+x,cell.y+y)>>>24)>=96){
                double score=Math.pow(x-cell.width*.5,2)+Math.pow(y-(body.y+body.height*.4),2);
                if(score<distance){distance=score;best=y*cell.width+x;}
            }
        if(best<0)throw new IllegalStateException("Empty body island");
        boolean[] visited=new boolean[length];int[] queue=new int[length];int next=0,end=1;queue[0]=best;visited[best]=true;
        int left=cell.width,top=cell.height,right=0,bottom=0;
        while(next<end){int point=queue[next++],x=point%cell.width,y=point/cell.width;
            left=Math.min(left,x);right=Math.max(right,x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                int px=x+dx,py=y+dy;if(px<0||py<0||px>=cell.width||py>=cell.height)continue;
                int candidate=py*cell.width+px;
                if(!visited[candidate]&&(image.getRGB(cell.x+px,cell.y+py)>>>24)>=96){visited[candidate]=true;queue[end++]=candidate;}
            }
        }
        left=Math.max(0,left-2);top=Math.max(0,top-2);right=Math.min(cell.width,right+2);bottom=Math.min(cell.height,bottom+2);
        return new Rectangle(left,top,right-left,bottom-top);
    }
    static void register(String clip,int direction,int frame,BufferedImage reference,BufferedImage candidate){
        var aligned=PolishedFrames.align(reference,candidate);
        TILES.put(clip+"-"+DIRS[direction]+"-"+frame,aligned.tile());
        LOG.append(clip+","+DIRS[direction]+","+frame+","+aligned.dx()+","+aligned.dy()+","+
            String.format(Locale.ROOT,"%.3f",aligned.score())+"\n");
    }
    static void farmer(String tool,Sheet reference)throws Exception {
        String actor="farmer-"+tool;Sheet walk=new Sheet(actor+"-walk",6),actions=new Sheet(actor+"-actions",8);
        for(int direction=0;direction<5;direction++) {
            BufferedImage anchor=reference.tile(0,direction);
            for(int frame=0;frame<6;frame++)register(actor+"-walk",direction,frame,anchor,walk.tile(frame,direction));
            for(int frame=0;frame<4;frame++) {
                register(actor+"-idle",direction,frame,anchor,actions.tile(frame,direction));
                register(actor+"-attack",direction,frame,anchor,actions.tile(frame+4,direction));
            }
        }
    }
    static BufferedImage fitted(BufferedImage image,Rectangle cell,int size,int padding){
        Rectangle alpha=EquippedAtlasBaker.alpha(image,cell,16,false);
        double scale=(size-padding*2)/(double)Math.max(alpha.width,alpha.height);
        int width=(int)Math.round(alpha.width*scale),height=(int)Math.round(alpha.height*scale);
        BufferedImage tile=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image,(size-width)/2,(size-height)/2,(size-width)/2+width,(size-height)/2+height,
            cell.x+alpha.x,cell.y+alpha.y,cell.x+alpha.x+alpha.width,cell.y+alpha.y+alpha.height,null);
        g.dispose();return tile;
    }
    static void extras()throws Exception {
        BufferedImage image=readFile("farmer-extras-clean.png");int width=image.getWidth(),height=image.getHeight();
        // The portrait is wider than the nominal generated cell. Crop each authored island without repainting.
        int firstBottom=(int)Math.round(height*.442);
        int[] firstXs={0,(int)Math.round(width*.315),(int)Math.round(width*.505),(int)Math.round(width*.75),width};
        String[] names={"portrait-camponesa","npc-camponesa","weapon-rake","weapon-scythe"};
        for(int col=0;col<4;col++)TILES.put(names[col],fitted(image,
            new Rectangle(firstXs[col],0,firstXs[col+1]-firstXs[col],firstBottom),col==0?256:64,2));
        image=readFile("farmer-fx-clean.png");width=image.getWidth();height=image.getHeight();
        for(int row=0;row<2;row++) {
            int top=row*height/2,bottom=(row+1)*height/2;
            if(row==0){
                int opaqueTop=bottom,opaqueBottom=top;
                for(int py=top;py<bottom;py++)for(int px=0;px<width;px++)if((image.getRGB(px,py)>>>24)>=96){
                    opaqueTop=Math.min(opaqueTop,py);opaqueBottom=Math.max(opaqueBottom,py+1);
                }
                top=Math.max(top,opaqueTop-4);bottom=Math.min(bottom,opaqueBottom+4);
            }
            for(int col=0;col<4;col++) {
                Rectangle cell=new Rectangle(col*width/4,top,(col+1)*width/4-col*width/4,bottom-top);
                // Equal cells keep the circular sweep and thrust's pivot stable over the four frames.
                int tileHeight=row==0?24:64;
                BufferedImage tile=new BufferedImage(64,tileHeight,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(image,1,1,63,tileHeight-1,cell.x,cell.y,cell.x+cell.width,cell.y+cell.height,null);g.dispose();
                TILES.put("fx-"+(row==0?"rake":"scythe")+"-"+col,tile);
            }
        }
    }
    static void pack()throws Exception {
        BufferedImage page=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);Graphics2D g=page.createGraphics();
        StringBuilder atlas=new StringBuilder("m5.png\nsize: 1024, 1024\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n");
        int x=1,y=1,rowHeight=0;
        for(var entry:TILES.entrySet()) {
            BufferedImage tile=entry.getValue();int width=tile.getWidth(),height=tile.getHeight();
            if(x+width+1>1024){x=1;y+=rowHeight+2;rowHeight=0;}
            if(y+height+1>1024)throw new IllegalStateException("M5 atlas exceeds page");
            g.drawImage(tile,x,y,null);atlas.append(entry.getKey()+"\n  rotate: false\n  xy: "+x+", "+y+
                "\n  size: "+width+", "+height+"\n  orig: "+width+", "+height+"\n  offset: 0, 0\n  index: -1\n");
            x+=width+2;rowHeight=Math.max(rowHeight,height);
        }
        g.dispose();Files.createDirectories(OUT);ImageIO.write(page,"png",OUT.resolve("m5.png").toFile());
        Files.writeString(OUT.resolve("m5.atlas"),atlas);
        Files.writeString(SOURCE.resolve("registration.csv"),LOG);
        Files.createDirectories(Path.of("build/smoke"));
        BufferedImage preview=new BufferedImage(6*128,10*128,BufferedImage.TYPE_INT_ARGB);Graphics2D p=preview.createGraphics();
        p.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(int tool=0;tool<2;tool++)for(int direction=0;direction<5;direction++)for(int frame=0;frame<6;frame++){
            BufferedImage tile=TILES.get("farmer-"+(tool==0?"rake":"scythe")+"-walk-"+DIRS[direction]+"-"+frame);
            p.drawImage(tile,frame*128,(tool*5+direction)*128,128,128,null);
        }
        p.dispose();ImageIO.write(preview,"png",Path.of("build/smoke/m5-farmer-walk-contact-sheet.png").toFile());
        System.out.println("M5_ATLAS_OK: "+TILES.size()+" regions, 1024x1024, 4194304 RGBA bytes");
    }
    public static void main(String[] args)throws Exception {
        Sheet reference=new Sheet("farmer-rake-walk",6);farmer("rake",reference);farmer("scythe",reference);extras();pack();
    }
}
