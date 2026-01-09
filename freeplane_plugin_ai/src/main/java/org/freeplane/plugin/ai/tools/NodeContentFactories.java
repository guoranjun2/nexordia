package org.freeplane.plugin.ai.tools;

final class NodeContentFactories {
    final NodeContentItemReader nodeContentItemReader;
    final IconDescriptionResolver iconDescriptionResolver;

    NodeContentFactories(NodeContentItemReader nodeContentItemReader,
                         IconDescriptionResolver iconDescriptionResolver) {
        this.nodeContentItemReader = nodeContentItemReader;
        this.iconDescriptionResolver = iconDescriptionResolver;
    }
}
