package br.com.srsups.paradiseinhell;

/** Native window operations are isolated from preference files and preview timing. */
public interface VideoBackend {
    interface Snapshot { VideoSettings settings(); }
    record BasicSnapshot(VideoSettings settings) implements Snapshot {
        public BasicSnapshot { java.util.Objects.requireNonNull(settings); }
    }
    VideoSettings current();
    default boolean matches(VideoSettings expected){return current().equals(expected);}
    boolean apply(VideoSettings settings);
    default VideoSettings adapt(VideoSettings settings){return java.util.Objects.requireNonNull(settings);}
    default Snapshot snapshot(){return new BasicSnapshot(current());}
    default boolean restore(Snapshot state){return apply(java.util.Objects.requireNonNull(state).settings());}
    default String monitorDescription(){return "Monitor atual";}
    default boolean supportsFullscreen(){return true;}
    default VideoSettings safeDefaults(){return VideoSettings.DEFAULT;}
    static VideoBackend memory() {
        return new VideoBackend() {
            private VideoSettings value=VideoSettings.DEFAULT;
            public VideoSettings current(){return value;}
            public boolean apply(VideoSettings next){value=java.util.Objects.requireNonNull(next);return true;}
        };
    }
}
