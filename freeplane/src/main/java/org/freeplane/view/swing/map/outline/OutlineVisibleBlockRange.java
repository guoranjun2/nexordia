package org.freeplane.view.swing.map.outline;

class OutlineVisibleBlockRange {
    private final int firstBlock;
    private final int lastBlock;
    private final int breadcrumbAreaHeight;

    OutlineVisibleBlockRange(int firstBlock, int lastBlock, int breadcrumbAreaHeight) {
        this.firstBlock = firstBlock;
        this.lastBlock = lastBlock;
        this.breadcrumbAreaHeight = breadcrumbAreaHeight;
    }

    int getFirstBlock() { return firstBlock; }
    int getLastBlock() { return lastBlock; }
    int getBreadcrumbAreaHeight() { return breadcrumbAreaHeight; }

    @Override
    public String toString() {
        return "OutlineVisibleBlockRange [firstBlock=" + firstBlock + ", lastBlock=" + lastBlock
                + ", breadcrumbAreaHeight=" + breadcrumbAreaHeight + "]";
    }
}

