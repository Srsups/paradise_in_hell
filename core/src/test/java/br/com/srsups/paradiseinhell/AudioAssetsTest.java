package br.com.srsups.paradiseinhell;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioAssetsTest {
    private byte[] bytes(String path) throws Exception {
        try(var input=getClass().getClassLoader().getResourceAsStream(path)) {assertNotNull(input,path);return input.readAllBytes();}
    }
    private float validate(String path) throws Exception {
        byte[] data=bytes(path);ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals("RIFF",new String(data,0,4,java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals("WAVE",new String(data,8,4,java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals(1,b.getShort(20));assertEquals(1,b.getShort(22));
        assertEquals(22050,b.getInt(24));assertEquals(16,b.getShort(34));assertEquals(data.length-44,b.getInt(40));
        int peak=0;for(int i=44;i<data.length;i+=2)peak=Math.max(peak,Math.abs((int)b.getShort(i)));
        assertTrue(peak>1000 && peak<22000,path+" must contain real unclipped audio");
        return (data.length-44)/44100f;
    }
    @Test void everyCueAndBothStreamingLoopsArePackagedPcmWithExpectedDurations() throws Exception {
        for(SoundCue cue:SoundCue.values())assertEquals(cue.duration,validate(cue.asset()),.001f);
        assertEquals(24,validate(AudioBackend.Track.REFUGE.asset),.001f);
        assertEquals(16,validate(AudioBackend.Track.COMBAT.asset),.001f);
    }
}

