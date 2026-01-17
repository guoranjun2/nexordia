package org.freeplane.plugin.ai.tools.content;

public class NodeContentFactories {
	public final NodeContentItemReader nodeContentItemReader;
	public final IconDescriptionResolver iconDescriptionResolver;

    public NodeContentFactories(NodeContentItemReader nodeContentItemReader,
                         IconDescriptionResolver iconDescriptionResolver) {
        this.nodeContentItemReader = nodeContentItemReader;
        this.iconDescriptionResolver = iconDescriptionResolver;
    }
}
