import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Technical extraction, constant-scale registration and packing of the generated airborne actor. */
class FlyerAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/m7-flyer"),OUT=Path.of("core/assets/visual");
    static final String[] DIRECTIONS={"down","up","left","up-left","down-left"};
    static final int COLUMNS=12,ROWS=5,TILE=64,PADDING=3,ATLAS_WIDTH=1024,ATLAS_HEIGHT=512;
    static final double ROOT_X=32,ROOT_Y=64*.52;
    static final LinkedHashMap<String,BufferedImage> TILES=new LinkedHashMap<>();
    static final StringBuilder LOG=new StringBuilder("clip,direction,frame,source_column,source_row,source_x,source_y,source_width,source_height,fixed_scale,source_root_x,source_root_y,target_root_x,target_root_y,offset_x,offset_y,opaque_pixels,alpha_left,alpha_top,alpha_width,alpha_height\n");
    record Pivot(double x,double y){}
    record Extract(Rectangle cell,Rectangle alpha,Pivot pivot){}
    static String decimal(double value){return String.format(Locale.ROOT,"%.8f",value);}
    static String hash(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
    static int[] cuts(BufferedImage source,int count,boolean vertical) {
        int length=vertical?source.getHeight():source.getWidth(),cross=vertical?source.getWidth():source.getHeight();
        int[] density=new int[length],result=new int[count+1];result[count]=length;
        for(int p=0;p<length;p++)for(int q=0;q<cross;q++)
            if((source.getRGB(vertical?q:p,vertical?p:q)>>>24)>=96)density[p]++;
        double nominal=length/(double)count;
        for(int index=1;index<count;index++){
            double expected=index*nominal;int best=(int)Math.round(expected),score=Integer.MAX_VALUE;
            for(int p=Math.max(result[index-1]+1,(int)(expected-nominal*.2));p<Math.min(length,(int)(expected+nominal*.2));p++){
                int candidate=density[p]*1000+(int)Math.round(Math.abs(p-expected));
                if(candidate<score){score=candidate;best=p;}
            }
            if(density[best]>0)throw new IllegalStateException("No transparent gutter at "+(vertical?"row ":"column ")+index);
            result[index]=best;
        }
        return result;
    }
    static Rectangle alpha(BufferedImage source,Rectangle cell){
        int left=cell.width,top=cell.height,right=0,bottom=0;
        for(int y=0;y<cell.height;y++)for(int x=0;x<cell.width;x++)if((source.getRGB(cell.x+x,cell.y+y)>>>24)>=96){
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);
        }
        if(right<=left||bottom<=top)throw new IllegalStateException("Empty generated source cell "+cell);
        return new Rectangle(left,top,right-left,bottom-top);
    }
    static Pivot torsoPivot(BufferedImage source,Rectangle cell){
        // The central opaque torso mass excludes the changing wing span. It is only a registration measurement.
        double xSum=0,ySum=0,count=0;
        for(int y=(int)(cell.height*.15);y<cell.height*.92;y++)for(int x=(int)(cell.width*.36);x<cell.width*.64;x++)
            if((source.getRGB(cell.x+x,cell.y+y)>>>24)>=96){xSum+=x+.5;ySum+=y+.5;count++;}
        if(count<30)throw new IllegalStateException("Missing central body pixels in "+cell);
        return new Pivot(xSum/count,ySum/count);
    }
    static double limitedScale(List<Extract> frames){
        double scale=Double.POSITIVE_INFINITY;
        for(Extract frame:frames){
            Rectangle a=frame.alpha;Pivot p=frame.pivot;
            if(p.x>a.x)scale=Math.min(scale,(ROOT_X-PADDING)/(p.x-a.x));
            if(a.x+a.width>p.x)scale=Math.min(scale,(TILE-PADDING-ROOT_X)/(a.x+a.width-p.x));
            if(p.y>a.y)scale=Math.min(scale,(ROOT_Y-PADDING)/(p.y-a.y));
            if(a.y+a.height>p.y)scale=Math.min(scale,(TILE-PADDING-ROOT_Y)/(a.y+a.height-p.y));
        }
        return scale;
    }
    static void extract(BufferedImage source,String clip,int row,int frame,int column,Extract input,double scale){
        Rectangle c=input.cell;Pivot p=input.pivot;
        int dx=(int)Math.round(ROOT_X-p.x*scale),dy=(int)Math.round(ROOT_Y-p.y*scale);
        BufferedImage tile=new BufferedImage(TILE,TILE,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();
        g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source,dx,dy,dx+(int)Math.round(c.width*scale),dy+(int)Math.round(c.height*scale),
            c.x,c.y,c.x+c.width,c.y+c.height,null);g.dispose();
        Rectangle a=alpha(tile,new Rectangle(0,0,TILE,TILE));int opaque=0;
        for(int y=0;y<TILE;y++)for(int x=0;x<TILE;x++)if((tile.getRGB(x,y)>>>24)>=96)opaque++;
        if(a.x<1||a.y<1||a.x+a.width>TILE-1||a.y+a.height>TILE-1||opaque<50)
            throw new IllegalStateException("Clipped or empty frame "+clip+" "+DIRECTIONS[row]+" "+frame+": "+a);
        TILES.put("flyer-"+clip+"-"+DIRECTIONS[row]+"-"+frame,tile);
        LOG.append(clip+","+DIRECTIONS[row]+","+frame+","+column+","+row+","+c.x+","+c.y+","+c.width+","+c.height+","+decimal(scale)+","+
            decimal(p.x)+","+decimal(p.y)+","+decimal(ROOT_X)+","+decimal(ROOT_Y)+","+dx+","+dy+","+opaque+","+a.x+","+a.y+","+a.width+","+a.height+"\n");
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUT);Path file=SOURCE.resolve("flyer-original.png");BufferedImage source=ImageIO.read(file.toFile());
        if(source==null||!source.getColorModel().hasAlpha()||(source.getRGB(0,0)>>>24)>16)
            throw new IllegalStateException("The generated source must retain its transparent RGBA background");
        int[] xs=cuts(source,COLUMNS,false),ys=cuts(source,ROWS,true);
        Extract[][] inputs=new Extract[ROWS][COLUMNS];List<Extract> calibration=new ArrayList<>();
        for(int row=0;row<ROWS;row++){
            Rectangle recoveryCell=new Rectangle(xs[8],ys[row],xs[9]-xs[8],ys[row+1]-ys[row]);
            Pivot deathRoot=torsoPivot(source,recoveryCell);
            double deathU=deathRoot.x/recoveryCell.width,deathV=deathRoot.y/recoveryCell.height;
            for(int column=0;column<COLUMNS;column++){
                Rectangle cell=new Rectangle(xs[column],ys[row],xs[column+1]-xs[column],ys[row+1]-ys[row]);
                Pivot pivot=column<=8?torsoPivot(source,cell):new Pivot(deathU*cell.width,deathV*cell.height);
                inputs[row][column]=new Extract(cell,alpha(source,cell),pivot);calibration.add(inputs[row][column]);
            }
        }
        double scale=limitedScale(calibration);
        for(int row=0;row<ROWS;row++){
            for(int frame=0;frame<6;frame++)extract(source,"walk",row,frame,frame,inputs[row][frame],scale);
            for(int frame=0;frame<3;frame++)extract(source,"hurt",row,frame,6+frame,inputs[row][6+frame],scale);
            for(int frame=0;frame<4;frame++)extract(source,"death",row,frame,8+frame,inputs[row][8+frame],scale);
        }
        if(TILES.size()!=65)throw new IllegalStateException("Expected 65 frame regions");
        BufferedImage page=new BufferedImage(ATLAS_WIDTH,ATLAS_HEIGHT,BufferedImage.TYPE_INT_ARGB);Graphics2D graphics=page.createGraphics();graphics.setComposite(AlphaComposite.Src);
        StringBuilder atlas=new StringBuilder("m7-flyer.png\nsize: 1024, 512\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n");
        int index=0;for(var entry:TILES.entrySet()){
            int x=1+index%15*66,y=1+index/15*66;graphics.drawImage(entry.getValue(),x,y,null);
            atlas.append(entry.getKey()+"\n  rotate: false\n  xy: "+x+", "+y+"\n  size: 64, 64\n  orig: 64, 64\n  offset: 0, 0\n  index: -1\n");index++;
        }
        graphics.dispose();ImageIO.write(page,"png",OUT.resolve("m7-flyer.png").toFile());
        Files.writeString(OUT.resolve("m7-flyer.atlas"),atlas,StandardCharsets.UTF_8);Files.writeString(SOURCE.resolve("registration.csv"),LOG,StandardCharsets.UTF_8);
        String manifest="""
            {
              "date": "2026-11-04",
              "tool": "built-in image_gen",
              "prompt": "prompt.txt",
              "source": "flyer-original.png",
              "sourceSha256": "%s",
              "sourceDimensions": [%d, %d],
              "requestedGrid": [13, 5],
              "actualGrid": [12, 5],
              "canonicalDirections": ["down", "up", "left", "up-left", "down-left"],
              "frameColumns": {"walk": [0, 1, 2, 3, 4, 5], "hurt": [6, 7, 8], "death": [8, 9, 10, 11]},
              "layoutNote": "The generated sheet contains twelve occupied columns. Hurt frame 2 is reused as the first death anticipation frame; no missing pose was synthesized.",
              "registration": "registration.csv",
              "registrationRule": "Central torso mass aligned for flight/hurt; death preserves recovery-frame row root so the authored fall remains. A single fixed scale is shared by every direction/state.",
              "targetRoot": [32, 33.28],
              "fixedScale": %s,
              "operations": ["transparent-gutter crop", "constant-scale root registration", "nearest-neighbor reduction", "atlas packing"],
              "preserveGeneratedAlpha": true,
              "paintOrRecolor": false,
              "packed": {"atlas": "../../../core/assets/visual/m7-flyer.atlas", "page": "../../../core/assets/visual/m7-flyer.png", "regions": 65, "logicalPosesWithMirrors": 104, "frameDimensions": [64, 64], "dimensions": [1024, 512], "rgbaBytes": 2097152, "sha256": "%s"},
              "tooling": "../../FlyerAtlasBaker.java"
            }
            """.formatted(hash(file),source.getWidth(),source.getHeight(),decimal(scale),hash(OUT.resolve("m7-flyer.png")));
        Files.writeString(SOURCE.resolve("manifest.json"),manifest,StandardCharsets.UTF_8);
        System.out.println("FLYER_ATLAS_OK: "+TILES.size()+" regions, "+source.getWidth()+"x"+source.getHeight()+" source, 12x5 grid, scale="+decimal(scale)+", atlas1024x512, transparent gutters and no clipped frames");
    }
}
