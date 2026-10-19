import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Offline technical crop/scale/packing only; all drawings come from the generated PNGs. */
class EquippedAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/equipped"),OUT=Path.of("core/assets/visual");
    static final String[] DIRS={"down","up","right","left","down-right","down-left","up-right","up-left"};
    static final java.util.List<Page> PAGES=new ArrayList<>();
    static Page page=new Page();
    static int count;
    static class Page {
        BufferedImage image=new BufferedImage(2048,2048,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        StringBuilder regions=new StringBuilder();
        int x=1,y=1,rowHeight;
        Page(){g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);PAGES.add(this);}
    }
    public static void main(String[] args)throws Exception {
        for(String hero:new String[]{"cacador","ferreiro","vigilante","apostador","feiticeira","sacerdote","necromante","guerreiro"}){
            PolishedFrames.hero(hero);
        }
        actor("archer-walk",4,8);actor("archer-attack",6,8);
        actor("carrasco-walk",4,8);actor("carrasco-spin",6,4);actor("carrasco-stones",6,4);
        actor("guardian-walk",6,8);actor("guardian-idle",4,8);actor("guardian-charge",6,8);
        PolishedFrames.cast();actor("guardian-land",6,4);actor("guardian-jump",6,4);
        for(String prop:new String[]{"refuge-charon","refuge-altar","refuge-crossing"})PolishedFrames.prop(prop);
        effects("tactical-fx",new String[]{"archer-target","archer-lane","mini-spin-warning","stone-fan","boss-mark","charge-lane","leap-target","landing-impact"});
        Files.createDirectories(OUT);StringBuilder atlas=new StringBuilder();int bytes=0;
        for(int i=0;i<PAGES.size();i++){
            Page p=PAGES.get(i);p.g.dispose();int height=1;while(height<p.y+p.rowHeight+1)height*=2;
            String name="equipped"+(i==0?"":"-"+(i+1))+".png";
            ImageIO.write(p.image.getSubimage(0,0,2048,height),"png",OUT.resolve(name).toFile());
            if(i>0)atlas.append("\n");
            atlas.append(name+"\nsize: 2048, "+height+"\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n"+p.regions);
            bytes+=2048*height*4;
        }
        Files.writeString(OUT.resolve("equipped.atlas"),atlas);
        PolishedFrames.previews();
        System.out.println("EQUIPPED_ATLAS_OK: "+count+" regions, "+PAGES.size()+" pages, "+bytes+" RGBA bytes");
    }
    static BufferedImage read(String name)throws Exception{
        BufferedImage image=ImageIO.read(SOURCE.resolve(name+"-original.png").toFile());
        if(image==null||!image.getColorModel().hasAlpha())throw new IllegalStateException("Missing alpha "+name);
        return image;
    }
    static Rectangle cell(BufferedImage image,int cols,int rows,int c,int r){
        int x=c*image.getWidth()/cols,y=r*image.getHeight()/rows;
        return new Rectangle(x,y,(c+1)*image.getWidth()/cols-x,(r+1)*image.getHeight()/rows-y);
    }
    static Rectangle alpha(BufferedImage image,Rectangle c,int threshold,boolean torso){
        int minX=c.width,minY=c.height,maxX=0,maxY=0;
        for(int y=0;y<c.height;y++)for(int x=0;x<c.width;x++){
            if(torso&&(x<c.width*.36||x>=c.width*.64))continue;
            if((image.getRGB(c.x+x,c.y+y)>>>24)>=threshold){
                minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x+1);maxY=Math.max(maxY,y+1);
            }
        }
        if(maxX<=minX||maxY<=minY)throw new IllegalStateException("Empty cell "+c);
        return new Rectangle(minX,minY,maxX-minX,maxY-minY);
    }
    /** Locate low-alpha gutters near grid boundaries; generated sheets are not always evenly spaced. */
    static int[] cuts(BufferedImage image,int count,boolean vertical){
        int length=vertical?image.getHeight():image.getWidth(),cross=vertical?image.getWidth():image.getHeight();
        int[] density=new int[length],result=new int[count+1];result[count]=length;
        for(int p=0;p<length;p++)for(int q=0;q<cross;q++)
            if((image.getRGB(vertical?q:p,vertical?p:q)>>>24)>=96)density[p]++;
        double cell=length/(double)count;
        for(int i=1;i<count;i++){
            double expected=i*cell;int start=Math.max(result[i-1]+1,(int)Math.ceil(expected-cell*.49));
            int end=Math.min(length-1,(int)Math.floor(expected+cell*.49)),best=(int)Math.round(expected),score=Integer.MAX_VALUE;
            for(int p=start;p<=end;p++){
                int value=density[p];
                if(value<score||value==score&&Math.abs(p-expected)<Math.abs(best-expected)){score=value;best=p;}
            }
            result[i]=best;
        }
        return result;
    }
    static Rectangle crop(int[] xs,int[] ys,int c,int r){
        return new Rectangle(xs[c],ys[r],xs[c+1]-xs[c],ys[r+1]-ys[r]);
    }
    static void actor(String name,int cols,int rows)throws Exception{
        BufferedImage image=read(name);int[] xs=cuts(image,cols,false),ys=cuts(image,rows,true);
        double bodyHeight=0;double[] feet=new double[rows];
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){
            Rectangle source=crop(xs,ys,col,row),body=alpha(image,source,96,true);
            bodyHeight=Math.max(bodyHeight,body.height);feet[row]=Math.max(feet[row],source.y+body.y+body.height);
        }
        double scale=64*.66/bodyHeight;
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){
            Rectangle source=crop(xs,ys,col,row),slot=slot();
            BufferedImage tile=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int w=(int)Math.round(source.width*scale),h=(int)Math.round(source.height*scale);
            double center=(col+.5)*image.getWidth()/cols;
            int x=(int)Math.round(32-(center-source.x)*scale),y=(int)Math.round(64*.86-(feet[row]-source.y)*scale);
            g.drawImage(image,x,y,x+w,y+h,source.x,source.y,source.x+source.width,source.y+source.height,null);g.dispose();
            page.g.drawImage(tile,slot.x,slot.y,null);region(name+"-"+DIRS[row]+"-"+col,slot);
        }
    }
    static void effects(String name,String[] names)throws Exception{
        BufferedImage image=read(name);int[] xs=cuts(image,8,false),ys=cuts(image,names.length,true);
        for(int row=0;row<names.length;row++){
            double left=1,top=1,right=0,bottom=0;
            for(int c=0;c<8;c++){
                Rectangle source=crop(xs,ys,c,row),b=alpha(image,source,12,false);
                left=Math.min(left,b.x/(double)source.width);top=Math.min(top,b.y/(double)source.height);
                right=Math.max(right,(b.x+b.width)/(double)source.width);bottom=Math.max(bottom,(b.y+b.height)/(double)source.height);
            }
            for(int c=0;c<8;c++){
                Rectangle source=crop(xs,ys,c,row),s=slot();
                page.g.drawImage(image,s.x+1,s.y+1,s.x+63,s.y+63,source.x+(int)(left*source.width),source.y+(int)(top*source.height),
                    source.x+(int)Math.ceil(right*source.width),source.y+(int)Math.ceil(bottom*source.height),null);
                region(names[row]+"-"+c,s);
            }
        }
    }
    static Rectangle slot(){
        if(page.x+65>2048){page.x=1;page.y+=page.rowHeight+2;page.rowHeight=0;}
        if(page.y+65>2048)page=new Page();
        Rectangle result=new Rectangle(page.x,page.y,64,64);page.x+=66;page.rowHeight=64;return result;
    }
    static void region(String name,Rectangle r){
        count++;page.regions.append(name+"\n  rotate: false\n  xy: "+r.x+", "+r.y+"\n  size: 64, 64\n  orig: 64, 64\n  offset: 0, 0\n  index: -1\n");
    }
}

