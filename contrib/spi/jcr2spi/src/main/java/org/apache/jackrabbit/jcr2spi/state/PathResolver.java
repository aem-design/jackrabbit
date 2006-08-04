/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.name.Path;

/**
 * <code>PathResolver</code> resolves a relative Path starting at a given
 * ItemState and returns the Item where the path points to.
 */
public class PathResolver {

    /**
     * The starting point to resolve the path.
     */
    private final ItemState start;

    /**
     * The path to resolve.
     */
    private final Path relPath;

    /**
     * Internal constructor.
     *
     * @param start   the starting point.
     * @param relPath the path to resolve.
     * @throws IllegalArgumentException if relPath is absolute or not normalized
     *                                  or starts with a parent ('..') path
     *                                  element.
     */
    private PathResolver(ItemState start, Path relPath) {
        if (relPath.isAbsolute()
                || !relPath.isNormalized()
                || relPath.getElement(0).denotesParent()) {
            throw new IllegalArgumentException("path must be relative and must " +
                    "not contain parent path elements");
        }
        this.start = start;
        this.relPath = relPath;
    }

    /**
     * Resolves the path starting at <code>start</code>.
     *
     * @param start   the starting point.
     * @param relPath the path to resolve.
     * @return the resolved item state.
     * @throws NoSuchItemStateException the the referenced item state does not
     *                                  exist.
     * @throws ItemStateException       if an error occurs while retrieving the
     *                                  item state.
     * @throws IllegalArgumentException if relPath is absolute or not normalized
     *                                  or starts with a parent ('..') path
     *                                  element.
     */
    public static ItemState resolve(ItemState start, Path relPath)
            throws NoSuchItemStateException, ItemStateException {
        return new PathResolver(start, relPath).resolve();
    }

    /**
     * Looks up the <code>ItemState</code> at <code>relPath</code> starting at
     * <code>start</code>.
     *
     * @param start   the starting point.
     * @param relPath the path to resolve.
     * @return the resolved item state or <code>null</code> if the item is not
     *         available.
     * @throws NoSuchItemStateException the the referenced item state does not
     *                                  exist.
     * @throws ItemStateException       if an error occurs while retrieving the
     *                                  item state.
     * @throws IllegalArgumentException if relPath is absolute or not normalized
     *                                  or starts with a parent ('..') path
     *                                  element.
     */
    public static ItemState lookup(ItemState start, Path relPath)
            throws NoSuchItemStateException, ItemStateException {
        return new PathResolver(start, relPath).lookup();
    }

    /**
     * Resolves the path.
     *
     * @return the resolved item state.
     * @throws NoSuchItemStateException the the item state does not exist.
     * @throws ItemStateException       if an error occurs while retrieving the
     *                                  item state.
     */
    private ItemState resolve()
            throws NoSuchItemStateException, ItemStateException {
        if (!start.isNode()) {
            throw new NoSuchItemStateException(relPath.toString());
        }
        NodeState state = (NodeState) start;
        for (int i = 0; i < relPath.getLength(); i++) {
            Path.PathElement elem = relPath.getElement(i);
            // first try to resolve node
            if (state.hasChildNodeEntry(elem.getName(), elem.getNormalizedIndex())) {
                state = state.getChildNodeEntry(elem.getName(),
                        elem.getNormalizedIndex()).getNodeState();
            } else if (elem.getIndex() == 0 // property must not have index
                    && state.hasPropertyName(elem.getName())
                    && i == relPath.getLength() - 1) { // property must be final path element
                return state.getPropertyState(elem.getName());
            } else {
                throw new NoSuchItemStateException(relPath.toString());
            }
        }
        return state;
    }

    /**
     * Resolves the path but does not the <code>ItemState</code> if it is not
     * yet loaded.
     *
     * @return the resolved item state or <code>null</code> if the item is not
     *         available.
     * @throws NoSuchItemStateException the the item state does not exist.
     * @throws ItemStateException       if an error occurs while retrieving the
     *                                  item state.
     */
    private ItemState lookup()
            throws NoSuchItemStateException, ItemStateException {
        if (!start.isNode()) {
            throw new NoSuchItemStateException(relPath.toString());
        }
        NodeState state = (NodeState) start;
        for (int i = 0; i < relPath.getLength(); i++) {
            Path.PathElement elem = relPath.getElement(i);
            // first try to resolve node
            if (state.hasChildNodeEntry(elem.getName(), elem.getNormalizedIndex())) {
                ChildNodeEntry cne = state.getChildNodeEntry(elem.getName(),
                        elem.getNormalizedIndex());
                if (cne.isAvailable()) {
                    state = cne.getNodeState();
                } else {
                    return null;
                }
            } else if (elem.getIndex() == 0 // property must not have index
                    && state.hasPropertyName(elem.getName())
                    && i == relPath.getLength() - 1) { // property must be final path element
                // TODO: check if available
                return state.getPropertyState(elem.getName());
            } else {
                throw new NoSuchItemStateException(relPath.toString());
            }
        }
        return state;
    }
}
