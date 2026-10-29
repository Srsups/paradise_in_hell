import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Technical crop/resize/packing of the generated M4 icons; never draws replacement artwork. */
class MilestoneFourAtlasBaker {
    static final String[] NAMES={"amparo","furor","folego","rastro","emboscada","convergencia","eco_do_estige","altar-folego"};
    public static void main(String[] args)throws Exception{
        BufferedImage source=ImageIO.read(Path.of("tools/art-sources/m4/boon-icons-original.png").toFile());
        if(source==null||!source.getColorModel().hasAlpha())throw new IllegalStateException("Missing icon alpha");
        BufferedImage page=new BufferedImage(256,128,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=page.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        StringBuilder atlas=new StringBuilder("m4.png\nsize: 256, 128\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n");
        for(int i=0;i<8;i++){
            int left=(i%4)*source.getWidth()/4,right=(i%4+1)*source.getWidth()/4;
            int top=(i/4)*source.getHeight()/2,bottom=(i/4+1)*source.getHeight()/2;
            int minX=right,minY=bottom,maxX=left,maxY=top;
            for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)if((source.getRGB(x,y)>>>24)>=96){
                minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x+1);maxY=Math.max(maxY,y+1);
            }
            if(minX>=maxX||minY>=maxY)throw new IllegalStateException("Empty icon "+NAMES[i]);
            int x=(i%4)*64+1,y=(i/4)*64+1;
            g.drawImage(source,x,y,x+62,y+62,minX,minY,maxX,maxY,null);
            atlas.append("boon-"+NAMES[i]+"\n  rotate: false\n  xy: "+x+", "+y+"\n  size: 62, 62\n  orig: 62, 62\n  offset: 0, 0\n  index: -1\n");
        }
        g.dispose();Path out=Path.of("core/assets/visual");Files.createDirectories(out);
        ImageIO.write(page,"png",out.resolve("m4.png").toFile());Files.writeString(out.resolve("m4.atlas"),atlas);
        System.out.println("M4_ATLAS_OK: 8 icons, 256x128, 131072 RGBA bytes");
    }
}
