package org.freeplane.view.swing.map.outline;

class OutlineVisibleBlockRange {
    private final int firstBlock;
    private final int lastBlock;
    private final int breadcrumbHeight;

    OutlineVisibleBlockRange(int firstBlock, int lastBlock, int breadcrumbHeight) {
        this.firstBlock = firstBlock;
        this.lastBlock = lastBlock;
        this.breadcrumbHeight = breadcrumbHeight;
    }

    int getFirstBlock() { return firstBlock; }
    int getLastBlock() { return lastBlock; }
    int getBreadcrumbHeight() { return breadcrumbHeight; }

    @Override
    public String toString() {
        return "OutlineVisibleBlockRange [firstBlock=" + firstBlock + ", lastBlock=" + lastBlock
                + ", breadcrumbHeight=" + breadcrumbHeight + "]";
    }
}

