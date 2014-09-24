/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.ingest.plugin.spi;

// This file was Taken out from openjdk-6-src-b16-24_apr_2009.tar.gz
// http://download.java.net/openjdk/jdk6/promoted/b16/openjdk-6-src-b16-24_apr_2009.tar.gz
// downloaded: 2009-05-07


/*
 * Copyright 2000 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

//package javax.imageio.spi;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * A set of <code>Object</code>s with pairwise orderings between them.
 * The <code>iterator</code> method provides the elements in
 * topologically sorted order.  Elements participating in a cycle
 * are not returned.
 *
 * Unlike the <code>SortedSet</code> and <code>SortedMap</code>
 * interfaces, which require their elements to implement the
 * <code>Comparable</code> interface, this class receives ordering
 * information via its <code>setOrdering</code> and
 * <code>unsetPreference</code> methods.  This difference is due to
 * the fact that the relevant ordering between elements is unlikely to
 * be inherent in the elements themselves; rather, it is set
 * dynamically according to application policy.  For example, in a
 * service provider registry situation, an application might allow the
 * user to set a preference order for service provider objects
 * supplied by a trusted vendor over those supplied by another.
 *
 */
class PartiallyOrderedSet extends AbstractSet<Object> {

    // The topological sort (roughly) follows the algorithm described in
    // Horowitz and Sahni, _Fundamentals of Data Structures_ (1976),
    // p. 315.

    // Maps Objects to DigraphNodes that contain them
    private Map<Object, DigraphNode> poNodes = new HashMap<>();

    // The set of Objects
    private Set<Object> nodes = poNodes.keySet();

    /**
     * Constructs a <code>PartiallyOrderedSet</code>.
     */
    public PartiallyOrderedSet() {}

    public int size() {
        return nodes.size();
    }

    public boolean contains(Object o) {
        return nodes.contains(o);
    }

    /**
     * Returns an iterator over the elements contained in this
     * collection, with an ordering that respects the orderings set
     * by the <code>setOrdering</code> method.
     */
    public Iterator<Object> iterator() {
        return new PartialOrderIterator(poNodes.values().iterator());
    }

    /**
     * Adds an <code>Object</code> to this
     * <code>PartiallyOrderedSet</code>.
     */
    public boolean add(Object o) {
        if (nodes.contains(o)) {
            return false;
        }

        DigraphNode node = new DigraphNode(o);
        poNodes.put(o, node);
        return true;
    }

    /**
     * Removes an <code>Object</code> from this
     * <code>PartiallyOrderedSet</code>.
     */
    public boolean remove(Object o) {
        DigraphNode node = poNodes.get(o);
        if (node == null) {
            return false;
        }

        poNodes.remove(o);
        node.dispose();
        return true;
    }

    public void clear() {
        poNodes.clear();
    }

    /**
     * Sets an ordering between two nodes.  When an iterator is
     * requested, the first node will appear earlier in the
     * sequence than the second node.  If a prior ordering existed
     * between the nodes in the opposite order, it is removed.
     *
     * @return <code>true</code> if no prior ordering existed
     * between the nodes, <code>false</code>otherwise.
     */
    public boolean setOrdering(Object first, Object second) {
        DigraphNode firstPONode =
            poNodes.get(first);
        DigraphNode secondPONode =
            poNodes.get(second);

        secondPONode.removeEdge(firstPONode);
        return firstPONode.addEdge(secondPONode);
    }

    /**
     * Removes any ordering between two nodes.
     *
     * @return true if a prior prefence existed between the nodes.
     */
    public boolean unsetOrdering(Object first, Object second) {
        DigraphNode firstPONode =
            poNodes.get(first);
        DigraphNode secondPONode =
            poNodes.get(second);

        return firstPONode.removeEdge(secondPONode) ||
            secondPONode.removeEdge(firstPONode);
    }

    /**
     * Returns <code>true</code> if an ordering exists between two
     * nodes.
     */
    public boolean hasOrdering(Object preferred, Object other) {
        DigraphNode preferredPONode =
            poNodes.get(preferred);
        DigraphNode otherPONode =
            poNodes.get(other);

        return preferredPONode.hasEdge(otherPONode);
    }
}

class PartialOrderIterator implements Iterator<Object> {

    LinkedList<DigraphNode> zeroList = new LinkedList<>();
    Map<DigraphNode, Integer> inDegrees = new HashMap<>(); // DigraphNode -> Integer

    public PartialOrderIterator(Iterator<DigraphNode> iter) {
        // Initialize scratch in-degree values, zero list
        while (iter.hasNext()) {
            DigraphNode node = iter.next();
            int inDegree = node.getInDegree();
            inDegrees.put(node, new Integer(inDegree));

            // Add nodes with zero in-degree to the zero list
            if (inDegree == 0) {
                zeroList.add(node);
            }
        }
    }

    public boolean hasNext() {
        return !zeroList.isEmpty();
    }

    public Object next() {
        DigraphNode first = zeroList.removeFirst();

        // For each out node of the output node, decrement its in-degree
        Iterator<DigraphNode> outNodes = first.getOutNodes();
        while (outNodes.hasNext()) {
            DigraphNode node = outNodes.next();
            int inDegree = ((Integer)inDegrees.get(node)).intValue() - 1;
            inDegrees.put(node, new Integer(inDegree));

            // If the in-degree has fallen to 0, place the node on the list
            if (inDegree == 0) {
                zeroList.add(node);
            }
        }

        return first.getData();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
