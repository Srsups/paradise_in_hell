import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;

/** Technical crop/scale/packing only; scenario artwork is generated with image_gen. */
class SceneryAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/m6-scenery"),OUT=Path.of("core/assets/visual");
    static final String[] THEMES={"refuge","styx","ashes","shadows","guardian"};
    static final LinkedHashMap<String,BufferedImage> TILES=new LinkedHashMap<>();
    static final StringBuilder REGISTRATION=new StringBuilder("region,source,source_x,source_y,source_width,source_height,target_width,target_height\n");
    static final LinkedHashMap<String,String> SOURCE_META=new LinkedHashMap<>();
    static int passageColumns,passageRows;
    static boolean refined=true;
    static String hash(Path file)throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
    static void references()throws Exception {
        Files.createDirectories(SOURCE);BufferedImage old=ImageIO.read(Path.of("core/assets/demo-atlas.png").toFile());
        int y=old.getHeight()/2,right=old.getWidth()*3/4,bottom=old.getHeight()*3/4;
        ImageIO.write(old.getSubimage(0,y,right,bottom-y),"png",SOURCE.resolve("reference-floors.png").toFile());
        System.out.println("SCENERY_REFERENCE_OK: existing floor row cropped without repainting");
    }
    static BufferedImage read(String filename,boolean opaque)throws Exception {
        Path file=SOURCE.resolve(filename);BufferedImage image=ImageIO.read(file.toFile());
        if(image==null)throw new IllegalStateException("Unreadable scenery source "+filename);
        if(opaque){
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
                if((image.getRGB(x,y)>>>24)!=255)throw new IllegalStateException("Scenery must be opaque: "+filename+" @"+x+","+y);
        } else if(!image.getColorModel().hasAlpha()||(image.getRGB(0,0)>>>24)>16)
            throw new IllegalStateException("Passage needs real transparent RGBA");
        SOURCE_META.put(filename,"{\"size\": ["+image.getWidth()+", "+image.getHeight()+"], \"opaque\": "+opaque+", \"sha256\": \""+hash(file)+"\"}");
        return image;
    }
    static Rectangle cell(BufferedImage image,int columns,int rows,int column,int row){
        int x=column*image.getWidth()/columns,y=row*image.getHeight()/rows;
        return new Rectangle(x,y,(column+1)*image.getWidth()/columns-x,(row+1)*image.getHeight()/rows-y);
    }
    static BufferedImage crop(BufferedImage source,Rectangle area,int width,int height){
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source,0,0,width,height,area.x,area.y,area.x+area.width,area.y+area.height,null);g.dispose();return image;
    }
    static void log(String key,String filename,Rectangle area,int width,int height){
        REGISTRATION.append(key+","+filename+","+area.x+","+area.y+","+area.width+","+area.height+","+width+","+height+"\n");
    }
    static void theme(String name)throws Exception {
        String filename=name+(refined?"-tiles-refined.png":"-tiles-original.png");BufferedImage image=read(filename,true);
        if(Math.abs(image.getWidth()/(double)image.getHeight()-1)>.04)throw new IllegalStateException("Theme sheet must be square "+name);
        for(int row=0;row<3;row++)for(int column=0;column<3;column++){
            String key=name+(row==1?"-obstacle-":"-floor-")+(row==2?column+3:column);
            Rectangle area=cell(image,3,3,column,row);TILES.put(key,crop(image,area,64,64));log(key,filename,area,64,64);
        }
    }
    static void preview(String name)throws Exception {
        String key="route-"+name,filename=key+"-original.png";BufferedImage image=read(filename,true);
        int width=image.getWidth(),height=image.getHeight();
        if(width/(double)height>16d/9)width=(int)Math.round(height*16d/9);else height=(int)Math.round(width*9d/16);
        Rectangle area=new Rectangle((image.getWidth()-width)/2,(image.getHeight()-height)/2,width,height);
        TILES.put(key,crop(image,area,320,180));log(key,filename,area,320,180);
    }
    static Rectangle alpha(BufferedImage image,Rectangle area){
        int left=area.width,top=area.height,right=0,bottom=0;
        for(int y=0;y<area.height;y++)for(int x=0;x<area.width;x++)if((image.getRGB(area.x+x,area.y+y)>>>24)>=96){
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);
        }
        if(right<=left||bottom<=top)throw new IllegalStateException("Empty passage frame");
        return new Rectangle(left,top,right-left,bottom-top);
    }
    static void passage()throws Exception {
        String filename="passage-original.png";BufferedImage image=read(filename,false);double aspect=image.getWidth()/(double)image.getHeight();
        passageColumns=aspect>2.5?4:aspect<.4?1:2;passageRows=4/passageColumns;
        Rectangle[] areas=new Rectangle[4],bodies=new Rectangle[4];double maximumWidth=0,maximumHeight=0;
        for(int frame=0;frame<4;frame++){
            areas[frame]=cell(image,passageColumns,passageRows,frame%passageColumns,frame/passageColumns);
            bodies[frame]=alpha(image,areas[frame]);maximumWidth=Math.max(maximumWidth,bodies[frame].width);maximumHeight=Math.max(maximumHeight,bodies[frame].height);
        }
        double scale=88/Math.max(maximumWidth,maximumHeight);
        Rectangle reference=areas[0],body=bodies[0];double center=(body.x+body.width*.5)/reference.width,bottom=(body.y+body.height)/(double)reference.height;
        for(int frame=0;frame<4;frame++){
            Rectangle area=areas[frame];int x=(int)Math.round(48-center*area.width*scale),y=(int)Math.round(92-bottom*area.height*scale);
            BufferedImage tile=new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();
            g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(image,x,y,x+(int)Math.round(area.width*scale),y+(int)Math.round(area.height*scale),area.x,area.y,area.x+area.width,area.y+area.height,null);g.dispose();
            String key="passage-"+frame;TILES.put(key,tile);log(key,filename,area,96,96);
        }
    }
    static void pack()throws Exception {
        if(TILES.size()!=52)throw new IllegalStateException("Expected52sceneryregions, found "+TILES.size());
        BufferedImage page=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);Graphics2D g=page.createGraphics();g.setComposite(AlphaComposite.Src);
        StringBuilder atlas=new StringBuilder("m6-scenery.png\nsize: 1024, 1024\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n");int x=1,y=1,rowHeight=0;
        for(var entry:TILES.entrySet()){
            BufferedImage tile=entry.getValue();int width=tile.getWidth(),height=tile.getHeight();
            if(x+width+1>1024){x=1;y+=rowHeight+2;rowHeight=0;}
            if(y+height+1>1024)throw new IllegalStateException("Scenery atlas overflow");
            g.drawImage(tile,x,y,null);atlas.append(entry.getKey()+"\n  rotate: false\n  xy: "+x+", "+y+"\n  size: "+width+", "+height+"\n  orig: "+width+", "+height+"\n  offset: 0, 0\n  index: -1\n");
            x+=width+2;rowHeight=Math.max(rowHeight,height);
        }
        g.dispose();Files.createDirectories(OUT);ImageIO.write(page,"png",OUT.resolve("m6-scenery.png").toFile());Files.writeString(OUT.resolve("m6-scenery.atlas"),atlas);
        Files.writeString(SOURCE.resolve("registration.csv"),REGISTRATION);
        StringBuilder manifest=new StringBuilder("{\n  \"date\": \"2026-10-25\",\n  \"tool\": \"built-in image_gen\",\n  \"prompts\": [\"prompts.json\", \"prompts-revisions.json\"],\n  \"contract\": \"contract.json\",\n  \"selectedAssets\": 9,\n  \"generationCalls\": "+(refined?14:9)+",\n  \"selection\": \""+(refined?"refined theme palettes and raised block silhouettes; original previews and passage":"original review version")+"\",\n  \"selectedSources\": {\n");int index=0;
        for(var entry:SOURCE_META.entrySet())manifest.append("    \""+entry.getKey()+"\": "+entry.getValue()+(++index<SOURCE_META.size()?",":"")+"\n");
        manifest.append("  },\n  \"retainedOriginalThemeSources\": {\n");
        for(int i=0;i<THEMES.length;i++){String filename=THEMES[i]+"-tiles-original.png";manifest.append("    \""+filename+"\": {\"sha256\": \""+hash(SOURCE.resolve(filename))+"\"}"+(i+1<THEMES.length?",":"")+"\n");}
        manifest.append("  },\n  \"themeGrid\": [3, 3],\n  \"themeRows\": [\"floor0/1/2\", \"obstacle0/1/2\", \"floor3/4/5\"],\n  \"passageSourceGrid\": ["+passageColumns+", "+passageRows+"],\n"+
            "  \"previewCrop\": \"center crop to16:9, then nearest-neighbor reduction to320x180\",\n  \"operations\": [\"crop\", \"uniform passage pivot registration\", \"nearest-neighbor reduction\", \"atlas packing\"],\n"+
            "  \"preserveGeneratedAlphaAndColors\": true,\n  \"paintOrRecolor\": false,\n  \"registration\": \"registration.csv\",\n"+
            "  \"packed\": {\"atlas\": \"../../../core/assets/visual/m6-scenery.atlas\", \"page\": \"../../../core/assets/visual/m6-scenery.png\", \"regions\": 52, \"dimensions\": [1024, 1024], \"rgbaBytes\": 4194304, \"sha256\": \""+hash(OUT.resolve("m6-scenery.png"))+"\"},\n"+
            "  \"reference\": {\"source\": \"../../../core/assets/demo-atlas.png\", \"technicalCrop\": \"reference-floors.png\", \"note\": \"Only existing stone floor row, cropped without repainting; not a new generation\"},\n"+
            "  \"tooling\": \"../../SceneryAtlasBaker.java\"\n}\n");Files.writeString(SOURCE.resolve("manifest.json"),manifest);
        review();
        System.out.println("SCENERY_ATLAS_OK: 52regions,1024x1024,4194304RGBAbytes");
    }
    static void review()throws Exception {
        Files.createDirectories(Path.of("build/smoke"));BufferedImage tiling=new BufferedImage(6*64,5*128,BufferedImage.TYPE_INT_ARGB);Graphics2D p=tiling.createGraphics();
        for(int theme=0;theme<5;theme++)for(int base=0;base<3;base++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)
            p.drawImage(TILES.get(THEMES[theme]+"-floor-"+base),(base*2+dx)*64,theme*128+dy*64,null);
        String version=refined?"":"-v1";
        p.dispose();
        ImageIO.write(tiling,"png",Path.of("build/smoke/m6-scenery-tiling"+version+".png").toFile());
        BufferedImage catalog=new BufferedImage(9*66,5*106,BufferedImage.TYPE_INT_ARGB);Graphics2D c=catalog.createGraphics();
        c.setComposite(AlphaComposite.Src);c.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(int theme=0;theme<5;theme++)for(int row=0;row<3;row++)for(int column=0;column<3;column++){
            String key=THEMES[theme]+(row==1?"-obstacle-":"-floor-")+(row==2?column+3:column);
            BufferedImage tile=TILES.get(key);int x=(row*3+column)*66,y=theme*106;
            c.drawImage(tile,x,y,null);BufferedImage mini=crop(tile,new Rectangle(0,0,64,64),16,16);
            c.drawImage(mini,x,y+68,null);c.drawImage(mini,x+20,y+68,32,32,null);
        }
        c.dispose();ImageIO.write(catalog,"png",Path.of("build/smoke/m6-scenery-catalog"+version+".png").toFile());
        System.out.println("SCENERY_REVIEW_OK: five themes, native64 and reduced16; floor tiling at64");
    }
    public static void main(String[] args)throws Exception {
        if(args.length>0&&args[0].equals("references")){references();return;}
        refined=!Arrays.asList(args).contains("--original");
        for(String theme:THEMES)theme(theme);
        if(Arrays.asList(args).contains("--review")){review();return;}
        for(String route:new String[]{"ashes","shadows","guardian"})preview(route);passage();pack();
    }
}
