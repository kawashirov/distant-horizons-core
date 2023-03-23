/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package tests;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.objects.quadTree.QuadTree;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class QuadTreeTest
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private static final DhBlockPos2D TREE_CENTER_POS = new DhBlockPos2D(BitShiftUtil.powerOfTwo(10)/2, BitShiftUtil.powerOfTwo(10)/2);
	
	private static final int ROOT_NODE_WIDTH_IN_BLOCKS = BitShiftUtil.powerOfTwo(10);
	/** needs to be an odd number to function correctly */
	private static final int BASIC_TREE_INPUT_WIDTH_IN_ROOT_NODES = 9;
	private static final int BASIC_TREE_WIDTH_IN_BLOCKS = ROOT_NODE_WIDTH_IN_BLOCKS * BASIC_TREE_INPUT_WIDTH_IN_ROOT_NODES;
	
	/** the tree should be slightly larger to account for offset centers */
	private static final int BASIC_TREE_ACTUAL_WIDTH_IN_ROOT_NODES = BASIC_TREE_INPUT_WIDTH_IN_ROOT_NODES + 2;
	
	private static final int MINIMUM_TREE_WIDTH_IN_BLOCKS = 32;
	
	static
	{
		Configurator.setRootLevel(Level.ALL);
	}
	
	
	
	@Test
	public void BasicPositiveQuadTreeTest()
	{
		QuadTree<Integer> tree = new QuadTree<>(BASIC_TREE_WIDTH_IN_BLOCKS, TREE_CENTER_POS);
		Assert.assertEquals("Incorrect basic tree width", BASIC_TREE_ACTUAL_WIDTH_IN_ROOT_NODES, tree.ringListWidth());
		
		
		// root node //
		testSet(tree, new DhSectionPos((byte)10, 0, 0), 0);
		
		// first child (0,0) //
		testSet(tree, new DhSectionPos((byte)9, 0, 0), 1);
		testSet(tree, new DhSectionPos((byte)9, 1, 0), 2);
		testSet(tree, new DhSectionPos((byte)9, 0, 1), 3);
		testSet(tree, new DhSectionPos((byte)9, 1, 1), 4);
		
		// second child (0,0) (0,0) //
		testSet(tree, new DhSectionPos((byte)8, 0, 0), 5);
		testSet(tree, new DhSectionPos((byte)8, 1, 0), 6);
		testSet(tree, new DhSectionPos((byte)8, 0, 1), 7);
		testSet(tree, new DhSectionPos((byte)8, 1, 1), 8);
		// second child (0,0) (1,1) //
		testSet(tree, new DhSectionPos((byte)8, 2, 2), 9);
		testSet(tree, new DhSectionPos((byte)8, 3, 2), 10);
		testSet(tree, new DhSectionPos((byte)8, 2, 3), 11);
		testSet(tree, new DhSectionPos((byte)8, 3, 3), 12);
		
		// third child (0,0) (1,0) (0,0) //
		testSet(tree, new DhSectionPos((byte)7, 5, 0), 9);
		testSet(tree, new DhSectionPos((byte)7, 6, 0), 10);
		testSet(tree, new DhSectionPos((byte)7, 5, 1), 11);
		testSet(tree, new DhSectionPos((byte)7, 6, 1), 12);
		
	}
	
	@Test
	public void BasicNegativeQuadTreeTest()
	{
		QuadTree<Integer> tree = new QuadTree<>(BASIC_TREE_WIDTH_IN_BLOCKS, TREE_CENTER_POS);
		
		
		// root node //
		testSet(tree, new DhSectionPos((byte)10, -1, -1), 0);
		
		// first child (-1,-1) //
		testSet(tree, new DhSectionPos((byte)9, -2, -1), 1);
		testSet(tree, new DhSectionPos((byte)9, -1, -1), 2);
		testSet(tree, new DhSectionPos((byte)9, -2, -2), 3);
		testSet(tree, new DhSectionPos((byte)9, -1, -2), 4);
		
		// TODO
//		// second child (-1,-1) (0,0) //
//		runTest(tree, new DhSectionPos((byte)8, 0, 0), 5);
//		runTest(tree, new DhSectionPos((byte)8, 1, 0), 6);
//		runTest(tree, new DhSectionPos((byte)8, 0, 1), 7);
//		runTest(tree, new DhSectionPos((byte)8, 1, 1), 8);
//		// second child (-1,-1) (1,1) //
//		runTest(tree, new DhSectionPos((byte)8, 2, 2), 9);
//		runTest(tree, new DhSectionPos((byte)8, 3, 2), 10);
//		runTest(tree, new DhSectionPos((byte)8, 2, 3), 11);
//		runTest(tree, new DhSectionPos((byte)8, 3, 3), 12);
//
//		// third child (-1,-1) (1,0) (0,0) //
//		runTest(tree, new DhSectionPos((byte)7, 5, 0), 9);
//		runTest(tree, new DhSectionPos((byte)7, 6, 0), 10);
//		runTest(tree, new DhSectionPos((byte)7, 5, 1), 11);
//		runTest(tree, new DhSectionPos((byte)7, 6, 1), 12);
		
	}
	
	@Test
	public void OutOfBoundsQuadTreeTest()
	{
		QuadTree<Integer> tree = new QuadTree<>(BASIC_TREE_WIDTH_IN_BLOCKS, new DhBlockPos2D(0,0));
		Assert.assertEquals("tree diameter incorrect", BASIC_TREE_WIDTH_IN_BLOCKS, tree.diameterInBlocks());
		
		
		// wrong detail level on purpose, if the detail level was 0 (block) this should work
		DhSectionPos outOfBoundsPos = new DhSectionPos(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, ROOT_NODE_WIDTH_IN_BLOCKS, 0);
		testSet(tree, outOfBoundsPos, -1, IndexOutOfBoundsException.class);
		Assert.assertEquals("incorrect leaf node count", 0, tree.leafNodeCount());
		
		
		// out of bounds //
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (BASIC_TREE_WIDTH_IN_BLOCKS/2) + 1, 0);
		testSet(tree, outOfBoundsPos, -1, IndexOutOfBoundsException.class);
		Assert.assertEquals("incorrect leaf node count", 0, tree.leafNodeCount());
		
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (BASIC_TREE_WIDTH_IN_BLOCKS/2), 0);
		testSet(tree, outOfBoundsPos, -1, IndexOutOfBoundsException.class);
		Assert.assertEquals("incorrect leaf node count", 0, tree.leafNodeCount());
		
		
		// in bounds //
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (BASIC_TREE_WIDTH_IN_BLOCKS/2)-1, 0);
		testSet(tree, outOfBoundsPos, 0);
		Assert.assertEquals("incorrect leaf node count", 1, tree.leafNodeCount());
		
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (BASIC_TREE_WIDTH_IN_BLOCKS/2)-3, 0);
		testSet(tree, outOfBoundsPos, 0);
		Assert.assertEquals("incorrect leaf node count", 2, tree.leafNodeCount());
		
		// TODO this position probably has trouble with getting the center.
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (BASIC_TREE_WIDTH_IN_BLOCKS/2)-2, 0);
		testSet(tree, outOfBoundsPos, 0);
		Assert.assertEquals("incorrect leaf node count", 3, tree.leafNodeCount());
		
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (BASIC_TREE_WIDTH_IN_BLOCKS/2)-4, 0);
		testSet(tree, outOfBoundsPos, 0);
		Assert.assertEquals("incorrect leaf node count", 4, tree.leafNodeCount());
		
	}
	
	@Test
	public void QuadTreeRootAlignedMovingTest()
	{
		int treeWidthInRootNodes = 8;
		int treeWidthInBlocks = ROOT_NODE_WIDTH_IN_BLOCKS * treeWidthInRootNodes;
		QuadTree<Integer> tree = new QuadTree<>(treeWidthInBlocks, TREE_CENTER_POS);
		
		
		// root nodes //
		testSet(tree, new DhSectionPos((byte)10, 0, 0), 1);
		
		// first child (0,0) //
		DhSectionPos nw = new DhSectionPos(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, 0, 0);
		DhSectionPos ne = new DhSectionPos(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, 1, 0);
		DhSectionPos sw = new DhSectionPos(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, 0, 1);
		DhSectionPos se = new DhSectionPos(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, 1, 1);
		
		testSet(tree, nw, 2);
		testSet(tree, ne, 3);
		testSet(tree, sw, 4);
		testSet(tree, se, 5);
		Assert.assertEquals("incorrect leaf node count", tree.leafNodeCount(), 4);
		
		
		// fake move //
		tree.setCenterBlockPos(DhBlockPos2D.ZERO);
		Assert.assertEquals("Tree center incorrect", DhBlockPos2D.ZERO, tree.getCenterBlockPos());
		
		testGet(tree, nw, 2);
		testGet(tree, ne, 3);
		testGet(tree, sw, 4);
		testGet(tree, se, 5);
		Assert.assertEquals("incorrect leaf node count", tree.leafNodeCount(), 4);
		
		
		// small move //
		DhBlockPos2D smallMoveBlockPos = new DhBlockPos2D(ROOT_NODE_WIDTH_IN_BLOCKS*2, 0); // move enough that the original root nodes aren't touching the same grid squares they were before, but not far enough as to be garbage collected (TODO reword)
		tree.setCenterBlockPos(smallMoveBlockPos);
		Assert.assertEquals("Tree center incorrect", smallMoveBlockPos, tree.getCenterBlockPos());
		
		// nodes should be found at the same locations
		testGet(tree, nw, 2);
		testGet(tree, ne, 3);
		testGet(tree, sw, 4);
		testGet(tree, se, 5);
		Assert.assertEquals("incorrect leaf node count", tree.leafNodeCount(), 4);
		
		
		
		// big move //
		DhBlockPos2D bigMoveBlockPos = new DhBlockPos2D(treeWidthInBlocks * 2, 0);
		tree.setCenterBlockPos(bigMoveBlockPos);
		Assert.assertEquals("Tree center incorrect", bigMoveBlockPos, tree.getCenterBlockPos());
		
		// nothing should be found in the tree
		testGet(tree, nw, null, IndexOutOfBoundsException.class);
		testGet(tree, ne, null, IndexOutOfBoundsException.class);
		testGet(tree, sw, null, IndexOutOfBoundsException.class);
		testGet(tree, se, null, IndexOutOfBoundsException.class);
		
		Assert.assertEquals("incorrect leaf node count", tree.leafNodeCount(), 0);
		
		
		
		// edge move //
		
		// move back to the origin for easy testing
		tree.setCenterBlockPos(DhBlockPos2D.ZERO);
		Assert.assertEquals("Tree center incorrect", DhBlockPos2D.ZERO, tree.getCenterBlockPos());
		
		// on the negative X edge
		DhSectionPos edgePos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, -treeWidthInBlocks/2, 0);
		testSet(tree, edgePos, 1);
		Assert.assertEquals("incorrect leaf node count", 1, tree.leafNodeCount());
		
		// +1 root node from the negative X edge
		DhSectionPos adjacentEdgePos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (-treeWidthInBlocks/2)+ROOT_NODE_WIDTH_IN_BLOCKS, 0);
		testSet(tree, adjacentEdgePos, 2);
		Assert.assertEquals("incorrect leaf node count", 2, tree.leafNodeCount());
		
		// move so only the root nodes exactly on the X edge remain
		DhBlockPos2D edgeMoveBlockPos = new DhBlockPos2D(ROOT_NODE_WIDTH_IN_BLOCKS - (BASIC_TREE_INPUT_WIDTH_IN_ROOT_NODES*ROOT_NODE_WIDTH_IN_BLOCKS), 0);
		tree.setCenterBlockPos(edgeMoveBlockPos);
		Assert.assertEquals("Tree center incorrect", edgeMoveBlockPos, tree.getCenterBlockPos());
		
		Assert.assertEquals("incorrect leaf node count", 2, tree.leafNodeCount());
		
	}
	
	@Test
	public void QuadTreeIterationTest()
	{
		QuadTree<Integer> tree = new QuadTree<>(BASIC_TREE_WIDTH_IN_BLOCKS, TREE_CENTER_POS);
		
		
		// root nodes //
		testSet(tree, new DhSectionPos((byte)10, 0, 0), 1);
		testSet(tree, new DhSectionPos((byte)10, 1, 0), 2);
		
		// first child (0,0) //
		testSet(tree, new DhSectionPos((byte)9, 0, 0), 3);
		testSet(tree, new DhSectionPos((byte)9, 1, 0), 4);
		testSet(tree, new DhSectionPos((byte)9, 0, 1), 5);
		testSet(tree, new DhSectionPos((byte)9, 1, 1), 6);
		
		
		final AtomicInteger rootNodeCount = new AtomicInteger(0);
		final AtomicInteger leafCount = new AtomicInteger(0);
		final AtomicInteger leafValueSum = new AtomicInteger(0);
		tree.forEachRootNode((rootNode) -> 
		{
			rootNodeCount.addAndGet(1);
			
			rootNode.forAllLeafValues((leafValue) -> 
			{
				leafCount.addAndGet(1);
				leafValueSum.addAndGet(leafValue);
			});	
		});
		
		Assert.assertEquals("incorrect root count", 2, rootNodeCount.get());
		Assert.assertEquals("incorrect leaf count", 5, leafCount.get());
		Assert.assertEquals("incorrect leaf value sum", 20, leafValueSum.get());
		
	}
	
	@Test
	public void CenteredGridListIterationTest()
	{
		final QuadTree<Integer> tree = new QuadTree<>(MINIMUM_TREE_WIDTH_IN_BLOCKS, TREE_CENTER_POS);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 0, 0), 0);
		
		// confirm the root node were added
		final AtomicInteger rootNodeCount = new AtomicInteger(0);
		tree.forEachRootNode((rootNode) -> { rootNodeCount.addAndGet(1); });
		Assert.assertEquals("incorrect root count", 1, rootNodeCount.get());
		
		// attempt to get and remove, each node in the tree
		final AtomicInteger rootNodePosCount = new AtomicInteger(0);
		tree.forEachRootNodePos((renderBufferNode, pos2d) ->
		{
			DhSectionPos sectionPos = new DhSectionPos(tree.treeMaxDetailLevel, pos2d.x, pos2d.y);
			
			testGet(tree, sectionPos, 0);
			testSet(tree, sectionPos, null);
			
			rootNodePosCount.addAndGet(1);
		});
		Assert.assertEquals("incorrect root count", 1, rootNodeCount.get());
		
	}
	
	@Test
	public void OffsetGridListIterationTest()
	{
		
		// offset fully inside (10*0,0)
		final QuadTree<Integer> fullyInsideTree = new QuadTree<>(MINIMUM_TREE_WIDTH_IN_BLOCKS, TREE_CENTER_POS);
		
		DhBlockPos2D fullyInsideOffsetBlockPos = new DhBlockPos2D(MINIMUM_TREE_WIDTH_IN_BLOCKS, MINIMUM_TREE_WIDTH_IN_BLOCKS);
		fullyInsideTree.setCenterBlockPos(fullyInsideOffsetBlockPos);
		
		fullyInsideTree.forEachRootNodePos((rootNode, pos2D) ->
		{
			testSet(fullyInsideTree, new DhSectionPos(fullyInsideTree.treeMaxDetailLevel, pos2D.x, pos2D.y), 0);
		});
		
		// only 1 root node should be added
		final AtomicInteger fullyInsideRootNodeCount = new AtomicInteger(0);
		fullyInsideTree.forEachRootNode((rootNode) -> { fullyInsideRootNodeCount.addAndGet(1); });
		Assert.assertEquals("incorrect root count", 1, fullyInsideRootNodeCount.get());
		
		
		
		
		// offset fully inside (10*0,0)
		final QuadTree<Integer> borderInsideTree = new QuadTree<>(MINIMUM_TREE_WIDTH_IN_BLOCKS, new DhBlockPos2D(MINIMUM_TREE_WIDTH_IN_BLOCKS * 2, MINIMUM_TREE_WIDTH_IN_BLOCKS * 2));
		
		borderInsideTree.forEachRootNodePos((rootNode, pos2D) ->
		{
			testSet(borderInsideTree, new DhSectionPos(borderInsideTree.treeMaxDetailLevel, pos2D.x, pos2D.y), 0);
		});
		
		// only 1 root node should be added
		final AtomicInteger borderInsideRootNodeCount = new AtomicInteger(0);
		borderInsideTree.forEachRootNode((rootNode) -> { borderInsideRootNodeCount.addAndGet(1); });
		Assert.assertEquals("incorrect root count", 1, borderInsideRootNodeCount.get());
		
		
		
		
		// offset across (10*-1,0) and (10*0,0)
		final QuadTree<Integer> acrossTree = new QuadTree<>(MINIMUM_TREE_WIDTH_IN_BLOCKS, TREE_CENTER_POS);

		DhBlockPos2D acrossOffsetBlockPos = new DhBlockPos2D(-MINIMUM_TREE_WIDTH_IN_BLOCKS/4, MINIMUM_TREE_WIDTH_IN_BLOCKS);
		acrossTree.setCenterBlockPos(acrossOffsetBlockPos);

		acrossTree.forEachRootNodePos((rootNode, pos2D) ->
		{
			testSet(acrossTree, new DhSectionPos(acrossTree.treeMaxDetailLevel, pos2D.x, pos2D.y), 0);
		});

		// 2 root nodes should be added
		final AtomicInteger acrossRootNodeCount = new AtomicInteger(0);
		acrossTree.forEachRootNode((rootNode) -> { acrossRootNodeCount.addAndGet(1); });
		Assert.assertEquals("incorrect root count", 2, acrossRootNodeCount.get());
		
	}
	
	@Test
	public void TinyGridAlignedTreeTest()
	{
		QuadTree<Integer> tree = new QuadTree<>(ROOT_NODE_WIDTH_IN_BLOCKS, TREE_CENTER_POS);
		// minimum size tree should be 3 root nodes wide
		Assert.assertEquals("incorrect tree node width", 3, tree.ringListWidth());
		Assert.assertEquals("incorrect tree width", ROOT_NODE_WIDTH_IN_BLOCKS, tree.diameterInBlocks());
		
		
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 0, 0), 0);
		
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, -1, -1), -1, IndexOutOfBoundsException.class);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 1, 1), -1, IndexOutOfBoundsException.class);
		
		AtomicInteger rootCount = new AtomicInteger(0);
		tree.forEachRootNode((rootNode) -> 
		{
			rootCount.getAndAdd(1);
		});
		Assert.assertEquals("incorrect leaf value sum", 1, rootCount.get());
		
	}
	
	@Test
	public void TinyGridOffsetTreeTest()
	{
		QuadTree<Integer> tree = new QuadTree<>(ROOT_NODE_WIDTH_IN_BLOCKS, new DhBlockPos2D(0, 0));
		// minimum size tree should be 3 root nodes wide
		Assert.assertEquals("incorrect tree node width", 3, tree.ringListWidth());
		Assert.assertEquals("incorrect tree width", ROOT_NODE_WIDTH_IN_BLOCKS, tree.diameterInBlocks());
		
		
		// 2x2 valid positions (overlap the tree's width)
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 0, 0), 0);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, -1, 0), 0);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 0, -1), 0);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, -1, -1), 0);
		
		// invalid positions
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, -1, 1), -1, IndexOutOfBoundsException.class);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 0, 1), -1, IndexOutOfBoundsException.class);
		
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 1, 0), -1, IndexOutOfBoundsException.class);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 1, 1), -1, IndexOutOfBoundsException.class);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 1, -1), -1, IndexOutOfBoundsException.class);
		
		
		AtomicInteger rootCount = new AtomicInteger(0);
		tree.forEachRootNode((rootNode) -> 
		{
			rootCount.getAndAdd(1);
		});
		Assert.assertEquals("incorrect leaf value sum", 4, rootCount.get());
		
	}
	
	
	
	
	//================//
	// helper methods //
	//================//
	
	private static void testSet(QuadTree<Integer> tree, DhSectionPos pos, Integer setValue) { testSet(tree, pos, setValue, null); }
	private static <TE extends Throwable> void testSet(QuadTree<Integer> tree, DhSectionPos pos, Integer setValue, Class<TE> expectedExceptionClass)
	{
		// set
		try
		{
			Integer previousValue = tree.set(pos, setValue);
		}
		catch (Exception e)
		{
			if (expectedExceptionClass == null || e.getClass() != expectedExceptionClass)
			{
				e.printStackTrace();
				Assert.fail("set failed "+pos+" with exception "+e.getClass()+", expected exception: "+expectedExceptionClass+". error: "+e.getMessage());
			}
		}
		
		
		// get (confirm value was correctly set)
		testGet(tree, pos, setValue, expectedExceptionClass);
	}
	
	private static void testGet(QuadTree<Integer> tree, DhSectionPos pos, Integer getValue) { testSet(tree, pos, getValue, null); }
	private static <TE extends Throwable> void testGet(QuadTree<Integer> tree, DhSectionPos pos, Integer getValue, Class<TE> expectedExceptionClass)
	{
		try
		{
			Integer getResult = tree.get(pos);
			Assert.assertEquals("get failed "+pos, getValue, getResult);
		}
		catch (Exception e)
		{
			if (expectedExceptionClass == null || e.getClass() != expectedExceptionClass)
			{
				e.printStackTrace();
				Assert.fail("get failed "+pos+" with exception "+e.getClass()+", expected exception: "+expectedExceptionClass+". error: "+e.getMessage());
			}
		}
	}
	
	
}
