import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;

/** Technical crop/registration/packing of generated damage and death artwork; never draws replacement poses. */
class ReactionAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/m6-reactions"),OUT=Path.of("core/assets/visual");
    static final String[] ACTORS={"cacador","ferreiro","vigilante","apostador","feiticeira","sacerdote","necromante","guerreiro",
        "farmer-rake","farmer-scythe","common","fast","tank","archer","carrasco","guardian"};
    static final String[] DIRS={"down","up","left","up-left","down-left"};
    static final LinkedHashMap<String,BufferedImage> TILES=new LinkedHashMap<>();
    static final StringBuilder REGISTRATION=new StringBuilder("actor,direction,action,frame,source_x,source_y,source_width,source_height,scale,offset_x,offset_y\n");
    static final StringBuilder CALIBRATION=new StringBuilder("actor,reference_height,target_height,generated_hurt_height,scale,direction,reference_head_x,reference_foot_y,target_root_x,target_foot_y\n");
    static final Map<String,Frame> REFERENCES=new HashMap<>();
    record Frame(BufferedImage page,int x,int y,int width,int height) {}
    record Calibration(float reference,float target,double generated,double scale) {}
    record Anchor(double referenceHeadX,double referenceFootY,double targetX,double targetY) {}
    static final Map<String,Calibration> METRICS=new LinkedHashMap<>();
    static final Map<String,Anchor[]> ANCHORS=new LinkedHashMap<>();
    static String hash(Path file)throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
    static void readAtlas(String name)throws Exception {
        BufferedImage page=null;String region=null;int x=0,y=0,width=0,height=0;
        for(String line:Files.readAllLines(OUT.resolve(name+".atlas"))) {
            String text=line.trim();if(text.isEmpty())continue;
            if(!line.startsWith(" ")&&!line.startsWith("\t")) {
                if(text.endsWith(".png")){page=ImageIO.read(OUT.resolve(text).toFile());region=null;}
                else if(!text.contains(":"))region=text;
            } else if(region!=null) {
                if(text.startsWith("xy:")){String[] xy=text.substring(3).trim().split(",\\s*");x=Integer.parseInt(xy[0]);y=Integer.parseInt(xy[1]);}
                else if(text.startsWith("size:")){String[] wh=text.substring(5).trim().split(",\\s*");width=Integer.parseInt(wh[0]);height=Integer.parseInt(wh[1]);}
                else if(text.startsWith("index:"))REFERENCES.put(region,new Frame(page,x,y,width,height));
            }
        }
    }
    static Rectangle alpha(BufferedImage image,Rectangle cell,boolean torso) {
        int left=cell.width,top=cell.height,right=0,bottom=0;
        for(int y=0;y<cell.height;y++)for(int x=0;x<cell.width;x++){
            if(torso&&(x<cell.width*.36||x>=cell.width*.64))continue;
            if((image.getRGB(cell.x+x,cell.y+y)>>>24)>=96){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x+1);bottom=Math.max(bottom,y+1);}
        }
        if(right<=left||bottom<=top)throw new IllegalStateException("Empty source cell "+cell);
        return new Rectangle(left,top,right-left,bottom-top);
    }
    static int[] cuts(BufferedImage image,int count,boolean vertical) {
        int length=vertical?image.getHeight():image.getWidth(),cross=vertical?image.getWidth():image.getHeight();
        int[] density=new int[length],result=new int[count+1];result[count]=length;
        for(int p=0;p<length;p++)for(int q=0;q<cross;q++)if((image.getRGB(vertical?q:p,vertical?p:q)>>>24)>=96)density[p]++;
        double cell=length/(double)count;
        for(int index=1;index<count;index++){
            double expected=index*cell;int best=(int)Math.round(expected),bestScore=Integer.MAX_VALUE;
            for(int p=Math.max(result[index-1]+1,(int)(expected-cell*.20));p<Math.min(length,(int)(expected+cell*.20));p++){
                int score=density[p]*100+(int)Math.round(Math.abs(p-expected));
                if(score<bestScore){bestScore=score;best=p;}
            }
            result[index]=best;
        }
        return result;
    }
    static Rectangle cell(int[] xs,int[] ys,int column,int row){return new Rectangle(xs[column],ys[row],xs[column+1]-xs[column],ys[row+1]-ys[row]);}
    static Frame referenceFrame(String actor,String direction){
        boolean basic=Set.of("common","fast","tank").contains(actor);
        String key=actor+(basic?"-":"-walk-")+direction+"-0";Frame frame=REFERENCES.get(key);
        if(frame==null&&basic){
            String cardinal=direction.equals("up-left")?"up":direction.equals("down-left")?"down":direction;
            frame=REFERENCES.get(actor+"-"+cardinal+"-0");
        }
        if(frame==null)throw new IllegalStateException("Missing reference "+key);return frame;
    }
    static Anchor anchor(String actor,String direction){
        boolean basic=Set.of("common","fast","tank").contains(actor);Frame ref=referenceFrame(actor,direction);
        Rectangle bounds=new Rectangle(ref.x,ref.y,ref.width,ref.height),body=alpha(ref.page,bounds,!basic);
        double center=0,pixels=0;
        for(int y=body.y;y<body.y+Math.max(2,body.height*.25);y++)for(int x=(int)(ref.width*.36);x<ref.width*.64;x++)
            if((ref.page.getRGB(ref.x+x,ref.y+y)>>>24)>=96){center+=x;pixels++;}
        double head=pixels>0?center/pixels:ref.width*.5,foot=body.y+body.height;
        return new Anchor(head,foot,basic?32+(head/ref.width-.5)*64/1.6:head,
            basic?32+(foot/ref.height-.5)*64/1.6:foot);
    }
    static float referenceHeight(String actor) {
        boolean basic=Set.of("common","fast","tank").contains(actor);
        double[] references=new double[5];int count=0;
        for(String direction:DIRS){
            Frame frame=referenceFrame(actor,direction);
            references[count++]=alpha(frame.page,new Rectangle(frame.x,frame.y,frame.width,frame.height),!basic).height;
        }
        if(count==0)throw new IllegalStateException("Missing calibration reference for "+actor);
        Arrays.sort(references,0,count);return (float)references[count/2];
    }
    static Calibration calibrate(String actor,BufferedImage image,int[] xs,int[] ys) {
        boolean basic=Set.of("common","fast","tank").contains(actor);float reference=referenceHeight(actor);
        float target=basic?reference*1.25f:reference;
        double[] heights=new double[15];int index=0;
        for(int row=0;row<5;row++)for(int column=0;column<3;column++)heights[index++]=alpha(image,cell(xs,ys,column,row),true).height;
        Arrays.sort(heights);double generated=heights[7],scale=target/generated;
        Calibration result=new Calibration(reference,target,generated,scale);METRICS.put(actor,result);
        return result;
    }
    static void actor(String actor)throws Exception {
        Path file=SOURCE.resolve(actor+"-reactions-original.png");BufferedImage image=ImageIO.read(file.toFile());
        if(image==null||!image.getColorModel().hasAlpha()||(image.getRGB(0,0)>>>24)>16)throw new IllegalStateException("Missing transparent source "+actor);
        int[] xs=cuts(image,7,false),ys=cuts(image,5,true);Calibration metric=calibrate(actor,image,xs,ys);
        double scale=metric.scale;Anchor[] anchors=new Anchor[5];ANCHORS.put(actor,anchors);
        for(int row=0;row<5;row++) {
            Anchor target=anchors[row]=anchor(actor,DIRS[row]);
            CALIBRATION.append(actor+","+metric.reference+","+metric.target+","+metric.generated+","+
                String.format(Locale.ROOT,"%.8f",scale)+","+DIRS[row]+","+target.referenceHeadX+","+target.referenceFootY+","+
                String.format(Locale.ROOT,"%.5f,%.5f",target.targetX,target.targetY)+"\n");
            Rectangle root=cell(xs,ys,2,row),body=alpha(image,root,true);
            double center=0,pixels=0;
            for(int py=body.y;py<body.y+Math.max(2,body.height*.25);py++)for(int px=(int)(root.width*.36);px<root.width*.64;px++)
                if((image.getRGB(root.x+px,root.y+py)>>>24)>=96){center+=px;pixels++;}
            double rootX=(pixels>0?center/pixels:root.width*.5)/root.width,rootY=(body.y+body.height)/(double)root.height;
            for(int column=0;column<7;column++) {
                Rectangle source=cell(xs,ys,column,row);alpha(image,source,false);
                int dx=(int)Math.round(target.targetX-rootX*source.width*scale),dy=(int)Math.round(target.targetY-rootY*source.height*scale);
                BufferedImage tile=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();
                g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(image,dx,dy,dx+(int)Math.round(source.width*scale),dy+(int)Math.round(source.height*scale),
                    source.x,source.y,source.x+source.width,source.y+source.height,null);g.dispose();
                String action=column<3?"hurt":"death";int frame=column<3?column:column-3;
                TILES.put(actor+"-"+action+"-"+DIRS[row]+"-"+frame,tile);
                REGISTRATION.append(actor+","+DIRS[row]+","+action+","+frame+","+source.x+","+source.y+","+source.width+","+source.height+","+
                    String.format(Locale.ROOT,"%.8f",scale)+","+dx+","+dy+"\n");
            }
        }
    }
    static void pack()throws Exception {
        BufferedImage image=new BufferedImage(2048,2048,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();g.setComposite(AlphaComposite.Src);
        StringBuilder atlas=new StringBuilder("m6-reactions.png\nsize: 2048, 2048\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n");
        int index=0;
        for(var entry:TILES.entrySet()){
            int x=1+index%31*66,y=1+index/31*66;g.drawImage(entry.getValue(),x,y,null);
            atlas.append(entry.getKey()+"\n  rotate: false\n  xy: "+x+", "+y+"\n  size: 64, 64\n  orig: 64, 64\n  offset: 0, 0\n  index: -1\n");index++;
        }
        if(index!=560)throw new IllegalStateException("Expected560reactions, found "+index);
        g.dispose();ImageIO.write(image,"png",OUT.resolve("m6-reactions.png").toFile());Files.writeString(OUT.resolve("m6-reactions.atlas"),atlas);
        Files.writeString(SOURCE.resolve("registration.csv"),REGISTRATION);Files.writeString(SOURCE.resolve("calibration.csv"),CALIBRATION);
        StringBuilder manifest=new StringBuilder("{\n  \"date\": \"2026-10-23\",\n  \"tool\": \"built-in image_gen\",\n  \"prompts\": [\"classes-prompts.json\", \"enemies-prompts.json\", \"enemies-prompts-revisions.json\"],\n  \"enemyGenerationMetadata\": \"manifest-enemies.json\",\n  \"contract\": \"contract.json\",\n  \"actors\": {\n");
        for(int i=0;i<ACTORS.length;i++){
            String name=ACTORS[i];Calibration c=METRICS.get(name);
            manifest.append("    \""+name+"\": {\"source\": \""+name+"-reactions-original.png\", \"sha256\": \""+hash(SOURCE.resolve(name+"-reactions-original.png"))+"\", "+
                "\"referenceAlphaHeight\": "+c.reference+", \"targetAlphaHeight\": "+c.target+", \"generatedHurtHeight\": "+c.generated+", \"fixedScale\": "+String.format(Locale.ROOT,"%.8f",c.scale)+", \"anchors\": {");
            Anchor[] roots=ANCHORS.get(name);
            for(int d=0;d<5;d++)manifest.append("\""+DIRS[d]+"\": {\"rootX\": "+String.format(Locale.ROOT,"%.5f",roots[d].targetX)+", \"footY\": "+String.format(Locale.ROOT,"%.5f",roots[d].targetY)+"}"+(d<4?", ":""));
            manifest.append("}}"+(i<ACTORS.length-1?",":"")+"\n");
        }
        manifest.append("  },\n  \"canonicalDirections\": [\"down\", \"up\", \"left\", \"up-left\", \"down-left\"],\n  \"sourceGrid\": [7, 5],\n  \"frames\": {\"hurt\": 3, \"death\": 4},\n"+
            "  \"registration\": \"registration.csv; fixed row root from recovery hurt pose, one measured scale per actor shared with all death frames\",\n  \"calibration\": \"calibration.csv; common/fast/tank source32 alpha height x1.25, other actors calibrated to equipped/m5 body alpha height\",\n"+
            "  \"operations\": [\"crop at gutters\", \"root registration\", \"nearest-neighbor reduction\", \"packing\"],\n  \"preserveGeneratedAlpha\": true,\n  \"paintOrRecolor\": false,\n"+
            "  \"packed\": {\"atlas\": \"../../../core/assets/visual/m6-reactions.atlas\", \"page\": \"../../../core/assets/visual/m6-reactions.png\", \"regions\": 560, \"logicalPosesWithMirrors\": 896, \"size\": [64, 64], \"dimensions\": [2048, 2048], \"rgbaBytes\": 16777216, \"sha256\": \""+hash(OUT.resolve("m6-reactions.png"))+"\"},\n"+
            "  \"tooling\": \"../../ReactionAtlasBaker.java\"\n}\n");
        Files.writeString(SOURCE.resolve("manifest.json"),manifest);
        Files.createDirectories(Path.of("build/smoke"));BufferedImage preview=new BufferedImage(7*96,16*96,BufferedImage.TYPE_INT_ARGB);Graphics2D p=preview.createGraphics();
        p.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(int a=0;a<16;a++)for(int f=0;f<7;f++)p.drawImage(TILES.get(ACTORS[a]+"-"+(f<3?"hurt":"death")+"-down-"+(f<3?f:f-3)),f*96,a*96,96,96,null);
        p.dispose();ImageIO.write(preview,"png",Path.of("build/smoke/m6-reactions-contact-sheet.png").toFile());
        System.out.println("REACTION_ATLAS_OK: "+index+" regions, 2048x2048, 16777216 RGBA bytes");
    }
    public static void main(String[] args)throws Exception {
        readAtlas("expanded");readAtlas("equipped");readAtlas("m5");
        if(args.length>0&&args[0].equals("references")){
            for(String actor:ACTORS){float height=referenceHeight(actor);System.out.println(actor+": reference="+height+", target="+
                (Set.of("common","fast","tank").contains(actor)?height*1.25f:height));}return;
        }
        for(String actor:ACTORS)actor(actor);pack();
    }
}
