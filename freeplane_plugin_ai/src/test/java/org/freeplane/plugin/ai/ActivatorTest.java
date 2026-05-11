package org.freeplane.plugin.ai;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import org.freeplane.plugin.ai.chat.AIChatPanel;
import org.freeplane.plugin.ai.mcpserver.ModelContextProtocolServer;
import org.freeplane.plugin.ai.prompt.AiPromptActionRegistry;
import org.junit.Test;

public class ActivatorTest {

    @Test
    public void stop_persistsChatPromptStateAndStopsMcpServer() throws Exception {
        Activator activator = new Activator();
        AIChatPanel aiChatPanel = mock(AIChatPanel.class);
        AiPromptActionRegistry promptActionRegistry = mock(AiPromptActionRegistry.class);
        ModelContextProtocolServer modelContextProtocolServer = mock(ModelContextProtocolServer.class);
        setField(activator, "aiChatPanel", aiChatPanel);
        setField(activator, "promptActionRegistry", promptActionRegistry);
        setField(activator, "modelContextProtocolServer", modelContextProtocolServer);

        activator.stop(null);

        verify(aiChatPanel).persistCurrentChatIfNeeded();
        verify(promptActionRegistry).persistStateIfChanged();
        verify(modelContextProtocolServer).stop();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = Activator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
