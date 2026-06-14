package org.freeplane.view.swing.map.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CloudImagePaintingTest {
	@Test
	public void paintsSimplifiedVisibleCloudImagesWhenEnabled() {
		assertThat(CloudImagePainting.shouldPaint(true, true, true, false)).isTrue();
	}

	@Test
	public void paintsVisibleFixedCloudImagesOutsideSimplifiedMode() {
		assertThat(CloudImagePainting.shouldPaint(true, true, false, true)).isTrue();
	}

	@Test
	public void paintsFixedCloudImagesOutsideVisibleArea() {
		assertThat(CloudImagePainting.shouldPaint(true, false, false, true)).isTrue();
	}

	@Test
	public void skipsCloudImagesWhenDisabled() {
		assertThat(CloudImagePainting.shouldPaint(false, true, true, true)).isFalse();
	}

	@Test
	public void skipsInvisibleNonFixedCloudImages() {
		assertThat(CloudImagePainting.shouldPaint(true, false, true, false)).isFalse();
	}

	@Test
	public void skipsNonFixedCloudImagesOutsideSimplifiedMode() {
		assertThat(CloudImagePainting.shouldPaint(true, true, false, false)).isFalse();
	}

	@Test
	public void keepsTextForFixedImageClouds() {
		assertThat(CloudImagePainting.shouldKeepTextWithFixedImage(true, true, true)).isTrue();
	}

	@Test
	public void skipsFixedImageTextWhenImagePaintingIsDisabled() {
		assertThat(CloudImagePainting.shouldKeepTextWithFixedImage(false, true, true)).isFalse();
	}

	@Test
	public void skipsFixedImageTextWithoutFixedIcon() {
		assertThat(CloudImagePainting.shouldKeepTextWithFixedImage(true, false, true)).isFalse();
	}

	@Test
	public void skipsFixedImageTextWithoutImage() {
		assertThat(CloudImagePainting.shouldKeepTextWithFixedImage(true, true, false)).isFalse();
	}

	@Test
	public void keepsOpacityInsideRange() {
		assertThat(CloudImagePainting.opacityInRange(0.5d, 0.05d, 1d)).isEqualTo(0.5d);
	}

	@Test
	public void raisesOpacityToMinimum() {
		assertThat(CloudImagePainting.opacityInRange(0d, 0.05d, 1d)).isEqualTo(0.05d);
	}

	@Test
	public void lowersOpacityToMaximum() {
		assertThat(CloudImagePainting.opacityInRange(2d, 0.05d, 1d)).isEqualTo(1d);
	}
}
