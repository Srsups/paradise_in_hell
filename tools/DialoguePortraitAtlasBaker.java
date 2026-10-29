import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;

/** Crop/register/pack generated portrait sheets. Never paints artwork or changes its alpha/colors. */
class DialoguePortraitAtlasBaker {
    static final Path SOURCE=Path.of("tools/art-sources/m6-dialogue"),OUT=Path.of("core/assets/visual");
    static final String[] ACTORS={"charon","blacksmith","camponesa"};
    static final String[] EXPRESSIONS={"neutral","amused","concerned","pleased"};
    static final StringBuilder LOG=new StringBuilder("actor,expression,source_x,source_y,crop_width,crop_height,dest_x,dest_y,draw_width,draw_height\n");
    static final LinkedHashMap<String,BufferedImage> TILES=new LinkedHashMap<>();
    static String sha256(Path file)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
    static void portraits(String actor)throws Exception {
        BufferedImage source=ImageIO.read(SOURCE.resolve(actor+"-expressions-original.png").toFile());
        if(source==null||!source.getColorModel().hasAlpha()||(source.getRGB(0,0)>>>24)>16)
            throw new IllegalStateException("Portrait source must have real transparent alpha: "+actor);
        int halfWidth=source.getWidth()/2,halfHeight=source.getHeight()/2;
        // Uniform bust crop for all expressions: the last few pixels are outside the dialogue's chest framing.
        // This avoids neighboring rows/matte tails while preserving every face and the generated edge alpha.
        int cropHeight=(int)Math.floor(halfHeight*.965),cropWidth=halfWidth;
        double scale=248d/Math.max(cropWidth,cropHeight);
        int width=(int)Math.round(cropWidth*scale),height=(int)Math.round(cropHeight*scale);
        int dx=(256-width)/2,dy=252-height;
        for(int index=0;index<4;index++) {
            int sx=index%2*halfWidth,sy=index/2*halfHeight,opaque=0,transparent=0;
            for(int y=0;y<cropHeight;y++)for(int x=0;x<cropWidth;x++){
                int alpha=source.getRGB(sx+x,sy+y)>>>24;
                if(alpha>=96)opaque++;if(alpha==0)transparent++;
            }
            if(opaque<10000||transparent<1000)throw new IllegalStateException("Missing portrait/transparency: "+actor+" "+index);
            BufferedImage tile=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(source,dx,dy,dx+width,dy+height,sx,sy,sx+cropWidth,sy+cropHeight,null);g.dispose();
            TILES.put("portrait-"+actor+"-"+EXPRESSIONS[index],tile);
            LOG.append(actor+","+EXPRESSIONS[index]+","+sx+","+sy+","+cropWidth+","+cropHeight+","+
                dx+","+dy+","+width+","+height+"\n");
        }
    }
    static void pack()throws Exception {
        BufferedImage page=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);Graphics2D g=page.createGraphics();
        g.setComposite(AlphaComposite.Src);
        StringBuilder atlas=new StringBuilder("m6-dialogue.png\nsize: 1024, 1024\nformat: RGBA8888\nfilter: Nearest, Nearest\nrepeat: none\n");
        int index=0;
        for(var entry:TILES.entrySet()) {
            int x=index%4*256,y=index/4*256;g.drawImage(entry.getValue(),x,y,null);
            atlas.append(entry.getKey()+"\n  rotate: false\n  xy: "+x+", "+y+
                "\n  size: 256, 256\n  orig: 256, 256\n  offset: 0, 0\n  index: -1\n");index++;
        }
        g.dispose();Files.createDirectories(OUT);
        ImageIO.write(page,"png",OUT.resolve("m6-dialogue.png").toFile());Files.writeString(OUT.resolve("m6-dialogue.atlas"),atlas);
        Files.writeString(SOURCE.resolve("registration.csv"),LOG);
        StringBuilder manifest=new StringBuilder("{\n  \"date\": \"2026-10-23\",\n  \"tool\": \"built-in image_gen\",\n  \"prompts\": \"prompts.json\",\n  \"selection\": {\n");
        for(int i=0;i<ACTORS.length;i++){
            String actor=ACTORS[i],filename=actor+"-expressions-original.png";
            manifest.append("    \""+actor+"\": {\"source\": \""+filename+"\", \"sha256\": \""+sha256(SOURCE.resolve(filename))+"\"}"+(i<ACTORS.length-1?",":"")+"\n");
        }
        manifest.append("  },\n  \"expressions\": [\"neutral\", \"amused\", \"concerned\", \"pleased\"],\n"+
            "  \"sourceGrid\": [2, 2],\n  \"registration\": \"registration.csv; fixed uniform bust crop and root/scale for each actor across all expressions\",\n"+
            "  \"operations\": [\"crop\", \"registration\", \"nearest-neighbor reduction\", \"atlas packing\"],\n"+
            "  \"preserveGeneratedAlpha\": true,\n  \"paintOrRecolor\": false,\n"+
            "  \"packed\": {\"atlas\": \"../../../core/assets/visual/m6-dialogue.atlas\", \"page\": \"../../../core/assets/visual/m6-dialogue.png\", \"regions\": 12, \"portraitSize\": [256, 256], \"dimensions\": [1024, 1024], \"rgbaBytes\": 4194304, \"sha256\": \""+sha256(OUT.resolve("m6-dialogue.png"))+"\"},\n"+
            "  \"runtime\": \"ExpandedArt shares the atlas across dialogue screens and disposes it once. Existing world sprites and protagonist portrait remain unchanged.\",\n"+
            "  \"tooling\": \"../../DialoguePortraitAtlasBaker.java\"\n}\n");
        Files.writeString(SOURCE.resolve("manifest.json"),manifest);
        System.out.println("DIALOGUE_PORTRAITS_OK: "+index+" regions, 1024x1024, 4194304 RGBA bytes");
    }
    public static void main(String[] args)throws Exception {
        for(String actor:ACTORS)portraits(actor);pack();
    }
}
