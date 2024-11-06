/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.utils.aspects;

import be.nabu.utils.aspects.api.NotImplemented;
import junit.framework.TestCase;

public class TestAspects extends TestCase {
	
	public void testMerge() {
		Read read = new Read() {
			public String read() {
				return "The content";
			}
		};
		Write write = new Write() {
			public int write(String stringToWrite) {
				return stringToWrite.length();
			}
		};
		Both both = AspectUtils.cast(
			AspectUtils.join(read, write),
			Both.class
		);
		assertEquals("The content", both.read());
		assertEquals(4, both.write("test"));
	}
	
	public void testOverrideAndRemove() {
		Read read = new Read() {
			public String read() {
				return "The content";
			}
		};
		Write write = new Write() {
			public int write(String stringToWrite) {
				return stringToWrite.length();
			}
		};
		Write otherWrite = new Write() {
			public int write(String stringToWrite) {
				return stringToWrite.length() * 2;
			}
		};
		// create new by joining and casting
		Both both = AspectUtils.cast(
			AspectUtils.join(read, write, otherWrite),
			Both.class
		);
		assertEquals("The content", both.read());
		assertEquals(8, both.write("test"));
		
		// remove the alternative implementation by class
		AspectUtils.remove(both, otherWrite.getClass());
		assertEquals(4, both.write("test"));
		
		// add it again
		AspectUtils.add(both, otherWrite);
		assertEquals(8, both.write("test"));
		
		// remove by instance
		AspectUtils.remove(both, otherWrite);
		assertEquals(4, both.write("test"));
	}
	
	public void testNotImplemented() {
		Read read = new Read() {
			public String read() {
				return "The content";
			}
		};
		Write write = new Write() {
			public int write(String stringToWrite) {
				return stringToWrite.length();
			}
		};
		Write otherWrite = new Write() {
			@NotImplemented
			public int write(String stringToWrite) {
				return stringToWrite.length() * 2;
			}
		};
		// create new by joining and casting
		Both both = AspectUtils.cast(
			AspectUtils.join(read, write, otherWrite),
			Both.class
		);
		assertEquals(4, both.write("test"));
	}
}
