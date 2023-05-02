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
import com.seibel.lod.coreapi.util.BitShiftUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.coreapi.util.MathUtil;
import com.seibel.lod.core.util.objects.quadTree.QuadNode;
import com.seibel.lod.core.util.objects.quadTree.QuadTree;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class QuadTreeTest
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	static
	{
		// by default Log4J doesn't log Info's, which can be a problem when debugging
		Configurator.setRootLevel(Level.ALL);
	}
	
	
	
	@Test
	public void BasicPositiveQuadTreeTest()
	{
		AbstractTestTreeParams treeParams = new LargeTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), treeParams.getPositiveEdgeCenterPos(), LodUtil.BLOCK_DETAIL_LEVEL);
		Assert.assertTrue("Tree min/max detail level out of expected bounds: "+tree, tree.treeMaxDetailLevel >= 10 && tree.treeMinDetailLevel <= 10 - 4);
		
		
		// (pseudo) root node //
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
		AbstractTestTreeParams treeParams = new LargeTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), DhBlockPos2D.ZERO, LodUtil.BLOCK_DETAIL_LEVEL);
		
		
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
		AbstractTestTreeParams treeParams = new LargeTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), new DhBlockPos2D(0,0), LodUtil.BLOCK_DETAIL_LEVEL);
		Assert.assertEquals("tree diameter incorrect", treeParams.getWidthInBlocks(), tree.diameterInBlocks());
		
		
		// wrong detail level on purpose, if the detail level was 0 (block) this should work
		DhSectionPos outOfBoundsPos = new DhSectionPos(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, (treeParams.getWidthInBlocks()/2), 0);
		testSet(tree, outOfBoundsPos, -1, IndexOutOfBoundsException.class);
		Assert.assertEquals("incorrect leaf node count", 0, tree.leafNodeCount());
		
		
		// out of bounds //
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (treeParams.getWidthInBlocks()/2) + 1, 0);
		testSet(tree, outOfBoundsPos, -1, IndexOutOfBoundsException.class);
		Assert.assertEquals("incorrect leaf node count", 0, tree.leafNodeCount());
		
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (treeParams.getWidthInBlocks()/2), 0);
		testSet(tree, outOfBoundsPos, -1, IndexOutOfBoundsException.class);
		Assert.assertEquals("incorrect leaf node count", 0, tree.leafNodeCount());
		
		
		// in bounds //
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (treeParams.getWidthInBlocks()/2)-1, 0);
		testSet(tree, outOfBoundsPos, 0);
		Assert.assertEquals("incorrect leaf node count", 1, tree.leafNodeCount());
		
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (treeParams.getWidthInBlocks()/2)-3, 0);
		testSet(tree, outOfBoundsPos, 0);
		Assert.assertEquals("incorrect leaf node count", 2, tree.leafNodeCount());
		
		// TODO this position probably has trouble with getting the center.
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (treeParams.getWidthInBlocks()/2)-2, 0);
		testSet(tree, outOfBoundsPos, 0);
		Assert.assertEquals("incorrect leaf node count", 3, tree.leafNodeCount());
		
		outOfBoundsPos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (treeParams.getWidthInBlocks()/2)-4, 0);
		testSet(tree, outOfBoundsPos, 0);
		Assert.assertEquals("incorrect leaf node count", 4, tree.leafNodeCount());
		
	}
	@Test
	public void outOfBoundsInTreeTest()
	{
		// very specific tree parameters to match test results
		QuadTree<Integer> tree = new QuadTree<>(512, new DhBlockPos2D(125, -516), (byte)6);
		Assert.assertEquals("Test may need to be re-calculated for different max detail level.", 9, tree.treeMaxDetailLevel);
		
		
		DhSectionPos rootPos = new DhSectionPos((byte)9, 0, -1);
		testSet(tree, rootPos, 1);
		
		// pos is in tree, but out of range
		DhSectionPos midPos = new DhSectionPos((byte)8, 0, -1);
		testSet(tree, midPos, 2, IndexOutOfBoundsException.class);
		
		// pos is in tree, but out of range
		DhSectionPos leafPos = new DhSectionPos((byte)7, 0, -2);
		testSet(tree, leafPos, 3, IndexOutOfBoundsException.class);
		
	}
	
	
	@Test
	public void QuadTreeRootAlignedMovingTest()
	{
		AbstractTestTreeParams treeParams = new LargeTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), treeParams.getPositiveEdgeCenterPos(), LodUtil.BLOCK_DETAIL_LEVEL);
		
		int pseudoRootNodeWidthInBlocks = BitShiftUtil.powerOfTwo(10);
		
		
		// (pseudo) root nodes //
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
		DhBlockPos2D smallMoveBlockPos = new DhBlockPos2D(pseudoRootNodeWidthInBlocks*2, 0); // move enough that the original root nodes aren't touching the same grid squares they were before, but not far enough as to be garbage collected (TODO reword)
		tree.setCenterBlockPos(smallMoveBlockPos);
		Assert.assertEquals("Tree center incorrect", smallMoveBlockPos, tree.getCenterBlockPos());
		
		// nodes should be found at the same locations
		testGet(tree, nw, 2);
		testGet(tree, ne, 3);
		testGet(tree, sw, 4);
		testGet(tree, se, 5);
		Assert.assertEquals("incorrect leaf node count", tree.leafNodeCount(), 4);
		
		
		
		// very large move //
		DhBlockPos2D bigMoveBlockPos = new DhBlockPos2D(treeParams.getWidthInBlocks() * 2, 0);
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
		DhSectionPos edgePos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, -treeParams.getWidthInBlocks()/2, 0);
		testSet(tree, edgePos, 1);
		Assert.assertEquals("incorrect leaf node count", 1, tree.leafNodeCount());
		
		// +1 root node from the negative X edge
		DhSectionPos adjacentEdgePos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, (-treeParams.getWidthInBlocks()/2)+pseudoRootNodeWidthInBlocks, 0);
		testSet(tree, adjacentEdgePos, 2);
		Assert.assertEquals("incorrect leaf node count", 2, tree.leafNodeCount());
		
		// move so only the root nodes exactly on the X edge remain
		DhBlockPos2D edgeMoveBlockPos = new DhBlockPos2D(pseudoRootNodeWidthInBlocks - (treeParams.getWidthInRootNodes()*pseudoRootNodeWidthInBlocks), 0);
		tree.setCenterBlockPos(edgeMoveBlockPos);
		Assert.assertEquals("Tree center incorrect", edgeMoveBlockPos, tree.getCenterBlockPos());
		Assert.assertEquals("incorrect leaf node count", 2, tree.leafNodeCount());
		
	}
	
	@Test
	public void QuadTreeIterationTest()
	{
		AbstractTestTreeParams treeParams = new LargeTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), treeParams.getPositiveEdgeCenterPos(), LodUtil.BLOCK_DETAIL_LEVEL);
		
		
		// (pseudo) root nodes //
		testSet(tree, new DhSectionPos((byte)10, 0, 0), 1);
		testSet(tree, new DhSectionPos((byte)10, 1, 0), 2);
		
		// first child (0,0) //
		testSet(tree, new DhSectionPos((byte)9, 0, 0), 3);
		testSet(tree, new DhSectionPos((byte)9, 1, 0), 4);
		testSet(tree, new DhSectionPos((byte)9, 0, 1), 5);
		testSet(tree, new DhSectionPos((byte)9, 1, 1), 6);
		
		
		
		// root nodes
		int rootNodeCount = 0;
		
		Iterator<DhSectionPos> rootNodePosIterator = tree.rootNodePosIterator();
		while (rootNodePosIterator.hasNext())
		{
			QuadNode<Integer> rootNode = tree.getNode(rootNodePosIterator.next());
			if (rootNode != null)
			{
				rootNodeCount++;	
			}
		}
		Assert.assertEquals("incorrect root count", 1, rootNodeCount);
		
		
		
		// leaf nodes
		int leafCount = 0;
		int leafValueSum = 0;
		
		Iterator<QuadNode<Integer>> leafNodeIterator = tree.leafNodeIterator();
		while (leafNodeIterator.hasNext())
		{
			QuadNode<Integer> leafNode = leafNodeIterator.next();
			
			leafCount++;
			leafValueSum += leafNode.value;
		}
		Assert.assertEquals("incorrect leaf count", 5, leafCount);
		Assert.assertEquals("incorrect leaf value sum", 20, leafValueSum);
		
	}
	
	@Test
	public void NewQuadTreeIterationTest()
	{
		AbstractTestTreeParams treeParams = new LargeTestTree();
		QuadNode<Integer> rootNode = new QuadNode<>(new DhSectionPos((byte)10, 0, 0), LodUtil.BLOCK_DETAIL_LEVEL);
		
		rootNode.setValue(new DhSectionPos((byte)10, 0, 0), 0);
		
		rootNode.setValue(new DhSectionPos((byte)9, 0, 0), 1);
		rootNode.setValue(new DhSectionPos((byte)9, 1, 0), 1);
		rootNode.setValue(new DhSectionPos((byte)9, 0, 1), 1);
		rootNode.setValue(new DhSectionPos((byte)9, 1, 1), null);
		
		rootNode.setValue(new DhSectionPos((byte)8, 0, 0), 2);
		rootNode.setValue(new DhSectionPos((byte)8, 1, 0), 2);
		rootNode.setValue(new DhSectionPos((byte)8, 0, 1), 2);
		rootNode.setValue(new DhSectionPos((byte)8, 1, 1), null);
		
		
		
		// all node iterator //
		
		Iterator<QuadNode<Integer>> iterator = rootNode.getNodeIterator();
		
		HashSet<QuadNode<Integer>> iteratedNodes = new HashSet<>();
		int populatedValueCount = 0;
		int totalNodeCount = 0;
		
		while (iterator.hasNext())
		{
			QuadNode<Integer> node = iterator.next();
			if (node.value != null)
			{
				populatedValueCount++;
			}
			
			if (!iteratedNodes.add(node))
			{
				Assert.fail("Iterator passed over the same node multiple times. Section Pos: "+node.sectionPos);
			}
			
			totalNodeCount++;
		}
		
		Assert.assertEquals("incorrect populated node count", 7, populatedValueCount);
		Assert.assertEquals("incorrect total node count", 9, totalNodeCount);
		
		
		
		// leaf node iterator //
		
		Iterator<QuadNode<Integer>> leafIterator = rootNode.getLeafNodeIterator();
		
		HashSet<QuadNode<Integer>> iteratedLeafNodes = new HashSet<>();
		int populatedLeafCount = 0;
		int totalLeafCount = 0;
		
		while (leafIterator.hasNext())
		{
			QuadNode<Integer> node = leafIterator.next();
			if (node.value != null)
			{
				populatedLeafCount++;
			}
			
			if (!iteratedLeafNodes.add(node))
			{
				Assert.fail("Iterator passed over the same node multiple times. Section Pos: "+node.sectionPos);
			}
			
			totalLeafCount++;
		}
		
		Assert.assertEquals("incorrect populated leaf count", 5, populatedLeafCount);
		Assert.assertEquals("incorrect total leaf count", 7, totalLeafCount);
		
	}
	
	@Test
	public void CenteredGridListIterationTest()
	{
		AbstractTestTreeParams treeParams = new TinyTestTree();
		final QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), treeParams.getPositiveEdgeCenterPos(), LodUtil.BLOCK_DETAIL_LEVEL);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 0, 0), 0);
		
		// confirm the root node were added
		int rootNodeCount = 0;
		Iterator<DhSectionPos> rootNodePosIterator = tree.rootNodePosIterator();
		while (rootNodePosIterator.hasNext())
		{
			QuadNode<Integer> rootNode = tree.getNode(rootNodePosIterator.next());
			if (rootNode != null)
			{
				rootNodeCount++;
			}
		}
		Assert.assertEquals("incorrect root count", 1, rootNodeCount);
		
		// attempt to get and remove, each node in the tree
		int rootNodePosCount = 0;
		rootNodePosIterator = tree.rootNodePosIterator();
		while (rootNodePosIterator.hasNext())
		{
			DhSectionPos rootNodePos = rootNodePosIterator.next();
			QuadNode<Integer> rootNode = tree.getNode(rootNodePos);
			if (rootNode != null)
			{
				testGet(tree, rootNodePos, 0);
				testSet(tree, rootNodePos, null);
				
				rootNodePosCount++;
			}
		}
		Assert.assertEquals("incorrect root count", 1, rootNodePosCount);
		
	}
	
	@Test
	public void OffsetGridListIterationTest()
	{
		AbstractTestTreeParams treeParams = new TinyTestTree();
		
		// exactly inside (5*0,0)
		testGridListRootCount(treeParams.getWidthInBlocks(), treeParams.getPositiveEdgeCenterPos(), 1);
		
		// offset across (5*-1,0) and (5*0,0)
		testGridListRootCount(treeParams.getWidthInBlocks(), new DhBlockPos2D(-treeParams.getWidthInBlocks() / 4, treeParams.getPositiveEdgeCenterPos().z), 2);
		
		// offset across the origin: (5*0,0), (5*-1,0), (5*0,-1), and (5*-1,-1)
		testGridListRootCount(treeParams.getWidthInBlocks(), DhBlockPos2D.ZERO, 4);
		
	}
	private static void testGridListRootCount(int treeWidth, DhBlockPos2D treeMovePos, int expectedRootNodeCount)
	{
		final QuadTree<Integer> tree = new QuadTree<>(treeWidth, DhBlockPos2D.ZERO, LodUtil.BLOCK_DETAIL_LEVEL);
		Assert.assertEquals("tree creation failed, incorrect initial position", DhBlockPos2D.ZERO, tree.getCenterBlockPos());
		
		tree.setCenterBlockPos(treeMovePos);
		Assert.assertEquals("tree move failed, incorrect position after move", treeMovePos, tree.getCenterBlockPos());
		
		Iterator<DhSectionPos> rootNodePosIterator = tree.rootNodePosIterator();
		while (rootNodePosIterator.hasNext())
		{
			testSet(tree, rootNodePosIterator.next(), 0);
		}
		
		
		// 4 root nodes should be added
		int rootNodeCount = 0;
		rootNodePosIterator = tree.rootNodePosIterator();
		while (rootNodePosIterator.hasNext())
		{
			QuadNode<Integer> rootNode = tree.getNode(rootNodePosIterator.next());
			if (rootNode != null)
			{
				rootNodeCount++;
			}
		}
		Assert.assertEquals("incorrect root count", expectedRootNodeCount, rootNodeCount);
	}
	
	@Test
	public void TinyGridAlignedTreeTest()
	{
		AbstractTestTreeParams treeParams = new MediumTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), treeParams.getPositiveEdgeCenterPos(), LodUtil.BLOCK_DETAIL_LEVEL);
		// minimum size tree should be 3 root nodes wide
		Assert.assertEquals("incorrect tree node width", 3, tree.ringListWidth());
		Assert.assertEquals("incorrect tree width", treeParams.getWidthInBlocks(), tree.diameterInBlocks());
		
		
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 0, 0), 0);
		
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, -1, -1), -1, IndexOutOfBoundsException.class);
		testSet(tree, new DhSectionPos(tree.treeMaxDetailLevel, 1, 1), -1, IndexOutOfBoundsException.class);
		
		int rootNodeCount = 0;
		Iterator<DhSectionPos> rootNodeIterator = tree.rootNodePosIterator();
		while (rootNodeIterator.hasNext())
		{
			QuadNode<Integer> rootNode = tree.getNode(rootNodeIterator.next());
			if (rootNode != null)
			{
				rootNodeCount++;
			}
		}
		Assert.assertEquals("incorrect leaf value sum", 1, rootNodeCount);
		
	}
	
	@Test
	public void TinyGridOffsetTreeTest()
	{
		AbstractTestTreeParams treeParams = new MediumTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), new DhBlockPos2D(0, 0), LodUtil.BLOCK_DETAIL_LEVEL);
		// minimum size tree should be 3 root nodes wide
		Assert.assertEquals("incorrect tree node width", 3, tree.ringListWidth());
		Assert.assertEquals("incorrect tree width", treeParams.getWidthInBlocks(), tree.diameterInBlocks());
		
		
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
		
		
		int rootNodeCount = 0;
		Iterator<DhSectionPos> rootNodeIterator = tree.rootNodePosIterator();
		while (rootNodeIterator.hasNext())
		{
			QuadNode<Integer> rootNode = tree.getNode(rootNodeIterator.next());
			if (rootNode != null)
			{
				rootNodeCount++;
			}
		}
		Assert.assertEquals("incorrect leaf value sum", 4, rootNodeCount);
		
	}
	
	@Test
	public void TreeDetailLevelLimitTest()
	{
		AbstractTestTreeParams treeParams = new MediumTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), new DhBlockPos2D(0, 0), (byte)8);
		Assert.assertEquals("Test detail level's need to be adjusted. This isn't necessarily a failed test.", 10, tree.treeMaxDetailLevel);
		
		// valid detail levels
		testSet(tree, new DhSectionPos((byte)10, 0, 0), 1);
		testSet(tree, new DhSectionPos((byte)9, 0, 0), 2);
		testSet(tree, new DhSectionPos((byte)8, 0, 0), 3);
		
		// detail level too low
		testSet(tree, new DhSectionPos((byte)7, 0, 0), -1, IndexOutOfBoundsException.class);
		testSet(tree, new DhSectionPos((byte)6, 0, 0), -1, IndexOutOfBoundsException.class);
		
		// detail level too high
		testSet(tree, new DhSectionPos((byte)11, 0, 0), -1, IndexOutOfBoundsException.class);
		testSet(tree, new DhSectionPos((byte)12, 0, 0), -1, IndexOutOfBoundsException.class);
		
	}
	
	@Test
	public void QuadNodeDetailLimitTest()
	{
		AbstractTestTreeParams treeParams = new MediumTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), new DhBlockPos2D(0, 0), (byte)6);
		Assert.assertEquals("Test detail level's need to be adjusted. This isn't necessarily a failed test.", 10, tree.treeMaxDetailLevel);

		// create the root node
		testSet(tree, new DhSectionPos((byte)10, 0, 0), 1);

		
		
		AtomicInteger minimumDetailLevelReachedRef = new AtomicInteger(tree.treeMaxDetailLevel);
		
		// recurse down the tree
		Iterator<DhSectionPos> rootNodePosIterator = tree.rootNodePosIterator();
		while (rootNodePosIterator.hasNext())
		{
			DhSectionPos sectionPos = rootNodePosIterator.next();
			QuadNode<Integer> rootNode = tree.getNode(sectionPos);
			if (rootNode != null)
			{
				// fill in the root node's direct children
				Iterator<DhSectionPos> childPosIterator = rootNode.getChildPosIterator();
				while (childPosIterator.hasNext())
				{
					DhSectionPos rootChildPos = childPosIterator.next();
					rootNode.setValue(rootChildPos, 0);
				}
				
				
				
				// recursively create child nodes down to the minimum detail level
				Iterator<QuadNode<Integer>> ChildIterator = rootNode.getChildNodeIterator();
				while (ChildIterator.hasNext())
				{
					QuadNode<Integer> childNode = ChildIterator.next();
					Assert.assertNotNull(childNode); // TODO is this correct?
					
					recursivelyCreateNodeChildren(childNode, tree.treeMinDetailLevel, minimumDetailLevelReachedRef);
				}
			}
		}
		
		// confirm that the tree can and did iterate all the way down to the minimum detail level
		Assert.assertEquals("Incorrect minimum detail level reached.", tree.treeMinDetailLevel, minimumDetailLevelReachedRef.get());
	}
	private void recursivelyCreateNodeChildren(QuadNode<Integer> node, byte minDetailLevel, AtomicInteger minimumDetailLevelReachedRef)
	{
		boolean childNodesCreated = false;
		boolean childNodesIterated = false;
		
		
		
		// fill in the null children
		Iterator<DhSectionPos> directChildIterator = node.getChildPosIterator();
		while (directChildIterator.hasNext())
		{
			node.setValue(directChildIterator.next(), 0);
			childNodesCreated = true;
		}
		
		
		// attempt to recurse down these new children
		directChildIterator = node.getChildPosIterator();
		while (directChildIterator.hasNext())
		{
			DhSectionPos sectionPos = directChildIterator.next();
			QuadNode<Integer> childNode = node.getNode(sectionPos);
			
			Assert.assertTrue("Child node recurred too low. Min detail level: "+minDetailLevel+", node detail level: "+childNode.sectionPos.sectionDetailLevel, childNode.sectionPos.sectionDetailLevel >= minDetailLevel);
			recursivelyCreateNodeChildren(childNode, minDetailLevel, minimumDetailLevelReachedRef);
			
			childNodesIterated = true;
		}
		
		
		// keep track of how far down the tree we have gone
		if (node.sectionPos.sectionDetailLevel < minimumDetailLevelReachedRef.get())
		{
			minimumDetailLevelReachedRef.set(node.sectionPos.sectionDetailLevel);
		}
		
		
		
		// assertions
		if (childNodesCreated)
		{
			Assert.assertTrue("node children created below minimum detail level", node.sectionPos.sectionDetailLevel >= minDetailLevel);
		}
		if (childNodesIterated)
		{
			Assert.assertTrue("node children iterated below minimum detail level", node.sectionPos.sectionDetailLevel-1 >= minDetailLevel);
		}
	}
	
	@Test
	public void quadNodeChildPositionIndexTest()
	{
		QuadNode<Integer> rootNode = new QuadNode<>(new DhSectionPos((byte)10, 0, 0), (byte)0);
		Iterator<DhSectionPos> directChildPosIterator = rootNode.getChildPosIterator();
		while (directChildPosIterator.hasNext())
		{
			DhSectionPos sectionPos = directChildPosIterator.next();
			Assert.assertNotEquals("Root node pos shouldn't be included in direct child pos iteration", sectionPos, rootNode.sectionPos);
			
			rootNode.setValue(sectionPos, 1);
		}
		Assert.assertEquals("node not filled", 4, rootNode.getChildValueCount());
		
		
		for (int i = 0; i < 4; i++)
		{
			DhSectionPos childPos = rootNode.sectionPos.getChildByIndex(i);
			QuadNode<Integer> childNode = rootNode.getChildByIndex(i);
			Assert.assertEquals("child position not the same as "+DhSectionPos.class.getSimpleName()+"'s getChildByIndex()", childPos, childNode.sectionPos);
		}
		
	}
	
	@Test
	public void quadNodeChildPositionOutOfBoundsTest()
	{
		int treeWidthInBlocks = 64;
		QuadTree<Integer> tree = new QuadTree<>(treeWidthInBlocks, new DhBlockPos2D(-2, 0), (byte)0);
		
		
		
		// center root node
		DhSectionPos centerNodePos = new DhSectionPos((byte)1, 0, 0);
		
		// create node
		tree.setValue(centerNodePos, 0);
		QuadNode<Integer> centerRootNode = tree.getNode(centerNodePos);
		Assert.assertNotNull(centerRootNode);
		
		// child pos in bounds of the tree
		Iterator<DhSectionPos> childPosIterator = centerRootNode.getChildPosIterator();
		while (childPosIterator.hasNext())
		{
			DhSectionPos childPos = childPosIterator.next();
			centerRootNode.setValue(childPos, 1);
		}
		Assert.assertEquals("center node not filled", 4, centerRootNode.getChildValueCount());
		
		
		
		// edge root node
		DhSectionPos offsetNodePos = new DhSectionPos((byte)1, -17, -16);
		
		// create node
		tree.setValue(offsetNodePos, 0);
		QuadNode<Integer> offsetRootNode = tree.getNode(offsetNodePos);
		Assert.assertNotNull(offsetRootNode);
		
		// child pos in bounds of the tree
		childPosIterator = offsetRootNode.getChildPosIterator();
		while (childPosIterator.hasNext())
		{
			DhSectionPos childPos = childPosIterator.next();
			offsetRootNode.setValue(childPos, 1);
		}
		// TODO James thought this shouldn't work for all 4 nodes, but he must've thought wrong.
		Assert.assertEquals("offset should only contain some children.", 4, offsetRootNode.getChildValueCount());
		
	}
	
	// this is here for quickly testing the toString method, it should never fail
	@Test
	public void toStringTest()
	{
		AbstractTestTreeParams treeParams = new MediumTestTree();
		QuadTree<Integer> tree = new QuadTree<>(treeParams.getWidthInBlocks(), new DhBlockPos2D(0, 0), (byte)6);
		
		String treeString = tree.toString();
		Assert.assertNotNull(treeString);
		Assert.assertNotEquals("", treeString);
		
	}
	
	// null node auto-deletion not yet implemented
	//@Test
	public void autoDeleteNullQuadNodeChildTest()
	{
		QuadNode<Integer> rootNode = new QuadNode<>(new DhSectionPos((byte)10, 0, 0), LodUtil.BLOCK_DETAIL_LEVEL);
		
		
		rootNode.setValue(new DhSectionPos((byte)10, 0, 0), 0);
		
		DhSectionPos midNodePos = new DhSectionPos((byte)9, 0, 0);
		//rootNode.setValue(midNodePos, null); // holds detail 8
		rootNode.setValue(new DhSectionPos((byte)9, 1, 0), 1);
		rootNode.setValue(new DhSectionPos((byte)9, 0, 1), 1);
		rootNode.setValue(new DhSectionPos((byte)9, 1, 1), 1);
		
		rootNode.setValue(new DhSectionPos((byte)8, 0, 0), 2);
		rootNode.setValue(new DhSectionPos((byte)8, 1, 0), 2);
		rootNode.setValue(new DhSectionPos((byte)8, 0, 1), 2);
		rootNode.setValue(new DhSectionPos((byte)8, 1, 1), 2);
		
		
		
		// validate nodes were added
		Assert.assertEquals(4, rootNode.getChildValueCount());
		Assert.assertEquals(4, rootNode.getNode(midNodePos).getChildValueCount());
		
		
		
		// test removing nodes //
		
		// remove two leaf nodes from the root
		DhSectionPos leafPos = new DhSectionPos((byte)9, 1, 1);
		rootNode.setValue(leafPos, null);
		Assert.assertEquals(3, rootNode.getChildValueCount());
		Assert.assertNull("Node wasn't deleted", rootNode.getNode(leafPos));
		
		leafPos = new DhSectionPos((byte)9, 0, 1);
		rootNode.setValue(leafPos, null);
		Assert.assertEquals(2, rootNode.getChildValueCount());
		Assert.assertNull("Node wasn't deleted", rootNode.getNode(leafPos));
		
		
		// remove 
		
		// remove all child nodes
		Assert.assertEquals(4, rootNode.getNode(midNodePos).getChildValueCount());
		
		// remove all but one, mid-node should still be present
		rootNode.setValue(new DhSectionPos((byte)8, 0, 0), null);
		rootNode.setValue(new DhSectionPos((byte)8, 0, 1), null);
		rootNode.setValue(new DhSectionPos((byte)8, 1, 0), null);
		Assert.assertEquals(1, rootNode.getNode(midNodePos).getChildValueCount());
		
		// remove last mid-node child, mid-node should now be removed
		rootNode.setValue(new DhSectionPos((byte)8, 1, 1), null);
		Assert.assertNull("Mid node not deleted.", rootNode.getNode(midNodePos));
		Assert.assertEquals(3, rootNode.getChildValueCount());
		
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
			Integer previousValue = tree.setValue(pos, setValue);
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
			Integer getResult = tree.getValue(pos);
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
	
	
	
	//================//
	// helper classes //
	//================//
	
	private abstract static class AbstractTestTreeParams
	{
		public abstract int getWidthInBlocks();
		
		/** the tree should be slightly larger than the width in blocks to account for offset centers */
		public int getWidthInRootNodes() { return MathUtil.log2(this.getWidthInBlocks()) + 2; }
		public byte getMaxDetailLevel() { return (byte) MathUtil.log2(this.getWidthInBlocks()); }
		/** @return the block pos so that the tree's negative corner lines up with (0,0) */
		public DhBlockPos2D getPositiveEdgeCenterPos() { return new DhBlockPos2D(BitShiftUtil.powerOfTwo(this.getMaxDetailLevel())/2, BitShiftUtil.powerOfTwo(this.getMaxDetailLevel())/2); }
	}
	
	private static class LargeTestTree extends AbstractTestTreeParams
	{
		public int getWidthInBlocks() { return 8192; }
	}
	
	private static class MediumTestTree extends AbstractTestTreeParams
	{
		public int getWidthInBlocks() { return 1024; }
	}
	
	private static class TinyTestTree extends AbstractTestTreeParams
	{
		// top detail level = 6
		public int getWidthInBlocks() { return 32; }
	}
	
	
}
