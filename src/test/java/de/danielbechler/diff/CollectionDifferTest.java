/*
 * Copyright 2012 Daniel Bechler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.danielbechler.diff;

import de.danielbechler.diff.mock.*;
import de.danielbechler.diff.node.*;
import de.danielbechler.diff.path.*;
import de.danielbechler.diff.visitor.*;
import org.testng.annotations.*;

import java.util.*;

import static de.danielbechler.diff.node.NodeAssertions.assertThat;
import static de.danielbechler.diff.path.PropertyPath.*;
import static java.util.Arrays.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.*;

/** @author Daniel Bechler */
public class CollectionDifferTest
{
	private CollectionDiffer differ;

	@BeforeMethod
	public void setUp() throws Exception
	{
		differ = new CollectionDiffer();
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testConstructionWithoutDelegate()
	{
		new CollectionDiffer(null);
	}

	@Test
	public void testConstructionWithDelegate()
	{
		new CollectionDiffer(new DelegatingObjectDifferImpl());
	}

	@Test
	public void testCompareWithEmptyLists()
	{
		final List<String> working = Collections.emptyList();
		final List<String> base = Collections.emptyList();
		final CollectionNode node = differ.compare(working, base);
		assertThat(node.hasChanges(), is(false));
		assertThat(node.hasChildren(), is(false));
	}

	@Test
	public void testCompareWithAddedCollection() throws Exception
	{
		final List<Object> working = Collections.emptyList();
		final CollectionNode node = differ.compare(working, null);
		assertThat(node.getState(), is(Node.State.ADDED));
	}

	@Test
	public void testCompareWithRemovedCollection() throws Exception
	{
		final List<Object> working = null;
		final List<Object> base = Collections.emptyList();
		final CollectionNode node = differ.compare(working, base);
		assertThat(node.getState(), is(Node.State.REMOVED));
	}

	@Test
	public void testCompareWithAddedItem() throws Exception
	{
		final Collection<String> working = new LinkedList<String>(asList("foo"));
		final Collection<String> base = new LinkedList<String>();

		final CollectionNode node = differ.compare(working, base);

		assertThat(node.hasChanges(), is(true));

		final Node child = node.getChild(createBuilder()
				.withRoot()
				.withCollectionItem("foo")
				.build());
		assertThat(child.getState(), is(Node.State.ADDED));
	}

	@Test
	public void testCompareWithRemovedItem() throws Exception
	{
		final Collection<String> working = new LinkedList<String>();
		final Collection<String> base = new LinkedList<String>(asList("foo"));

		final CollectionNode node = differ.compare(working, base);

		assertThat(node.hasChanges(), is(true));

		final Node child = node.getChild(createBuilder()
				.withRoot()
				.withCollectionItem("foo")
				.build());
		assertThat(child.getState(), is(Node.State.REMOVED));
	}

	@Test
	public void testCompareWithChangedItem() throws Exception
	{
		final List<ObjectWithHashCodeAndEquals> working = asList(new ObjectWithHashCodeAndEquals("foo", "1"));
		final List<ObjectWithHashCodeAndEquals> base = asList(new ObjectWithHashCodeAndEquals("foo", "2"));

		final CollectionNode node = differ.compare(working, base);

		assertThat(node.hasChanges(), is(true));

		final PropertyPath propertyPath = createBuilder()
				.withRoot()
				.withCollectionItem(new ObjectWithHashCodeAndEquals("foo"))
				.build();
		final Node child = node.getChild(propertyPath);
		assertThat(child.getState(), is(Node.State.CHANGED));
	}

	@Test
	public void testCompareWithDifferentCollectionImplementationsSucceeds() throws Exception
	{
		final Collection<String> base = new LinkedHashSet<String>(asList("one", "two"));
		final Collection<String> working = new TreeSet<String>(asList("one", "three"));
		final CollectionNode node = differ.compare(base, working);
		assertThat(node.hasChanges(), is(true));
	}

	@Test(enabled = false, description = "Currently this is simply not possible because of the way, the CollectionItemAccessor works. Would be great, to support this.")
	public void testCompareWithListContainingObjectTwiceDetectsIfOneGetsRemoved() throws Exception
	{
		final List<ObjectWithHashCodeAndEquals> base = asList(new ObjectWithHashCodeAndEquals("foo"), new ObjectWithHashCodeAndEquals("foo"));
		final List<ObjectWithHashCodeAndEquals> working = asList(new ObjectWithHashCodeAndEquals("foo"));
		final CollectionNode node = differ.compare(working, base);
		node.visit(new NodeHierarchyVisitor());
		assertThat(node)
				.child(PropertyPath.createBuilder()
								   .withRoot()
								   .withCollectionItem(new ObjectWithHashCodeAndEquals("foo"))
								   .build())
				.hasState(Node.State.REMOVED);
	}

	@Test(enabled = false, description = "Currently this is simply not possible because of the way, the CollectionItemAccessor works. Would be great, to support this.")
	public void testCompareWithListContainingObjectOnceDetectsIfAnotherInstanceOfItGetsAdded() throws Exception
	{
		final List<ObjectWithHashCodeAndEquals> base = asList(new ObjectWithHashCodeAndEquals("foo"));
		final List<ObjectWithHashCodeAndEquals> working = asList(new ObjectWithHashCodeAndEquals("foo"), new ObjectWithHashCodeAndEquals("foo"));
		final CollectionNode node = differ.compare(working, base);
		node.visit(new NodeHierarchyVisitor());
		assertThat(node)
				.child(PropertyPath.createBuilder()
								   .withRoot()
								   .withCollectionItem(new ObjectWithHashCodeAndEquals("foo"))
								   .build())
				.hasState(Node.State.ADDED);
	}
}
