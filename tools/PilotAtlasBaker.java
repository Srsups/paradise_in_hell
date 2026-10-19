import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Technical atlas packing: preserve alpha, normalize cell pivots and bake game-resolution copies. */
class PilotAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/m3"), OUT=Path.of("core/assets/visual");
    static final BufferedImage PAGE=new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB);
    static final StringBuilder INDEX=new StringBuilder("pilot.png\nsize: 1024, 512\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n");
    static final Graphics2D G=PAGE.createGraphics();
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUT);
        G.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        actor("hunter","hero",0);actor("common","common",128);
        BufferedImage fx=ImageIO.read(SOURCE.resolve("effects.png").toFile());
        String[] effects={"arrow","impact","xp","dash"};
        for(int row=0;row<4;row++)for(int col=0;col<4;col++)
            copy(fx,col*fx.getWidth()/4,row*fx.getHeight()/4,fx.getWidth()/4,fx.getHeight()/4,
                256+col*64,row*64,64,64,effects[row]+"-"+col);
        BufferedImage icons=ImageIO.read(SOURCE.resolve("icons.png").toFile());
        String[] iconNames={"health","stamina","experience","boon"};
        for(int i=0;i<4;i++)copy(icons,(i%2)*icons.getWidth()/2,(i/2)*icons.getHeight()/2,
            icons.getWidth()/2,icons.getHeight()/2,(i%2)*48,128+(i/2)*48,48,48,"icon-"+iconNames[i]);
        BufferedImage charon=ImageIO.read(SOURCE.resolve("charon.png").toFile());
        G.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        copy(charon,0,0,charon.getWidth(),charon.getHeight(),512,0,512,512,"portrait-charon");
        G.dispose();ImageIO.write(PAGE,"png",OUT.resolve("pilot.png").toFile());
        Files.writeString(OUT.resolve("pilot.atlas"),INDEX);
        System.out.println("PILOT_ATLAS_OK: 53 named regions, 1024x512 RGBA");
    }
    static void actor(String source,String name,int offset)throws Exception {
        BufferedImage image=ImageIO.read(SOURCE.resolve(source+".png").toFile());
        Rectangle[] bounds=new Rectangle[16];int maxW=1,maxH=1;
        for(int i=0;i<16;i++) {
            int x=i%4*image.getWidth()/4,y=i/4*image.getHeight()/4,w=image.getWidth()/4,h=image.getHeight()/4;
            int left=w,top=h,right=0,bottom=0;
            for(int py=0;py<h;py++)for(int px=0;px<w;px++)if((image.getRGB(x+px,y+py)>>>24)>=96) {
                left=Math.min(left,px);top=Math.min(top,py);right=Math.max(right,px+1);bottom=Math.max(bottom,py+1);
            }
            if(right<=left||bottom<=top)throw new IllegalStateException("Empty actor cell "+source+" "+i);
            bounds[i]=new Rectangle(x+left,y+top,right-left,bottom-top);
            maxW=Math.max(maxW,right-left);maxH=Math.max(maxH,bottom-top);
        }
        float scale=Math.min(28f/maxW,28f/maxH);
        String[] dirs={"down","up","right","left"};
        for(int i=0;i<16;i++) {
            Rectangle b=bounds[i];int width=Math.max(1,Math.round(b.width*scale)),height=Math.max(1,Math.round(b.height*scale));
            int x=offset+i%4*32,y=i/4*32;
            G.drawImage(image,x+(32-width)/2,y+30-height,x+(32-width)/2+width,y+30,b.x,b.y,b.x+b.width,b.y+b.height,null);
            region(name+"-"+dirs[i/4]+"-"+(i%4),x,y,32,32);
        }
    }
    static void copy(BufferedImage image,int sx,int sy,int sw,int sh,int x,int y,int w,int h,String name) {
        G.drawImage(image,x,y,x+w,y+h,sx,sy,sx+sw,sy+sh,null);region(name,x,y,w,h);
    }
    static void region(String name,int x,int y,int width,int height) {
        INDEX.append(name+"\n  rotate: false\n  xy: "+x+", "+y+"\n  size: "+width+", "+height
            +"\n  orig: "+width+", "+height+"\n  offset: 0, 0\n  index: -1\n");
    }
}

