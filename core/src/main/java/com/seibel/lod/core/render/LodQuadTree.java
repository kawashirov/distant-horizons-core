package com.seibel.lod.core.render;

import com.seibel.lod.core.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.renderfile.IRenderSourceProvider;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MathUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import org.apache.logging.log4j.Logger;

// QuadTree built from several layers of 2d ring buffers

/**
 * This quadTree structure is the core of the DH mod.
 * This class represent a circular quadTree of lodSection
 *
 * Each section at level n is populated in one (sometimes more than one) ways:
 *      -by constructing it from the data of all the children sections (lower levels)
 *      -by loading from file
 *      -by adding data with the lodBuilder
 */
public class LodQuadTree implements AutoCloseable {

    /**
     * Note: all config value should be via the class that extends this class, and
     *          by implementing different abstract methods
     */
    private static final byte LAYER_BEGINNING_OFFSET = ColumnRenderSource.SECTION_SIZE_OFFSET;
    private static final boolean SUPER_VERBOSE_LOGGING = false;
    public final byte getLayerDataDetailOffset(byte sectionDetail) {
        return ColumnRenderSource.SECTION_SIZE_OFFSET;
    }
    public final byte getLayerSectionDetailOffset(byte dataDetail) {
        return ColumnRenderSource.SECTION_SIZE_OFFSET;
    }
    public final byte getLayerDataDetail(byte sectionDetail) {
        return (byte) (sectionDetail - getLayerDataDetailOffset(sectionDetail));
    }
    public final byte getLayerSectionDetail(byte dataDetail) {
        return (byte) (dataDetail + getLayerSectionDetailOffset(dataDetail));
    }

    private static final Logger LOGGER = DhLoggerBuilder.getLogger("LodQuadTree");

    public final byte numbersOfSectionLevels;
    private final MovableGridRingList<LodRenderSection>[] ringLists;
	/** measured in blocks */
    public final int viewDistance;
    private final IRenderSourceProvider renderSourceProvider;

    private final IDhClientLevel level; //FIXME: Proper hierarchy to remove this reference!

    /**
     * Constructor of the quadTree
     * @param viewDistance View distance in blocks
     * @param initialPlayerX player x coordinate
     * @param initialPlayerZ player z coordinate
     */
    public LodQuadTree(IDhClientLevel level, int viewDistance, int initialPlayerX, int initialPlayerZ, IRenderSourceProvider provider) {
        DetailDistanceUtil.updateSettings(); //TODO: Move this to somewhere else
        this.level = level;
        renderSourceProvider = provider;
        this.viewDistance = viewDistance;

        { // Calculate the max section detail
            byte maxDataDetailLevel = getMaxDetailInRange(viewDistance * Math.sqrt(2));
            byte topSectionLevel = getLayerSectionDetail(maxDataDetailLevel);
            numbersOfSectionLevels = (byte) (topSectionLevel + 1);
            ringLists = new MovableGridRingList[numbersOfSectionLevels - LAYER_BEGINNING_OFFSET];
        }

        { // Construct the ringLists
            LOGGER.info("Creating ringLists with player center at {}", new Pos2D(initialPlayerX, initialPlayerZ));
            for (byte i = LAYER_BEGINNING_OFFSET; i < numbersOfSectionLevels; i++) {
                byte targetDataDetail = getLayerDataDetail(i);
                int maxDist = getFurthestDistance(targetDataDetail);
                int halfSize = MathUtil.ceilDiv(maxDist, (1 << i)) + 8; // +8 to make sure the section is fully contained in the ringList
                {
                    DhSectionPos checkerPos = new DhSectionPos(i, halfSize, halfSize);
                    byte checkedDetail = calculateExpectedDetailLevel(new DhBlockPos2D(initialPlayerX, initialPlayerZ),checkerPos);
                    LodUtil.assertTrue(checkedDetail > targetDataDetail,
                            "in {}, getFuthestDistance return {} which would be contained in range {}, but calculateExpectedDetailLevel at {} is {} <= {}",
                            i, maxDist, halfSize - 2, checkerPos, checkedDetail, targetDataDetail);
                }
                LOGGER.info("ringlist centered in {} with halfSize {} (maxDist {}, dataDetail {})", new Pos2D(initialPlayerX >> i, initialPlayerZ >> i), halfSize, maxDist, targetDataDetail);
                ringLists[i - LAYER_BEGINNING_OFFSET] = new MovableGridRingList<>(halfSize,
                        initialPlayerX >> i, initialPlayerZ >> i);
                LOGGER.info("Creating ringList {}: {}", i, ringLists[i - LAYER_BEGINNING_OFFSET].toString());
            }
        }
    }


    /**
     * This method return the LodSection given the Section Pos
     * @param pos the section positon.
     * @return the LodSection
     */
    public LodRenderSection getSection(DhSectionPos pos) {
        return getSection(pos.sectionDetailLevel, pos.sectionX, pos.sectionZ);
    }

    /**
     * This method returns the RingList of a given detail level
     * @apiNote The returned ringList should not be modified!
     * @param detailLevel the detail level
     * @return the RingList
     */
    public MovableGridRingList<LodRenderSection> getRingList(byte detailLevel) {
        return ringLists[detailLevel - LAYER_BEGINNING_OFFSET];
    }

    /**
     * This method returns the number of detail levels in the quadTree
     * @return the number of detail levels
     */
    public byte getNumbersOfSectionLevels() {
        return numbersOfSectionLevels;
    }

    public byte getStartingSectionLevel() {
        return LAYER_BEGINNING_OFFSET;
    }

    /**
     * This method return the LodSection at the given detail level and level coordinate x and z
     * @param detailLevel detail level of the section
     * @param x x coordinate of the section
     * @param z z coordinate of the section
     * @return the LodSection
     */
    public LodRenderSection getSection(byte detailLevel, int x, int z) {
        return ringLists[detailLevel - LAYER_BEGINNING_OFFSET].get(x, z);
    }

    
    /**
     * This method will compute the detail level based on player position and section pos
     * Override this method if you want to use a different algorithm
     * @param playerPos player position as a reference for calculating the detail level
     * @param sectionPos section position
     * @return detail level of this section pos
     */
    public byte calculateExpectedDetailLevel(DhBlockPos2D playerPos, DhSectionPos sectionPos) {
        return DetailDistanceUtil.getDetailLevelFromDistance(
                playerPos.dist(sectionPos.getCenter().getCenter()));
    }

    /**
     * The method will return the highest detail level in a circle around the center
     * Override this method if you want to use a different algorithm
     * Note: the returned distance should always be the ceiling estimation of the distance
     * //TODO: Make this input a bbox or a circle or something....
     * @param distance the circle radius
     * @return the highest detail level in the circle
     */
    public byte getMaxDetailInRange(double distance) {
        return DetailDistanceUtil.getDetailLevelFromDistance(distance);
    }

    /**
     * The method will return the furthest distance to the center for the given detail level
     * Override this method if you want to use a different algorithm
     * Note: the returned distance should always be the ceiling estimation of the distance
     * //TODO: Make this return a bbox instead of a distance in circle
     * @param detailLevel detail level
     * @return the furthest distance to the center, in blocks
     */
    public int getFurthestDistance(byte detailLevel) {
        return (int)Math.ceil(DetailDistanceUtil.getDrawDistanceFromDetail(detailLevel + 1));
        // +1 because that's the border to the next detail level, and we want to include up to it.
    }
    
    /**
     * Given a section pos at level n this method returns the parent section at level n+1
     * @param pos the section positon
     * @return the parent LodSection
     */
    public LodRenderSection getParentSection(DhSectionPos pos) {
        return getSection(pos.getParentPos());
    }
    
    /**
     * Given a section pos at level n and a child index this method return the
     * child section at level n-1
     * @param pos
     * @param child0to3 since there are 4 possible children this index identify which one we are getting
     * @return one of the child LodSection
     */
    public LodRenderSection getChildSection(DhSectionPos pos, int child0to3) {
        return getSection(pos.getChildByIndex(child0to3));
    }

    private LodRenderSection _set(MovableGridRingList<LodRenderSection> list, int x, int z, LodRenderSection t) {
        LodUtil.assertTrue(t != null, "setting null at [{},{}] in {}", x, z, list.toString());
        LodUtil.assertTrue(t.pos.sectionX == x && t.pos.sectionZ == z, "pos {} != [{},{}] in {}", t.pos, x, z, list.toString());
        LodRenderSection s = list.setChained(x,z,t);
        LodUtil.assertTrue(s != null, "returned null at [{},{}]: {}", x, z, list.toString());
        LodUtil.assertTrue(s == t,"{} != {} in {}",s,t, list.toString());
        return s;
    }
    private LodRenderSection _getNotNull(MovableGridRingList<LodRenderSection> list, int x, int z) {
        LodUtil.assertTrue(list.inRange(x,z), "[{},{}] not in range of {}", x, z, list.toString());
        LodRenderSection s = list.get(x,z);
        LodUtil.assertTrue(s != null, "getting null at [{},{}] in {}", x, z, list.toString());
        LodUtil.assertTrue(s.pos.sectionX == x && s.pos.sectionZ == z, "obj {} != [{},{}] in {}", s, x, z, list.toString());
        return s;
    }
    private LodRenderSection _get(MovableGridRingList<LodRenderSection> list, int x, int z) {
        LodRenderSection s = list.get(x,z);
        LodUtil.assertTrue(s == null || (s.pos.sectionX == x && s.pos.sectionZ == z), "obj {} != [{},{}] in {}", s, x, z, list.toString());
        return s;
    }

    /**
     * This function update the quadTree based on the playerPos and the current game configs (static and global)
     * @param playerPos the reference position for the player
     */
    public void tick(DhBlockPos2D playerPos) {
        for (int sectLevel = LAYER_BEGINNING_OFFSET; sectLevel < numbersOfSectionLevels; sectLevel++) {
            if (!ringLists[sectLevel - LAYER_BEGINNING_OFFSET].getCenter().equals(
                    new Pos2D(playerPos.x >> sectLevel, playerPos.z >> sectLevel))) {
                LOGGER.info("TreeTick: Moving ring list {} from {} to {}", sectLevel,
                        ringLists[sectLevel - LAYER_BEGINNING_OFFSET].getCenter(),
                        new Pos2D(playerPos.x >> sectLevel, playerPos.z >> sectLevel));
                ringLists[sectLevel - LAYER_BEGINNING_OFFSET]
                        .move(playerPos.x >> sectLevel, playerPos.z >> sectLevel,
                                LodRenderSection::dispose);
            }
        }

        // First tick pass: update all sections' childCount from bottom level to top level. Step:
        //   If sectLevel is bottom && section != null:
        //     - set childCount to 0
        //   If section != null && child != 0: //TODO: Should I move this createChild steps to Second tick pass?
        //     - // Section will be in the unloaded state.
        //     - create parent if not at final level and if it doesn't exist, with childCount = 1
        //     - for each child:
        //       - if null, create new with childCount = 0 (force load due to neighboring issues)
        //       - else if childCount == -1, set childCount = 0 (rescue it)
        //     - set childCount to 4
        //   Else:
        //     - Calculate targetLevel at that section
        //     - If sectLevel == numberOfSectionLevels - 1:
        //       - // Section is the top level.
        //       - If targetLevel > dataLevel@sectLevel && section != null:
        //         - set childCount to -1 (Signal that section is to be freed) (this prob not be rescued as it is the top level)
        //       - If targetLevel <= dataLevel@sectLevel && section == null: (direct use the current sectLevel's dataLevel)
        //         - create new section with childCount = 0
        //     - Else:
        //       - // Section is not the top level. So we also need to consider the parent.
        //       - If targetLevel >= dataLevel@(sectLevel+1) && section != null: (use the next level's dataLevel)
        //         - Parent's childCount-- (Assert parent != null && childCount > 0 before decrementing)
        //         - // Note that this doesn't necessarily mean this section will be freed as it may be rescued later
        //              due to neighboring quadrants not able to be freed (they pass targetLevel checks or has children)
        //              or due to parent's layer is in the Always Cascade mode. (containerType == null)
        //         - set childCount to -1 (Signal that this section will be freed if not rescued)
        //       - If targetLevel < dataLevel@(sectLevel+1) && section == null: (use the next level's dataLevel)
        //         - create new section with childCount = 0
        //         - Parent's childCount++ (Create parent if needed)
        for (byte sectLevel = LAYER_BEGINNING_OFFSET; sectLevel < numbersOfSectionLevels; sectLevel++) {
            final MovableGridRingList<LodRenderSection> ringList = ringLists[sectLevel - LAYER_BEGINNING_OFFSET];
            final MovableGridRingList<LodRenderSection> childRingList =
                    sectLevel == LAYER_BEGINNING_OFFSET ? null : ringLists[sectLevel - LAYER_BEGINNING_OFFSET - 1];
            final MovableGridRingList<LodRenderSection> parentRingList =
                    sectLevel == numbersOfSectionLevels - 1 ? null : ringLists[sectLevel - LAYER_BEGINNING_OFFSET + 1];
            final byte f_sectLevel = sectLevel;
            ringList.forEachPosOrdered((section, pos) -> {
                if (f_sectLevel == LAYER_BEGINNING_OFFSET && section != null) {
                    section.childCount = 0;
                    //LOGGER.info("sect {} in first layer with non-null. Reset childCount", section.pos);
                }
                if (section != null && section.childCount != 0) {
                    // Section will be in the unloaded state.
                    if (SUPER_VERBOSE_LOGGING) LOGGER.info("sect {} has child", section.pos);
                    if (parentRingList != null) {
                        LodRenderSection parent = _get(parentRingList, pos.x >> 1, pos.y >> 1);
                        if (parent == null) {
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("sect {} missing parent. Creating at {}", section.pos, section.pos.getParentPos());
                            parent = _set(parentRingList, pos.x >> 1, pos.y >> 1, new LodRenderSection(section.pos.getParentPos()));
                            parent.childCount++;
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("parent sect {} now has {} childs.", section.pos.getParentPos(), parent.childCount);
                        }
                        LodUtil.assertTrue(parent.childCount <= 4 && parent.childCount > 0);
                    }
                    for (byte i = 0; i < 4; i++) {
                        DhSectionPos childPos = section.pos.getChildByIndex(i);
                        LodUtil.assertTrue(childRingList != null);
                        LodRenderSection child = _get(childRingList, childPos.sectionX, childPos.sectionZ);
                        if (child == null) {
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("sect {} missing child at {}. Creating.", section.pos, childPos);
                            child = _set(childRingList, childPos.sectionX, childPos.sectionZ, new LodRenderSection(childPos));
                            child.childCount = 0;
                        } else if (child.childCount == -1) {
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("sect {} rescued child at {}.", section.pos, childPos);
                            child.childCount = 0;
                        }
                    }
                    section.childCount = 4;
                } else {
                    final DhSectionPos sectPos = section != null ? section.pos : new DhSectionPos(f_sectLevel, pos.x, pos.y);
                    LodUtil.assertTrue(sectPos.sectionDetailLevel == f_sectLevel
                    && sectPos.sectionX == pos.x && sectPos.sectionZ == pos.y,
                            "sectPos {} != {} @ {}", sectPos, pos, f_sectLevel);

                    byte targetLevel = calculateExpectedDetailLevel(playerPos, sectPos);
                    if (SUPER_VERBOSE_LOGGING) LOGGER.info("0 child sect {}(null?{}) - target:{}/{} (parent:{})", sectPos, section == null,
                            targetLevel, getLayerDataDetail(f_sectLevel),
                            f_sectLevel == numbersOfSectionLevels-1 ? "N/A" : getLayerDataDetail((byte) (f_sectLevel+1)));
                    if (f_sectLevel == numbersOfSectionLevels -1) {
                        // Section is in the top level.
                        if (targetLevel > getLayerDataDetail(f_sectLevel) && section != null) {
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("sect {} in top & target>current. Mark as free.", sectPos);
                            section.childCount = -1;
                        }
                        if (targetLevel <= getLayerDataDetail(f_sectLevel) && section == null) {
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("null sect {} in top & target<=current. Creating.", sectPos);
                            section = _set(ringList, pos.x, pos.y, new LodRenderSection(sectPos));
                        }
                    } else {
                        // Section is not the top level. So we also need to consider the parent.
                        if (targetLevel >= getLayerDataDetail((byte) (f_sectLevel+1)) && section != null) {
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("sect {} target>=nextLevel. Mark as free.", sectPos);
                            LodUtil.assertTrue(parentRingList != null);
                            LodRenderSection parent = _getNotNull(parentRingList, pos.x >> 1, pos.y >> 1);
                            LodUtil.assertTrue(parent.childCount <= 4 && parent.childCount > 0);
                            parent.childCount--;
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("parent sect {} now has {} child.", sectPos, parent.childCount);
                            section.childCount = -1;
                        }
                        if (targetLevel < getLayerDataDetail((byte) (f_sectLevel+1)) && section == null) {
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("null sect {} target<nextLevel. Creating.", sectPos);
                            section = _set(ringList, pos.x, pos.y, new LodRenderSection(sectPos));
                            LodUtil.assertTrue(parentRingList != null);
                            LodRenderSection parent = _get(parentRingList, pos.x >> 1, pos.y >> 1);
                            if (parent == null) {
                                if (SUPER_VERBOSE_LOGGING) LOGGER.info("sect {} missing parent. Creating at {}", sectPos, sectPos.getParentPos());
                                parent = _set(parentRingList, pos.x >> 1, pos.y >> 1, new LodRenderSection(sectPos.getParentPos()));
                            }
                            parent.childCount++;
                            if (SUPER_VERBOSE_LOGGING) LOGGER.info("parent sect {} now has {} childs.", sectPos.getParentPos(), parent.childCount);
                        }
                    }
                }
                // Final quick assert to insure section pos is correct.
                if (section != null) {
                    LodUtil.assertTrue(section.pos.sectionDetailLevel == f_sectLevel, "section.pos: " + section.pos + " vs level: " + f_sectLevel);
                    LodUtil.assertTrue(section.pos.sectionX == pos.x, "section.pos: " + section.pos + " vs pos: " + pos);
                    LodUtil.assertTrue(section.pos.sectionZ == pos.y, "section.pos: " + section.pos + " vs pos: " + pos);
                }
            });
        }

        // Second tick pass:
        // Cascade the layers that is in Always Cascade Mode from top to bottom. (Not yet exposed or used)
        // At the same time, load and unload sections (and can also be used to assert everything is working). Step:
        // ===Assertion steps===
        //   assert childCount == 4 || childCount == 0 || childCount == -1
        //   if childCount == 4 assert all children exist
        //   if childCount == 0 assert all children are null
        //   if childCount == -1 assert parent childCount is 0
        //   // ======================
        //   if childCount == 4 && section is loaded:
        //     - unload section
        //   if childCount == 0 && section is unloaded:
        //     - load section
        //   if childCount == -1: // (section can be loaded or unloaded, due to fast movement)
        //     - set this section to null (TODO: Is this needed to be first or last or don't matter for concurrency?)
        //     - If loaded unload section
        for (byte sectLevel = (byte) (numbersOfSectionLevels - 1); sectLevel >= LAYER_BEGINNING_OFFSET; sectLevel--) {
            final MovableGridRingList<LodRenderSection> ringList = ringLists[sectLevel - LAYER_BEGINNING_OFFSET];
            final MovableGridRingList<LodRenderSection> childRingList =
                    sectLevel == LAYER_BEGINNING_OFFSET ? null : ringLists[sectLevel - LAYER_BEGINNING_OFFSET - 1];
            final boolean doCascade = false; // TODO: Utilize this cascade mode or at least expose this option
            ringList.forEachPosOrdered((section, pos) -> {
                if (section == null) return;

                // Cascade layers
//                if (doCascade && section.childCount == 0) {
//                    LodUtil.assertTrue(childRingList != null);
//                    // Create childs to cascade the layer.
//                    for (byte i = 0; i < 4; i++) {
//                        DhSectionPos childPos = section.pos.getChild(i);
//                        LodRenderSection child = childRingList.get(childPos.sectionX, childPos.sectionZ);
//                        if (child == null) {
//                            child = childRingList.setChained(childPos.sectionX, childPos.sectionZ,
//                                    new LodRenderSection(childPos));
//                            child.childCount = 0;
//                        } else {
//                            LodUtil.assertTrue(child.childCount == -1,
//                                    "Self has child count 0 but an existing child's child count != -1!");
//                            child.childCount = 0;
//                        }
//                    }
//                    section.childCount = 4;
//                }

                // Call load on new sections, and tick on existing ones, and dispose old sections
                if (section.childCount == -1) {
                    if (section.pos.sectionDetailLevel < numbersOfSectionLevels-1)
                        LodUtil.assertTrue(getParentSection(section.pos).childCount == 0);
                    ringList.set(pos.x, pos.y, null);
                    section.dispose();
                    return;
                } else {
                    if (!section.isLoaded() && !section.isLoading()) {
                        section.load(renderSourceProvider);
                    } else if (section.isOutdated()) {
                        section.reload(renderSourceProvider);
                    }
                    if (section.childCount == 4) section.disableRender();
                    if (section.childCount == 0) section.enableRender(level, this);
                    section.tick(this, level);
                }

                // Assertion steps
                LodUtil.assertTrue(section.childCount == 4 || section.childCount == 0);
                if (section.pos.sectionDetailLevel == LAYER_BEGINNING_OFFSET) LodUtil.assertTrue(section.childCount == 0);
                if (section.pos.sectionDetailLevel != LAYER_BEGINNING_OFFSET) {
                    LodRenderSection child0 = getChildSection(section.pos, 0);
                    LodRenderSection child1 = getChildSection(section.pos, 1);
                    LodRenderSection child2 = getChildSection(section.pos, 2);
                    LodRenderSection child3 = getChildSection(section.pos, 3);
                    if (section.childCount == 4) LodUtil.assertTrue(
                            child0 != null && child0.childCount != -1 &&
                            child1 != null && child1.childCount != -1 &&
                            child2 != null && child2.childCount != -1 &&
                            child3 != null && child3.childCount != -1,
                            "Sect {} child count 4 but child has null or is being disposed: {} {} {} {}",
                            section.pos, child0, child1, child2, child3);

                    if (section.childCount == 0) LodUtil.assertTrue(
                            (child0 == null || child0.childCount == -1) &&
                            (child1 == null || child1.childCount == -1) &&
                            (child2 == null || child2.childCount == -1) &&
                            (child3 == null || child3.childCount == -1),
                            "Sect {} child count 0 but child is neither null or being disposed: {} {} {} {}",
                            section.pos, child0, child1, child2, child3);
                }
            });
        }
    }

    public String getDebugString() {
        StringBuilder sb = new StringBuilder();
        for (byte i = 0; i < ringLists.length; i++) {
            sb.append("Layer ").append(i + LAYER_BEGINNING_OFFSET).append(":\n");
            sb.append(ringLists[i].toDetailString());
            sb.append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public void close() {
        for (MovableGridRingList<LodRenderSection> ringList : ringLists) {
            ringList.forEach((section) -> {
                if (section != null) section.dispose();
            });
        }

    }
}
