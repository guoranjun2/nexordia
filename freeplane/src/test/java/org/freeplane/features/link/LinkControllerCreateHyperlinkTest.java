package org.freeplane.features.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;

import org.freeplane.core.util.Hyperlink;
import org.junit.Test;

public class LinkControllerCreateHyperlinkTest {

    @Test
    public void testCreateHyperlink_PreservesOriginalTextWhileNormalizingURI() throws URISyntaxException {
        String inputValue =
            "https://flightcontrol-master.github.io/MOOSE_DOCS_DEVELOP/Documentation/Functional.Detection.html##(DETECTION_AREAS).New";

        Hyperlink hyperlink = LinkController.createHyperlink(inputValue);

        assertThat(hyperlink.toString()).isEqualTo(inputValue);
        assertThat(hyperlink.getUri().getFragment()).isEqualTo("#(DETECTION_AREAS).New");
        assertThat(hyperlink.getUri().toString()).isEqualTo(
            "https://flightcontrol-master.github.io/MOOSE_DOCS_DEVELOP/Documentation/Functional.Detection.html#%23(DETECTION_AREAS).New");
    }
}
