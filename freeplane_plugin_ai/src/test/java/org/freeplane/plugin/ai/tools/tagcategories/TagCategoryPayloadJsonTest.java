package org.freeplane.plugin.ai.tools.tagcategories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.freeplane.features.icon.TagCategoryEdit;
import org.freeplane.features.icon.TagCategoryEditBatch;
import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.icon.TagDescriptor;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TagCategoryPayloadJsonTest {
    @Test
    public void snapshotPayloadRoundtripIsLossless() throws Exception {
        TagCategorySnapshot snapshot = new TagCategorySnapshot(
            "sha256:abc",
            "::",
            Collections.singletonList(new TagCategoryNode(
                Collections.singletonList("Project"),
                "Project",
                "Project",
                "#01020304",
                Collections.singletonList(new TagCategoryNode(
                    Arrays.asList("Project", "Status"),
                    "Status",
                    "Project::Status",
                    "#05060708",
                    Collections.emptyList())))),
            Collections.singletonList(new TagDescriptor(
                Collections.singletonList("urgent"),
                "urgent",
                "urgent",
                "#ff0000ff")));

        TagCategorySnapshotPayload payload = TagCategorySnapshotPayload.fromSnapshot("map-1", snapshot);
        ObjectMapper uut = new ObjectMapper();

        String json = uut.writeValueAsString(payload);
        TagCategorySnapshotPayload restoredPayload = uut.readValue(json, TagCategorySnapshotPayload.class);

        assertThat(restoredPayload).isEqualTo(payload);
        assertThat(restoredPayload.toSnapshot()).isEqualTo(snapshot);
    }

    @Test
    public void editBatchPayloadRoundtripIsLossless() throws Exception {
        TagCategoryEditBatch editBatch = new TagCategoryEditBatch(
            "sha256:abc",
            Arrays.asList(
                TagCategoryEdit.rename(Arrays.asList("Project", "Status"), "State"),
                TagCategoryEdit.setSeparator("/")));
        TagCategoryEditBatchPayload payload = TagCategoryEditBatchPayload.fromEditBatch("map-1", editBatch);
        ObjectMapper uut = new ObjectMapper();

        String json = uut.writeValueAsString(payload);
        TagCategoryEditBatchPayload restoredPayload = uut.readValue(json, TagCategoryEditBatchPayload.class);

        assertThat(restoredPayload).isEqualTo(payload);
        assertThat(restoredPayload.toEditBatch()).isEqualTo(editBatch);
    }
}
