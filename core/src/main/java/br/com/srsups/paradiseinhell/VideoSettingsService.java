package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Preview first, persist only explicit confirmation. Native rollback retains window placement too. */
public final class VideoSettingsService {
    public static final float PREVIEW_SECONDS=15;
    private final VideoSettingsStore store;
    private final VideoBackend backend;
    private final LongSupplier nanoClock;
    private VideoSettings value,before,target;
    private VideoBackend.Snapshot beforeState;
    private long started;
    private boolean writable=true,pendingSave,busy,cancelRequested,initialized;
    private String error="",notice="";
    public VideoSettingsService(VideoSettingsStore store,VideoBackend backend){this(store,backend,System::nanoTime);}
    public VideoSettingsService(VideoSettingsStore store,VideoBackend backend,LongSupplier nanoClock) {
        this.store=Objects.requireNonNull(store);this.backend=Objects.requireNonNull(backend);this.nanoClock=Objects.requireNonNull(nanoClock);
        try{value=Objects.requireNonNull(store.load());}
        catch(IOException | RuntimeException failure) {
            value=VideoSettings.DEFAULT;writable=false;
            error="Não foi possível ler o vídeo salvo. Preferências apenas nesta sessão; arquivo preservado.";
        }
    }
    public VideoSettings value(){return value;}
    public VideoSettings current(){return backend.current();}
    public String monitorDescription(){
        try{return backend.monitorDescription();}catch(RuntimeException unavailable){return "Monitor atual indisponível.";}
    }
    public boolean previewing(){return target!=null;}
    public boolean busy(){return busy;}
    public boolean pendingSave(){return pendingSave;}
    public String error(){return error;}
    public String notice(){return notice;}
    public float remainingSeconds(){return previewing()?Math.max(0,PREVIEW_SECONDS-(nanoClock.getAsLong()-started)/1_000_000_000f):0;}
    public boolean initialize() {
        if(initialized)return true;initialized=true;
        VideoSettings requested=value,effective=null;
        boolean applied=false;busy=true;
        try {
            effective=adapt(requested);
            applied=matches(effective)||(apply(effective)&&matches(effective));
        } catch(RuntimeException unavailable){applied=false;}
        finally{busy=false;cancelRequested=false;}
        if(applied) {
            value=effective;
            if(!effective.equals(requested))notice="O tamanho da janela foi ajustado à área disponível do monitor.";
            return true;
        }
        restoreSafe();value=current();
        notice="O modo salvo não está disponível. Uma janela segura foi aplicada.";return false;
    }
    public boolean preview(VideoSettings requested) {
        Objects.requireNonNull(requested);
        if(busy || previewing())return false;
        VideoSettings effective;
        try {
            effective=adapt(requested);
            beforeState=Objects.requireNonNull(backend.snapshot());before=Objects.requireNonNull(beforeState.settings());
        } catch(RuntimeException unavailable) {
            beforeState=null;before=null;
            error=requested.fullscreen()?"Tela cheia não está disponível neste monitor."
                :"Não foi possível preparar o modo para o monitor atual.";
            return false;
        }
        if(effective.equals(before)&&matches(effective)){beforeState=null;before=null;notice="A imagem já usa essas opções.";return false;}
        target=effective;started=nanoClock.getAsLong();cancelRequested=false;busy=true;
        boolean applied;
        try{applied=apply(effective);}finally{busy=false;}
        if(cancelRequested || !applied) {
            VideoSettings snapshot=before;cancelPreview();
            if(!applied && matches(snapshot))error="Não foi possível aplicar o modo escolhido. A imagem anterior foi restaurada.";
            return false;
        }
        notice=effective.equals(requested)?"Confira a imagem e confirme para manter."
            :"O tamanho foi ajustado ao monitor. Confira a imagem e confirme para manter.";
        return true;
    }
    private VideoSettings adapt(VideoSettings requested) {
        if(requested.fullscreen()&&!backend.supportsFullscreen())throw new IllegalArgumentException("Fullscreen unavailable");
        VideoSettings effective=Objects.requireNonNull(backend.adapt(requested));
        if(effective.mode()!=requested.mode())throw new IllegalArgumentException("Native adaptation changed the requested display mode");
        return effective;
    }
    private boolean apply(VideoSettings next) {
        try{return backend.apply(next);}catch(RuntimeException failed){return false;}
    }
    private boolean matches(VideoSettings expected) {
        try{return backend.matches(expected);}catch(RuntimeException unavailable){return false;}
    }
    public boolean confirm() {
        if(busy || !previewing())return false;
        if(remainingSeconds()==0){cancelPreview();return false;}
        if(!matches(target)) {
            VideoSettings snapshot=before;cancelPreview();
            if(matches(snapshot))error="O sistema não confirmou o modo escolhido. A imagem anterior foi restaurada.";
            return false;
        }
        value=target;target=null;before=null;beforeState=null;pendingSave=true;
        notice="Imagem mantida.";return retrySave();
    }
    public boolean cancelPreview() {
        if(busy){cancelRequested=true;return false;}
        if(!previewing())return true;
        VideoBackend.Snapshot snapshot=beforeState;target=null;before=null;beforeState=null;cancelRequested=false;
        boolean restored=restore(snapshot);
        notice=restored?"Alteração desfeita. A imagem anterior foi restaurada.":"Alteração encerrada. Confira o modo de exibição.";
        return restored;
    }
    private boolean restore(VideoBackend.Snapshot snapshot) {
        busy=true;
        try {
            try{if(backend.restore(snapshot)&&matches(snapshot.settings()))return true;}
            catch(RuntimeException unavailable){/* Try a fitted safe window below. */}
            return restoreSafeWhileBusy();
        } finally {busy=false;cancelRequested=false;}
    }
    private boolean restoreSafe() {
        busy=true;
        try{return restoreSafeWhileBusy();}
        finally{busy=false;cancelRequested=false;}
    }
    private boolean restoreSafeWhileBusy() {
        boolean recovered=false;
        try {
            VideoSettings safe=adapt(backend.safeDefaults());
            recovered=apply(safe)&&matches(safe);
        } catch(RuntimeException unavailable){/* Leave the diagnostic message visible. */}
        error=recovered?"O modo anterior não está disponível. Uma janela segura foi aplicada."
            :"Não foi possível restaurar o modo anterior. Tente o modo janela novamente.";
        return recovered;
    }
    public void tick(){if(!busy && previewing() && remainingSeconds()==0)cancelPreview();}
    public boolean retrySave() {
        if(busy || previewing() || !writable)return false;
        if(!pendingSave)return true;
        try{store.save(value);pendingSave=false;error="";return true;}
        catch(IOException|RuntimeException failure){error="Imagem mantida; não foi salva. Pressione S para tentar novamente.";return false;}
    }
}
