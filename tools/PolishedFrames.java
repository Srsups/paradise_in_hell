import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;

/** Technical cropping, registration and review only. PNG drawings are generated, never painted here. */
class PolishedFrames {
    static final Path SOURCE=Path.of("tools/art-sources/polish");
    static final String[] DIRS={"down","up","left","up-left","down-left"};
    static final Map<String,BufferedImage[]> FRAMES=new LinkedHashMap<>();
    static final StringBuilder LOG=new StringBuilder("clip,direction,frame,shift_x,shift_y,score,source_column\n");
    static class Sheet {
        final BufferedImage image;final int[] xs,ys;
        Sheet(String name,int cols)throws Exception{
            image=ImageIO.read(SOURCE.resolve(name+"-original.png").toFile());
            if(image==null||!image.getColorModel().hasAlpha())throw new IllegalStateException("Missing transparency: "+name);
            if((image.getRGB(0,0)>>>24)>16)throw new IllegalStateException("Opaque background: "+name);
            xs=EquippedAtlasBaker.cuts(image,cols,false);ys=EquippedAtlasBaker.cuts(image,5,true);
        }
        Rectangle cell(int col,int row){return EquippedAtlasBaker.crop(xs,ys,col,row);}
        Rectangle body(Rectangle c){return EquippedAtlasBaker.alpha(image,c,96,true);}
        double scale(int columns){
            double[] heights=new double[columns*5];int i=0;
            for(int r=0;r<5;r++)for(int c=0;c<columns;c++)heights[i++]=body(cell(c,r)).height;
            Arrays.sort(heights);return 42/heights[heights.length/2];
        }
        BufferedImage tile(int col,int row,double scale){
            Rectangle c=cell(col,row),b=body(c),first=cell(0,row),base=body(first);
            double sum=0,pixels=0;int bottom=b.y+Math.max(2,(int)(b.height*.25));
            for(int y=b.y;y<bottom;y++)for(int x=(int)(c.width*.30);x<c.width*.70;x++)
                if((image.getRGB(c.x+x,c.y+y)>>>24)>=96){sum+=x;pixels++;}
            double center=pixels>0?sum/pixels:c.width*.5;
            return render(image,c,scale,(int)Math.round(32-center*scale),(int)Math.round(55-(first.y+base.y+base.height-c.y)*scale));
        }
    }
    static BufferedImage render(BufferedImage source,Rectangle c,double scale,int x,int y){
        BufferedImage tile=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source,x,y,x+(int)Math.round(c.width*scale),y+(int)Math.round(c.height*scale),c.x,c.y,c.x+c.width,c.y+c.height,null);
        g.dispose();return tile;
    }
    record Aligned(BufferedImage tile,int dx,int dy,double score){}
    /** Match head and upper torso. Legs and weapons cannot pull the body pivot sideways. */
    static Aligned align(BufferedImage reference,BufferedImage candidate){
        int bestX=0,bestY=0;double best=Double.POSITIVE_INFINITY;
        for(int dy=-8;dy<=8;dy++)for(int dx=-8;dx<=8;dx++){
            double score=0,weight=0;
            for(int y=13;y<30;y++)for(int x=24;x<41;x++){
                int a=reference.getRGB(x,y),b=candidate.getRGB(Math.max(0,Math.min(63,x-dx)),Math.max(0,Math.min(63,y-dy)));
                boolean aa=(a>>>24)>=96,bb=(b>>>24)>=96;if(!aa&&!bb)continue;
                double w=y<24?2:1;
                score+=w*(aa!=bb?260:Math.abs((a>>16&255)-(b>>16&255))*.35+Math.abs((a>>8&255)-(b>>8&255))*.45+Math.abs((a&255)-(b&255))*.20);weight+=w;
            }
            score=score/Math.max(1,weight)+.16*(Math.abs(dx)+Math.abs(dy));
            if(score<best){best=score;bestX=dx;bestY=dy;}
        }
        BufferedImage result=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D g=result.createGraphics();
        g.drawImage(candidate,bestX,bestY,null);g.dispose();return new Aligned(result,bestX,bestY,best);
    }
    static void hero(String name)throws Exception{
        Sheet walk=new Sheet(name+"-walk",6),actions=new Sheet(name+"-actions",8);
        double walkScale=walk.scale(6),actionScale=actions.scale(4);
        for(int r=0;r<5;r++){
            BufferedImage reference=walk.tile(0,r,walkScale);
            for(int c=0;c<6;c++){
                // Reject two authored SW frames that swap the priest's talisman hand.
                // Reuse the four coherent gait poses; never repaint or mirror body parts.
                int source=name.equals("sacerdote")&&r==4?new int[]{0,1,4,5,4,1}[c]:c;
                registered(name+"-walk",r,c,6,reference,walk.tile(source,r,walkScale),source);
            }
            for(int c=0;c<4;c++){
                registered(name+"-idle",r,c,4,reference,actions.tile(c,r,actionScale));
                registered(name+"-attack",r,c,4,reference,actions.tile(c+4,r,actionScale));
            }
        }
    }
    static void registered(String clip,int row,int frame,int count,BufferedImage reference,BufferedImage candidate){
        registered(clip,row,frame,count,reference,candidate,frame);
    }
    static void registered(String clip,int row,int frame,int count,BufferedImage reference,BufferedImage candidate,int source){
        Aligned a=align(reference,candidate);put(clip+"-"+DIRS[row]+"-"+frame,a.tile);
        FRAMES.computeIfAbsent(clip,k->new BufferedImage[5*count])[row*count+frame]=a.tile;
        LOG.append(clip+","+DIRS[row]+","+frame+","+a.dx+","+a.dy+","+String.format(Locale.ROOT,"%.3f",a.score)+","+source+"\n");
    }
    static void cast()throws Exception{
        Sheet sheet=new Sheet("guardian-cast",6);double scale=sheet.scale(6);
        for(int r=0;r<5;r++)for(int c=0;c<6;c++)registered("guardian-cast",r,c,6,sheet.tile(0,r,scale),sheet.tile(c,r,scale));
    }
    static void prop(String name)throws Exception{
        BufferedImage image=ImageIO.read(SOURCE.resolve(name+"-original.png").toFile());
        if(image==null||!image.getColorModel().hasAlpha()||(image.getRGB(0,0)>>>24)>16)throw new IllegalStateException("Missing transparency: "+name);
        int[] xs=EquippedAtlasBaker.cuts(image,2,false),ys=EquippedAtlasBaker.cuts(image,2,true);
        Rectangle[] cells=new Rectangle[4],bounds=new Rectangle[4];double width=0,height=0;
        for(int f=0;f<4;f++){
            cells[f]=EquippedAtlasBaker.crop(xs,ys,f%2,f/2);bounds[f]=EquippedAtlasBaker.alpha(image,cells[f],32,false);
            width=Math.max(width,bounds[f].width);height=Math.max(height,bounds[f].height);
        }
        double scale=60/Math.max(width,height);
        for(int f=0;f<4;f++){
            Rectangle b=bounds[f],c=cells[f];
            put(name+"-"+f,render(image,c,scale,(int)Math.round(32-(b.x+b.width*.5)*scale),(int)Math.round(62-(b.y+b.height)*scale)));
        }
    }
    static void put(String name,BufferedImage tile){
        Rectangle slot=EquippedAtlasBaker.slot();EquippedAtlasBaker.page.g.drawImage(tile,slot.x,slot.y,null);EquippedAtlasBaker.region(name,slot);
    }
    static void previews()throws Exception{
        Files.createDirectories(Path.of("build/smoke"));Files.writeString(Path.of("build/smoke/polish-registration.csv"),LOG);
        gif("walk",6,14);gif("idle",4,40);gif("attack",4,12);
    }
    static void gif(String action,int count,int delay)throws Exception{
        var writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(var out=ImageIO.createImageOutputStream(Path.of("build/smoke/polish-"+action+".gif").toFile())){
            writer.setOutput(out);writer.prepareWriteSequence(null);
            for(int f=0;f<count;f++){
                BufferedImage image=new BufferedImage(720,752,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                g.setColor(new Color(16,22,33));g.fillRect(0,0,720,752);g.setFont(new Font("SansSerif",Font.PLAIN,12));
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int row=0;
                for(var e:FRAMES.entrySet())if(e.getKey().endsWith("-"+action)&&!e.getKey().startsWith("guardian")){
                    g.setColor(new Color(220,204,152));g.drawString(e.getKey(),4,row*94+52);
                    for(int d=0;d<5;d++)g.drawImage(e.getValue()[d*count+f],150+d*112,row*94,92,92,null);
                    row++;
                }
                g.dispose();var type=javax.imageio.ImageTypeSpecifier.createFromRenderedImage(image);
                var metadata=writer.getDefaultImageMetadata(type,writer.getDefaultWriteParam());
                var root=(IIOMetadataNode)metadata.getAsTree(metadata.getNativeMetadataFormatName());
                var control=new IIOMetadataNode("GraphicControlExtension");
                control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");
                control.setAttribute("delayTime",String.valueOf(delay));control.setAttribute("transparentColorIndex","0");root.appendChild(control);
                if(f==0){
                    IIOMetadataNode extensions=new IIOMetadataNode("ApplicationExtensions"),loop=new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");loop.setUserObject(new byte[]{1,0,0});extensions.appendChild(loop);root.appendChild(extensions);
                }
                metadata.setFromTree(metadata.getNativeMetadataFormatName(),root);
                writer.writeToSequence(new javax.imageio.IIOImage(image,null,metadata),writer.getDefaultWriteParam());
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
    }
}
