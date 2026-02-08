package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.freeplane.plugin.ai.chat.history.MapRootShortTextCount;
import org.junit.Test;

public class ChatListDialogTest {

    @Test
    public void mergeLiveMapCounts_includesFreshMapsWhenCachedCountsExist() {
        List<MapRootShortTextCount> cached = Arrays.asList(
            new MapRootShortTextCount("Map A", 3));
        List<MapRootShortTextCount> fresh = Arrays.asList(
            new MapRootShortTextCount("Map B", 1));

        List<MapRootShortTextCount> merged = ChatListDialog.mergeLiveMapCounts(cached, fresh);

        assertThat(merged).anySatisfy(entry -> {
            assertThat(entry.getText()).isEqualTo("Map A");
            assertThat(entry.getCount()).isEqualTo(3);
        });
        assertThat(merged).anySatisfy(entry -> {
            assertThat(entry.getText()).isEqualTo("Map B");
            assertThat(entry.getCount()).isEqualTo(1);
        });
    }
}
