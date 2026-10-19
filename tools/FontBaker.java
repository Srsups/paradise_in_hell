import java.awt.*;
import java.awt.font.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Offline bitmap-font bake; the game ships PNG/FNT files and needs no AWT or native font extension. */
class FontBaker {
    public static void main(String[] args) throws Exception {
        Path sources=Path.of("tools/font-sources"), output=Path.of("core/assets/ui/fonts");
        Files.createDirectories(output);
        bake(sources.resolve("NotoSans.ttf"),output,"body",14);
        bake(sources.resolve("Cinzel.ttf"),output,"title",32);
        for(String name:java.util.List.of("NotoSans","Cinzel"))
            Files.copy(sources.resolve(name+"-OFL.txt"),output.resolve(name+"-OFL.txt"),StandardCopyOption.REPLACE_EXISTING);
    }
    static void bake(Path source,Path output,String name,int size) throws Exception {
        Font font=Font.createFont(Font.TRUETYPE_FONT,source.toFile()).deriveFont((float)size);
        BufferedImage page=new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=page.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(font);g.setColor(Color.WHITE);
        FontRenderContext frc=g.getFontRenderContext();
        FontMetrics metrics=g.getFontMetrics();
        String chars="";
        for(char c=32;c<256;c++)if(c<127||c>=160)chars+=c;
        chars+="\u2013\u2014\u2018\u2019\u201c\u201d\u2026\u2190\u2192\u2191\u2193\u2264\u2265\u25b6";
        StringBuilder data=new StringBuilder("info face=\""+font.getFamily()+"\" size="+size+" bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1\n"
            +"common lineHeight="+metrics.getHeight()+" base="+metrics.getAscent()+" scaleW=1024 scaleH=512 pages=1 packed=0\n"
            +"page id=0 file=\""+name+".png\"\nchars count="+chars.length()+"\n");
        int x=2,y=2,row=0,total=0;
        for(char c:chars.toCharArray()) {
            if(!font.canDisplay(c)) { if(c<256)throw new IllegalStateException("Missing glyph "+c); continue; }
            GlyphVector glyph=font.createGlyphVector(frc,new char[]{c});
            Rectangle b=glyph.getPixelBounds(frc,0,0);
            int w=Math.max(1,b.width),h=Math.max(1,b.height);
            if(x+w+2>1024){x=2;y+=row+3;row=0;}
            if(y+h+2>512)throw new IllegalStateException("Font overflow");
            g.drawGlyphVector(glyph,x-b.x,y-b.y);
            int advance=Math.round(glyph.getGlyphMetrics(0).getAdvanceX());
            data.append("char id="+(int)c+" x="+x+" y="+y+" width="+w+" height="+h
                +" xoffset="+b.x+" yoffset="+(metrics.getAscent()+b.y)+" xadvance="+advance+" page=0 chnl=15\n");
            x+=w+3;row=Math.max(row,h);
            total++;
        }
        int height=1;while(height<y+row+2)height*=2;
        g.dispose();ImageIO.write(page.getSubimage(0,0,1024,height),"png",output.resolve(name+".png").toFile());
        Files.writeString(output.resolve(name+".fnt"),data.toString().replace("scaleH=512","scaleH="+height)
            .replace("chars count="+chars.length(),"chars count="+total));
        System.out.println(name+": "+total+" glyphs, 1024x"+height);
    }
}

