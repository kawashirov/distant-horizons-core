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

public class DhSectionPosTest
{
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
	
}
