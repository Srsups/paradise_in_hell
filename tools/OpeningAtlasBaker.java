import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;

/** Technical cropping, registration and packing only. All visible artwork comes from image_gen. */
class OpeningAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/opening"),OUT=Path.of("core/assets/cutscenes");
    static final LinkedHashMap<String,BufferedImage> REGIONS=new LinkedHashMap<>();
    static final LinkedHashMap<String,String> SOURCE_META=new LinkedHashMap<>();
    static final StringBuilder REGISTRATION=new StringBuilder("region,source,source_x,source_y,source_width,source_height,target_width,target_height,draw_x,draw_y,draw_width,draw_height,group_scale,pivot_x,pivot_y\n");
    static final int ACTOR_WIDTH=432,ACTOR_HEIGHT=480,ACTOR_PIVOT_X=216,ACTOR_PIVOT_Y=464;
    static final LinkedHashMap<String,Integer> POSE_SEAMS=new LinkedHashMap<>();
    static String hash(Path file)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
    static BufferedImage atlasRegion(String atlasName,String region)throws Exception {
        Path file=Path.of("core/assets/visual",atlasName+".atlas");var lines=Files.readAllLines(file);
        int index=lines.indexOf(region);if(index<0)throw new IllegalStateException("Missing reference region "+region);
        String xy=lines.get(index+2).split(":",2)[1].trim(),size=lines.get(index+3).split(":",2)[1].trim();
        String[] point=xy.split(","),dimensions=size.split(",");
        BufferedImage page=ImageIO.read(file.resolveSibling(atlasName+".png").toFile());
        return page.getSubimage(Integer.parseInt(point[0].trim()),Integer.parseInt(point[1].trim()),
            Integer.parseInt(dimensions[0].trim()),Integer.parseInt(dimensions[1].trim()));
    }
    static void references()throws Exception {
        Files.createDirectories(SOURCE);
        ImageIO.write(atlasRegion("expanded","portrait-protagonist"),"png",SOURCE.resolve("reference-protagonist.png").toFile());
        ImageIO.write(atlasRegion("m6-dialogue","portrait-charon-neutral"),"png",SOURCE.resolve("reference-charon.png").toFile());
        Files.copy(Path.of("tools/art-sources/m6-scenery/route-shadows-original.png"),SOURCE.resolve("reference-scenery.png"),StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(SOURCE.resolve("references.json"),"""
            {
              "operation": "technical crop/copy only; no repainting",
              "reference-protagonist.png": {"atlas": "../../../core/assets/visual/expanded.atlas", "region": "portrait-protagonist", "crop": [68,868,512,512]},
              "reference-charon.png": {"atlas": "../../../core/assets/visual/m6-dialogue.atlas", "region": "portrait-charon-neutral", "crop": [0,0,256,256]},
              "reference-scenery.png": {"source": "../m6-scenery/route-shadows-original.png", "operation": "copy"}
            }
            """);
        System.out.println("OPENING_REFERENCES_OK: protagonist, Charon and existing scenery; technical crop/copy only");
    }
    static BufferedImage read(String filename,boolean opaque)throws Exception {
        Path file=SOURCE.resolve(filename);BufferedImage source=ImageIO.read(file.toFile());
        if(source==null)throw new IllegalStateException("Unreadable opening source "+filename);
        long transparent=0,visible=0;int minimumAlpha=255,maximumAlpha=0;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
            int alpha=source.getRGB(x,y)>>>24;minimumAlpha=Math.min(minimumAlpha,alpha);maximumAlpha=Math.max(maximumAlpha,alpha);
            if(alpha==0)transparent++;if(alpha>=96)visible++;
            if(opaque && alpha!=255)throw new IllegalStateException("Opaque scene has transparency: "+filename+" @"+x+","+y);
        }
        if(!opaque && (!source.getColorModel().hasAlpha() || transparent==0 || visible==0))
            throw new IllegalStateException("Cutout needs real transparency and visible painted pixels: "+filename);
        SOURCE_META.put(filename,"{\"size\": ["+source.getWidth()+", "+source.getHeight()+"], \"opaque\": "+opaque
            +", \"minimumAlpha\": "+minimumAlpha+", \"maximumAlpha\": "+maximumAlpha+", \"sha256\": \""+hash(file)+"\"}");
        return source;
    }
    static Rectangle cell(BufferedImage source,int columns,int rows,int column,int row) {
        int x=column*source.getWidth()/columns,y=row*source.getHeight()/rows;
        return new Rectangle(x,y,(column+1)*source.getWidth()/columns-x,(row+1)*source.getHeight()/rows-y);
    }
    static BufferedImage transform(BufferedImage source,Rectangle area,int width,int height,int drawX,int drawY,int drawWidth,int drawHeight) {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source,drawX,drawY,drawX+drawWidth,drawY+drawHeight,
            area.x,area.y,area.x+area.width,area.y+area.height,null);g.dispose();return image;
    }
    static void log(String key,String filename,Rectangle area,int width,int height,int x,int y,int drawWidth,int drawHeight,double scale,double pivotX,double pivotY) {
        REGISTRATION.append(key+","+filename+","+area.x+","+area.y+","+area.width+","+area.height+","+width+","+height+","+x+","+y+","+drawWidth+","+drawHeight+","+String.format(Locale.ROOT,"%.9f,%.4f,%.4f",scale,pivotX,pivotY)+"\n");
    }
    static void scene(String key,boolean opaque)throws Exception {
        String filename=key+"-original.png";BufferedImage source=read(filename,opaque);
        int width=source.getWidth(),height=source.getHeight();
        if(width/(double)height>16d/9)width=(int)Math.round(height*16d/9);else height=(int)Math.round(width*9d/16);
        Rectangle area=new Rectangle((source.getWidth()-width)/2,(source.getHeight()-height)/2,width,height);
        REGIONS.put(key,transform(source,area,960,540,0,0,960,540));log(key,filename,area,960,540,0,0,960,540,960d/width,480,270);
    }
    static Rectangle alpha(BufferedImage image,Rectangle area) {
        int left=area.width,top=area.height,right=0,bottom=0;
        for(int y=0;y<area.height;y++)for(int x=0;x<area.width;x++)if((image.getRGB(area.x+x,area.y+y)>>>24)>=32) {
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);
        }
        if(right<=left || bottom<=top)throw new IllegalStateException("Empty painted cell");
        return new Rectangle(left,top,right-left,bottom-top);
    }
    static int actorSeam(BufferedImage image) {
        int from=image.getWidth()/4,to=image.getWidth()*3/4,best=-1,bestRun=0,start=-1;
        for(int x=from;x<=to;x++) {
            boolean empty=x<to;
            if(empty)for(int y=0;y<image.getHeight();y++)if((image.getRGB(x,y)>>>24)>=96){empty=false;break;}
            if(empty){if(start<0)start=x;}
            else if(start>=0) {
                int length=x-start,mid=start+length/2;
                if(length>bestRun){bestRun=length;best=mid;}
                start=-1;
            }
        }
        if(best<0 || bestRun<4)throw new IllegalStateException("No transparent gutter between actor poses");
        return best;
    }
    static Point feet(BufferedImage source,Rectangle area,Rectangle body) {
        int top=body.y+(int)Math.floor(body.height*.9),left=area.width,right=0,bottom=body.y;
        for(int y=top;y<body.y+body.height;y++)for(int x=body.x;x<body.x+body.width;x++)
            if((source.getRGB(area.x+x,area.y+y)>>>24)>=96) {
                left=Math.min(left,x);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);
            }
        if(right<=left)throw new IllegalStateException("Actor has no visible feet in its lower body band");
        return new Point((left+right)/2,bottom);
    }
    static void actor(String actor,String... poses)throws Exception {
        String filename=actor.equals("charon")?"charon-original.png":actor+"-poses-original.png";
        BufferedImage source=read(filename,false);int count=poses.length;
        Rectangle[] cells=new Rectangle[count],bodies=new Rectangle[count];Point[] pivots=new Point[count];
        int seam=count==2?actorSeam(source):source.getWidth();POSE_SEAMS.put(actor,seam);
        double maximumWidth=0,maximumHeight=0;
        for(int pose=0;pose<count;pose++) {
            cells[pose]=count==1?new Rectangle(0,0,source.getWidth(),source.getHeight())
                :pose==0?new Rectangle(0,0,seam,source.getHeight()):new Rectangle(seam,0,source.getWidth()-seam,source.getHeight());
            bodies[pose]=alpha(source,cells[pose]);pivots[pose]=feet(source,cells[pose],bodies[pose]);
            maximumWidth=Math.max(maximumWidth,bodies[pose].width);maximumHeight=Math.max(maximumHeight,bodies[pose].height);
        }
        // A generated hand can reach beyond the nominal half-sheet boundary. Crop the real gutter.
        // Feet, rather than a silhouette's centroid, keep an extended hand from recentering the torso.
        double scale=Math.min(400/maximumWidth,448/maximumHeight);
        for(int pose=0;pose<count;pose++) {
            Rectangle body=bodies[pose];double pivotX=pivots[pose].x,pivotY=pivots[pose].y;
            scale=Math.min(scale,200/Math.max(Math.abs(body.x-pivotX),Math.abs(body.x+body.width-pivotX)));
            if(body.y<pivotY)scale=Math.min(scale,448/(pivotY-body.y));
            if(body.y+body.height>pivotY)scale=Math.min(scale,14/(body.y+body.height-pivotY));
        }
        for(int pose=0;pose<count;pose++) {
            Rectangle area=cells[pose];int drawWidth=(int)Math.round(area.width*scale),drawHeight=(int)Math.round(area.height*scale);
            int x=(int)Math.round(ACTOR_PIVOT_X-pivots[pose].x*scale),y=(int)Math.round(ACTOR_PIVOT_Y-pivots[pose].y*scale);
            String key=poses[pose];REGIONS.put(key,transform(source,area,ACTOR_WIDTH,ACTOR_HEIGHT,x,y,drawWidth,drawHeight));
            log(key,filename,area,ACTOR_WIDTH,ACTOR_HEIGHT,x,y,drawWidth,drawHeight,scale,ACTOR_PIVOT_X,ACTOR_PIVOT_Y);
        }
    }
    static void portal()throws Exception {
        String filename="portal-original.png";BufferedImage source=read(filename,false);
        Rectangle[] cells=new Rectangle[4],bodies=new Rectangle[4];double maximumWidth=0,maximumHeight=0;
        for(int frame=0;frame<4;frame++) {
            cells[frame]=cell(source,2,2,frame%2,frame/2);bodies[frame]=alpha(source,cells[frame]);
            Rectangle body=bodies[frame];maximumWidth=Math.max(maximumWidth,body.width);maximumHeight=Math.max(maximumHeight,body.height);
        }
        double scale=240/Math.max(maximumWidth,maximumHeight);
        for(int frame=0;frame<4;frame++) {
            Rectangle area=cells[frame];int width=(int)Math.round(area.width*scale),height=(int)Math.round(area.height*scale);
            Rectangle body=bodies[frame];int x=(int)Math.round(128-body.getCenterX()*scale),y=(int)Math.round(128-body.getCenterY()*scale);
            String key="portal-"+frame;REGIONS.put(key,transform(source,area,256,256,x,y,width,height));
            log(key,filename,area,256,256,x,y,width,height,scale,128,128);
        }
    }
    static void pack()throws Exception {
        if(REGIONS.size()!=15)throw new IllegalStateException("Expected 15 opening regions, got "+REGIONS.size());
        BufferedImage page=new BufferedImage(2048,2048,BufferedImage.TYPE_INT_ARGB);Graphics2D g=page.createGraphics();g.setComposite(AlphaComposite.Src);
        StringBuilder atlas=new StringBuilder("opening.png\nsize: 2048, 2048\nformat: RGBA8888\nfilter: Linear, Linear\nrepeat: none\n");
        var positions=new LinkedHashMap<String,Point>();
        positions.put("background-home",new Point(1,1));positions.put("background-styx",new Point(963,1));
        positions.put("foreground-home",new Point(1,543));positions.put("foreground-styx",new Point(963,543));
        String[] actors={"hero-idle","hero-reach","wife-idle","wife-resist","zeus-idle","zeus-cast","charon"};
        for(int i=0;i<actors.length;i++)positions.put(actors[i],new Point(1+(i%4)*434,i<4?1085:1567));
        positions.put("portal-0",new Point(1737,1085));positions.put("portal-1",new Point(1737,1343));
        positions.put("portal-2",new Point(1737,1601));positions.put("portal-3",new Point(1303,1567));
        for(var entry:REGIONS.entrySet()) {
            String key=entry.getKey();BufferedImage image=entry.getValue();Point at=positions.get(key);
            if(at==null || at.x+image.getWidth()+1>2048 || at.y+image.getHeight()+1>2048)throw new IllegalStateException("Atlas overflow: "+key);
            g.drawImage(image,at.x,at.y,null);atlas.append(key+"\n  rotate: false\n  xy: "+at.x+", "+at.y+"\n  size: "+image.getWidth()+", "+image.getHeight()+"\n  orig: "+image.getWidth()+", "+image.getHeight()+"\n  offset: 0, 0\n  index: -1\n");
        }
        g.dispose();Files.createDirectories(OUT);ImageIO.write(page,"png",OUT.resolve("opening.png").toFile());
        Files.writeString(OUT.resolve("opening.atlas"),atlas);Files.writeString(SOURCE.resolve("registration.csv"),REGISTRATION);
        StringBuilder manifest=new StringBuilder("{\n  \"date\": \"2026-10-27\",\n  \"tool\": \"built-in image_gen\",\n  \"prompts\": \"prompts.json\",\n  \"contract\": \"contract.json\",\n  \"selectedSources\": {\n");int i=0;
        for(var entry:SOURCE_META.entrySet())manifest.append("    \""+entry.getKey()+"\": "+entry.getValue()+(++i<SOURCE_META.size()?",":"")+"\n");
        manifest.append("  },\n  \"registration\": \"registration.csv\",\n  \"actorPoseCropSeams\": {");i=0;
        for(var seam:POSE_SEAMS.entrySet())manifest.append("\""+seam.getKey()+"\": "+seam.getValue()+(++i<POSE_SEAMS.size()?", ":""));
        manifest.append("},\n  \"actorCell\": [432,480],\n  \"actorFeetPivot\": [216,464],\n  \"filter\": \"Linear\",\n  \"operations\": [\"technical crop at actual pose gutter\", \"nearest-neighbor uniform scaling per actor group\", \"foot-pivot registration per pose\", \"atlas packing\"],\n  \"paintOrRecolor\": false,\n  \"preserveGeneratedAlphaAndColors\": true,\n  \"packed\": {\"atlas\": \"../../../core/assets/cutscenes/opening.atlas\", \"page\": \"../../../core/assets/cutscenes/opening.png\", \"regions\": 15, \"size\": [2048,2048], \"rgbaBytes\": 16777216, \"sha256\": \""+hash(OUT.resolve("opening.png"))+"\"}\n}\n");
        Files.writeString(SOURCE.resolve("manifest.json"),manifest);review();
        System.out.println("OPENING_ATLAS_OK: 15 regions, 2048x2048, 16777216 RGBA bytes");
    }
    static void review()throws Exception {
        Files.createDirectories(Path.of("build/smoke"));
        BufferedImage actors=new BufferedImage(7*434,520,BufferedImage.TYPE_INT_ARGB);Graphics2D g=actors.createGraphics();
        g.setColor(new Color(14,22,30));g.fillRect(0,0,actors.getWidth(),actors.getHeight());
        String[] names={"hero-idle","hero-reach","wife-idle","wife-resist","zeus-idle","zeus-cast","charon"};
        g.setColor(new Color(180,150,85));g.drawLine(0,464,actors.getWidth(),464);
        for(int i=0;i<names.length;i++) {g.drawImage(REGIONS.get(names[i]),i*434,0,null);g.setColor(Color.WHITE);g.drawString(names[i],i*434+12,505);}
        g.dispose();ImageIO.write(actors,"png",Path.of("build/smoke/opening-actors-catalog.png").toFile());
        BufferedImage layers=new BufferedImage(960,540*4,BufferedImage.TYPE_INT_ARGB);g=layers.createGraphics();
        g.setColor(new Color(14,22,30));g.fillRect(0,0,layers.getWidth(),layers.getHeight());int row=0;
        for(String key:new String[]{"background-home","background-styx","foreground-home","foreground-styx"})g.drawImage(REGIONS.get(key),0,row++*540,null);
        g.dispose();ImageIO.write(layers,"png",Path.of("build/smoke/opening-layers-catalog.png").toFile());
        BufferedImage catalog=new BufferedImage(4*434,2*520+270,BufferedImage.TYPE_INT_ARGB);g=catalog.createGraphics();
        g.setColor(new Color(14,22,30));g.fillRect(0,0,catalog.getWidth(),catalog.getHeight());
        for(int i=0;i<names.length;i++) {
            int x=(i%4)*434,y=(i/4)*520;BufferedImage background=REGIONS.get(i==6?"background-styx":"background-home");
            g.drawImage(background,x,y,x+432,y+480,0,0,background.getWidth(),background.getHeight(),null);
            g.drawImage(REGIONS.get(names[i]),x,y,null);g.setColor(Color.WHITE);g.drawString(names[i],x+12,y+505);
        }
        g.drawImage(REGIONS.get("background-home"),0,1040,480,270,null);
        g.drawImage(REGIONS.get("background-styx"),480,1040,480,270,null);
        for(int i=0;i<4;i++)g.drawImage(REGIONS.get("portal-"+i),960+i*190,1040,190,190,null);
        g.dispose();ImageIO.write(catalog,"png",Path.of("build/smoke/opening-art-catalog.png").toFile());
    }
    public static void main(String[] args)throws Exception {
        if(args.length==1&&args[0].equals("references")){references();return;}
        scene("background-home",true);scene("background-styx",true);
        scene("foreground-home",false);scene("foreground-styx",false);
        actor("hero","hero-idle","hero-reach");actor("wife","wife-idle","wife-resist");
        actor("zeus","zeus-idle","zeus-cast");actor("charon","charon");portal();pack();
    }
}
