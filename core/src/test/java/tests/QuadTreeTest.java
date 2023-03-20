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

/**
 * This is just a quick demo to confirm the testing system is set up correctly.
 * 
 * @author James Seibel
 * @version 2022-9-5
 */
public class QuadTreeTest
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private static final int ROOT_NODE_WIDTH_IN_BLOCKS = BitShiftUtil.powerOfTwo(10);
	private static final int MIN_TREE_WIDTH_IN_BLOCKS = ROOT_NODE_WIDTH_IN_BLOCKS * 8;
	
	static
	{
		Configurator.setRootLevel(Level.ALL);
	}
	
	@Test
	public void SectionPosTest()
	{
		DhSectionPos root = new DhSectionPos((byte)10, 0, 0);
		DhSectionPos child = new DhSectionPos((byte)9, 1, 1);
		
		Assert.assertTrue("section pos contains fail", root.contains(child));
		Assert.assertFalse("section pos contains fail", child.contains(root));
		
		
		root = new DhSectionPos((byte)10, 1, 0);
		child = new DhSectionPos((byte)9, 1, 1);
		Assert.assertFalse("section pos contains fail", root.contains(child));
		child = new DhSectionPos((byte)9, 2, 2);
		Assert.assertTrue("section pos contains fail", root.contains(child));
		
	}
	
	@Test
	public void BasicPositiveQuadTreeTest()
	{
		QuadTree<Integer> tree = new QuadTree<>(MIN_TREE_WIDTH_IN_BLOCKS, new DhBlockPos2D(0, 0));
		
		
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
		QuadTree<Integer> tree = new QuadTree<>(MIN_TREE_WIDTH_IN_BLOCKS, new DhBlockPos2D(0, 0));
		
		
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
	public void QuadTreeMovingTest()
	{
		int treeWidthInRootNodes = 8;
		int treeWidthInBlocks = ROOT_NODE_WIDTH_IN_BLOCKS * treeWidthInRootNodes;
		QuadTree<Integer> tree = new QuadTree<>(treeWidthInBlocks, new DhBlockPos2D(0, 0));
		
		
		
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
		DhBlockPos2D smallMoveBlockPos = new DhBlockPos2D(ROOT_NODE_WIDTH_IN_BLOCKS *2, 0); // move enough that the original root nodes aren't touching the same grid squares they were before, but not far enough as to be garbage collected (TODO reword)
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
		Assert.assertThrows(IndexOutOfBoundsException.class, () -> testGet(tree, nw, null));
		Assert.assertThrows(IndexOutOfBoundsException.class, () -> testGet(tree, ne, null));
		Assert.assertThrows(IndexOutOfBoundsException.class, () -> testGet(tree, sw, null));
		Assert.assertThrows(IndexOutOfBoundsException.class, () -> testGet(tree, se, null));
		
		Assert.assertEquals("incorrect leaf node count", tree.leafNodeCount(), 0);
		
		
		
		// edge move //
		
		// move back to the origin for easy testing
		tree.setCenterBlockPos(DhBlockPos2D.ZERO);
		Assert.assertEquals("Tree center incorrect", DhBlockPos2D.ZERO, tree.getCenterBlockPos());
		
		// TODO move me
		DhSectionPos outOfBoundsPos = new DhSectionPos(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL, ROOT_NODE_WIDTH_IN_BLOCKS, 0); // wrong detail level on purpose, if the detail level was 0 (block) this should work
		Assert.assertThrows("incorrect exception thrown ", IndexOutOfBoundsException.class, () -> testSet(tree, outOfBoundsPos, 2));
		Assert.assertEquals("incorrect leaf node count", 0, tree.leafNodeCount());
		
		// 1 root node from the edge
		DhSectionPos edgePos = new DhSectionPos(LodUtil.BLOCK_DETAIL_LEVEL, -((treeWidthInBlocks/2)-ROOT_NODE_WIDTH_IN_BLOCKS), 0);
		testSet(tree, edgePos, 2);
		Assert.assertEquals("incorrect leaf node count", 1, tree.leafNodeCount());
		
		// edge move 
		DhBlockPos2D edgeMoveBlockPos = new DhBlockPos2D(ROOT_NODE_WIDTH_IN_BLOCKS, 0); // TODO I can only move this 1 root node away from the center for some reason
		tree.setCenterBlockPos(edgeMoveBlockPos);
		Assert.assertEquals("Tree center incorrect", edgeMoveBlockPos, tree.getCenterBlockPos());
		
		Assert.assertEquals("incorrect leaf node count", 1, tree.leafNodeCount());
		
		
	}
	
	
	@Test
	public void QuadTreeIterationTest()
	{
		QuadTree<Integer> tree = new QuadTree<>(MIN_TREE_WIDTH_IN_BLOCKS, new DhBlockPos2D(0, 0));
		
		
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
	
	
	
	private static void testSet(QuadTree<Integer> tree, DhSectionPos pos, Integer value)
	{
		// set
		Integer setResult = tree.set(pos, value);
		Assert.assertNull("set failed "+pos, setResult);
		// get
		Integer getResult = tree.get(pos);
		Assert.assertEquals("get failed "+pos, value, getResult);
	}
	
	private static void testGet(QuadTree<Integer> tree, DhSectionPos pos, Integer value)
	{
		// get
		Integer getResult = tree.get(pos);
		Assert.assertEquals("get failed "+pos, value, getResult);
	}
	
	
}
