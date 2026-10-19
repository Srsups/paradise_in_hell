import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Offline packing only. Source PNGs retain their generated art and alpha channel. */
class ExpandedAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/expansion"), OUT=Path.of("core/assets/visual");
    static final BufferedImage PAGE=new BufferedImage(1024,2048,BufferedImage.TYPE_INT_ARGB);
    static final Graphics2D G=PAGE.createGraphics();
    static final StringBuilder REGIONS=new StringBuilder();
    static int nextX=2,nextY=2,rowHeight,count;
    static final String[] DIRS={"down","up","right","left"};
    public static void main(String[] args)throws Exception {
        G.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(String name:new String[]{"cacador","ferreiro","vigilante","apostador","feiticeira","sacerdote","necromante","guerreiro"})
            actor(name,"class-"+name,6,32);
        for(String name:new String[]{"common","fast","tank","archer","carrasco","guardian"})
            actor(name,name,4,name.equals("guardian")||name.equals("carrasco")?64:32);
        sequences("weapon-fx",new String[]{"attack-bow","attack-sword","attack-pistol","attack-fists","attack-wand","attack-curse","attack-soul","attack-shield"});
        sequences("boon-fx",new String[]{"water","aegis","lightning","fire","quake","resurrect","heal","magnet"});
        sequences("chests",new String[]{"chest-normal","chest-special"});
        icons("weapons",4,2,new String[]{"weapon-bow","weapon-sword","weapon-pistol","weapon-fists","weapon-wand","weapon-curse","weapon-soul","weapon-shield"});
        icons("boon-icons",4,4,new String[]{"boon-forca","boon-cadencia","boon-vida","boon-velocidade","boon-resistencia","boon-inteligencia",
            "boon-magnetismo","boon-cura","boon-vampirismo","boon-egide","boon-poseidon","boon-choque","boon-queimadura","boon-impacto","boon-ressurreicao","boon-currency"});
        BufferedImage portrait=read("protagonist");
        Rectangle slot=slot(512,512);G.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        G.drawImage(portrait,slot.x,slot.y,512,512,null);region("portrait-protagonist",slot);
        G.dispose();int height=1;while(height<nextY+rowHeight+2)height*=2;
        Files.createDirectories(OUT);ImageIO.write(PAGE.getSubimage(0,0,1024,height),"png",OUT.resolve("expanded.png").toFile());
        Files.writeString(OUT.resolve("expanded.atlas"),"expanded.png\nsize: 1024, "+height+"\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n"+REGIONS);
        System.out.println("EXPANDED_ATLAS_OK: "+count+" regions, 1024x"+height);
    }
    static BufferedImage read(String name)throws Exception {
        BufferedImage result=ImageIO.read(SOURCE.resolve(name+"-original.png").toFile());
        if(result==null||!result.getColorModel().hasAlpha())throw new IllegalStateException("Missing alpha: "+name);
        return result;
    }
    static Rectangle cell(BufferedImage image,int cols,int rows,int col,int row) {
        int x=col*image.getWidth()/cols,y=row*image.getHeight()/rows;
        return new Rectangle(x,y,(col+1)*image.getWidth()/cols-x,(row+1)*image.getHeight()/rows-y);
    }
    static Rectangle bounds(BufferedImage image,Rectangle cell,int alpha) {
        int left=cell.width,top=cell.height,right=0,bottom=0;
        for(int y=0;y<cell.height;y++)for(int x=0;x<cell.width;x++)if((image.getRGB(cell.x+x,cell.y+y)>>>24)>=alpha) {
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);
        }
        if(right<=left||bottom<=top)throw new IllegalStateException("Empty sprite cell");
        return new Rectangle(left,top,right-left,bottom-top);
    }
    static void actor(String input,String name,int cols,int size)throws Exception {
        BufferedImage image=read(input);Rectangle[] boxes=new Rectangle[cols*4];int maxW=1,maxH=1;
        for(int i=0;i<boxes.length;i++) {
            Rectangle c=cell(image,cols,4,i%cols,i/cols),b=bounds(image,c,96);
            if(b.width>=c.width*.98f&&b.height>=c.height*.98f)throw new IllegalStateException("Opaque background: "+input);
            boxes[i]=new Rectangle(c.x+b.x,c.y+b.y,b.width,b.height);maxW=Math.max(maxW,b.width);maxH=Math.max(maxH,b.height);
        }
        float scale=Math.min((size-4f)/maxW,(size-4f)/maxH);
        for(int i=0;i<boxes.length;i++) {
            Rectangle b=boxes[i],s=slot(size,size);int w=Math.max(1,Math.round(b.width*scale)),h=Math.max(1,Math.round(b.height*scale));
            int x=s.x+(size-w)/2,y=s.y+size-2-h;
            G.drawImage(image,x,y,x+w,y+h,b.x,b.y,b.x+b.width,b.y+b.height,null);
            region(name+"-"+DIRS[i/cols]+"-"+i%cols,s);
        }
    }
    static void sequences(String input,String[] names)throws Exception {
        BufferedImage image=read(input);
        for(int row=0;row<names.length;row++) {
            Rectangle union=null;
            for(int col=0;col<4;col++) {
                Rectangle c=cell(image,4,names.length,col,row),b=bounds(image,c,12);
                union=union==null?b:union.union(b);
            }
            for(int col=0;col<4;col++) {
                Rectangle c=cell(image,4,names.length,col,row);
                fitted(image,new Rectangle(c.x+union.x,c.y+union.y,union.width,union.height),64,names[row]+"-"+col);
            }
        }
    }
    static void icons(String input,int cols,int rows,String[] names)throws Exception {
        BufferedImage image=read(input);
        if(input.equals("boon-icons")) {
            // Generated frames have small grid offsets. Extract each opaque framed tile as one component.
            java.util.List<Rectangle> tiles=components(image);
            tiles.sort(java.util.Comparator.comparingInt((Rectangle r)->r.width*r.height).reversed());
            if(tiles.size()<names.length)throw new IllegalStateException("Missing icon components");
            tiles=new java.util.ArrayList<>(tiles.subList(0,names.length));
            tiles.sort(java.util.Comparator.comparingDouble(Rectangle::getCenterY));
            for(int row=0;row<rows;row++) {
                var line=tiles.subList(row*cols,(row+1)*cols);
                line.sort(java.util.Comparator.comparingDouble(Rectangle::getCenterX));
                for(int col=0;col<cols;col++)fitted(image,line.get(col),64,names[row*cols+col]);
            }
            return;
        }
        for(int i=0;i<names.length;i++) {
            Rectangle c=cell(image,cols,rows,i%cols,i/cols),b=bounds(image,c,24);
            fitted(image,new Rectangle(c.x+b.x,c.y+b.y,b.width,b.height),64,names[i]);
        }
    }
    static java.util.List<Rectangle> components(BufferedImage image) {
        int w=image.getWidth(),h=image.getHeight();boolean[] seen=new boolean[w*h];int[] queue=new int[w*h];
        var result=new java.util.ArrayList<Rectangle>();
        for(int start=0;start<seen.length;start++) {
            if(seen[start]||(image.getRGB(start%w,start/w)>>>24)<96)continue;
            int head=0,tail=0,minX=w,minY=h,maxX=0,maxY=0;queue[tail++]=start;seen[start]=true;
            while(head<tail) {
                int p=queue[head++],x=p%w,y=p/w;
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                for(int direction=0;direction<4;direction++) {
                    int nx=x+(direction==0?-1:direction==1?1:0),ny=y+(direction==2?-1:direction==3?1:0);
                    if(nx<0||nx>=w||ny<0||ny>=h)continue;int next=ny*w+nx;
                    if(!seen[next]&&(image.getRGB(nx,ny)>>>24)>=96){seen[next]=true;queue[tail++]=next;}
                }
            }
            if(tail>200)result.add(new Rectangle(minX,minY,maxX-minX+1,maxY-minY+1));
        }
        return result;
    }
    static void fitted(BufferedImage image,Rectangle b,int size,String name) {
        Rectangle s=slot(size,size);float scale=Math.min((size-4f)/b.width,(size-4f)/b.height);
        int w=Math.max(1,Math.round(b.width*scale)),h=Math.max(1,Math.round(b.height*scale));
        G.drawImage(image,s.x+(size-w)/2,s.y+(size-h)/2,s.x+(size-w)/2+w,s.y+(size-h)/2+h,b.x,b.y,b.x+b.width,b.y+b.height,null);
        region(name,s);
    }
    static Rectangle slot(int width,int height) {
        if(nextX+width+2>1024){nextX=2;nextY+=rowHeight+2;rowHeight=0;}
        if(nextY+height+2>2048)throw new IllegalStateException("Atlas overflow");
        Rectangle result=new Rectangle(nextX,nextY,width,height);nextX+=width+2;rowHeight=Math.max(rowHeight,height);return result;
    }
    static void region(String name,Rectangle r) {
        count++;REGIONS.append(name+"\n  rotate: false\n  xy: "+r.x+", "+r.y+"\n  size: "+r.width+", "+r.height
            +"\n  orig: "+r.width+", "+r.height+"\n  offset: 0, 0\n  index: -1\n");
    }
}

