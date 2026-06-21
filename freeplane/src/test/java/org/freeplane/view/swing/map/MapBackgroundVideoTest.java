package org.freeplane.view.swing.map;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.Test;

public class MapBackgroundVideoTest {
    @Test
    public void acceptsSupportedVideoExtensions() throws Exception {
        assertThat(MapBackgroundVideo.isSupportedVideoUri(new URI("file:/tmp/background.mp4"))).isTrue();
        assertThat(MapBackgroundVideo.isSupportedVideoUri(new URI("file:/tmp/background.M4V"))).isTrue();
        assertThat(MapBackgroundVideo.isSupportedVideoUri(new URI("https://example.test/background.mov?token=1"))).isTrue();
    }

    @Test
    public void rejectsStaticImagesAndUnsupportedSchemes() throws Exception {
        assertThat(MapBackgroundVideo.isSupportedVideoUri(null)).isFalse();
        assertThat(MapBackgroundVideo.isSupportedVideoUri(new URI("file:/tmp/background.png"))).isFalse();
        assertThat(MapBackgroundVideo.isSupportedVideoUri(new URI("ftp://example.test/background.mp4"))).isFalse();
    }

    @Test
    public void parsesForegroundOpacityAsPercent() {
        assertThat(MapBackgroundVideo.foregroundOpacity(null)).isEqualTo(0.35d);
        assertThat(MapBackgroundVideo.foregroundOpacity("35")).isEqualTo(0.35d);
        assertThat(MapBackgroundVideo.foregroundOpacity("35%")).isEqualTo(0.35d);
        assertThat(MapBackgroundVideo.foregroundOpacity("0.8")).isEqualTo(0.8d);
        assertThat(MapBackgroundVideo.foregroundOpacity("1")).isEqualTo(0.01d);
        assertThat(MapBackgroundVideo.foregroundOpacity("1.0")).isEqualTo(1d);
        assertThat(MapBackgroundVideo.foregroundOpacity("-10")).isEqualTo(0d);
        assertThat(MapBackgroundVideo.foregroundOpacity("120")).isEqualTo(1d);
        assertThat(MapBackgroundVideo.foregroundOpacity("bad")).isEqualTo(0.35d);
    }
}
