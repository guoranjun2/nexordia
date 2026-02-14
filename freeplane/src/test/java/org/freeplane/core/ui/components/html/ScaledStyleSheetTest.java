package org.freeplane.core.ui.components.html;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.text.AttributeSet;

import org.freeplane.core.ui.components.UITools;
import org.junit.Test;

public class ScaledStyleSheetTest {

    @Test
    public void noArgConstructorUsesDefaultFontScaleFactor() {
        ScaledStyleSheet uut = new ScaledStyleSheet();
        uut.addRule("body { font-size: 10pt; }");

        AttributeSet rule = uut.getRule("body");

        assertThat(uut.getFont(rule).getSize()).isEqualTo(Math.round(10f * UITools.FONT_SCALE_FACTOR));
    }

    @Test
    public void constructorWithScaleUsesProvidedFontScaleFactor() {
        ScaledStyleSheet uut = new ScaledStyleSheet(2f);
        uut.addRule("body { font-size: 10pt; }");

        AttributeSet rule = uut.getRule("body");

        assertThat(uut.getFont(rule).getSize()).isEqualTo(20);
    }

    @Test
    public void inheritedRulesFromParentStyleSheetUseConfiguredScaleFactor() {
        ScaledStyleSheet parentStyleSheet = new ScaledStyleSheet(1.5f);
        parentStyleSheet.addRule("body { font-size: 10pt; }");
        ScaledStyleSheet uut = new ScaledStyleSheet(1.5f);
        uut.addStyleSheet(parentStyleSheet);

        AttributeSet rule = uut.getRule("body");

        assertThat(uut.getFont(rule).getSize()).isEqualTo(15);
    }

    @Test
    public void relativeFontSizeRulesUseScaleFactorFromParentChain() {
        ScaledStyleSheet parentStyleSheet = new ScaledStyleSheet(1.5f);
        parentStyleSheet.addRule("body { font-size: 10pt; }");
        ScaledStyleSheet uut = new ScaledStyleSheet(1.5f);
        uut.addStyleSheet(parentStyleSheet);
        uut.addRule("h1 { font-size: 120%; }");

        AttributeSet rule = uut.getRule("h1");

        assertThat(uut.getFont(rule).getSize()).isEqualTo(18);
    }
}
