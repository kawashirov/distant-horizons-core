package com.seibel.lod.core.a7.util;

import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.util.Atomics;
import com.seibel.lod.core.util.LodUtil;
import com.sun.jna.platform.unix.X11;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

interface CombinableResult<T> {
    T combineWith(T b, T c, T d);
}

public class ConcurrentGenerationPosTree<R extends CombinableResult<R>> {
    public class Node {
        private final DhLodPos pos;
        public final AtomicReference<CompletableFuture<R>> future;
        public final AtomicReferenceArray<Node> children = new AtomicReferenceArray<>(4);
        private Node(DhLodPos pos, CompletableFuture<R> future) {
            this.pos = pos;
            this.future = new AtomicReference<>(future);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return pos.equals(node.pos);
        }
        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }

    class RootMap {
        private final ConcurrentSkipListMap<DhLodPos, Node> roots = new ConcurrentSkipListMap<>();
        private final int topLevel;
        RootMap(int topLevel) {
            this.topLevel = topLevel;
        }
        Node get(DhLodPos pos) {
            return roots.get(pos);
        }
        Node swap(DhLodPos pos, Node newRoot) {
            return roots.put(pos, newRoot);
        }
        Node compareNullAndExchange(DhLodPos pos, Node newRoot) {
            return roots.putIfAbsent(pos, newRoot);
        }
        boolean compareNullAndSet(DhLodPos pos, Node newRoot) {
            Node oldRoot = roots.putIfAbsent(pos, newRoot);
            return oldRoot == null;
        }
        Node setIfNullAndGet(DhLodPos pos, Node newRoot) {
            Node oldRoot = roots.putIfAbsent(pos, newRoot);
            return oldRoot == null ? newRoot : oldRoot;
        }
        Node swapNull(DhLodPos pos) {
            return roots.remove(pos);
        }

    }
    private final AtomicReference<RootMap> rootMap = new AtomicReference<>(new RootMap(0));


    public ConcurrentGenerationPosTree() {}
    @Override
    public int hashCode() {
        throw new NotImplementedException();
    }
    @Override
    public boolean equals(Object obj) {
        throw new NotImplementedException();
    }
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new NotImplementedException();
    }
    @Override
    public String toString() {
        throw new NotImplementedException();
    }

    // Atomically update and get the generation future
    private CompletableFuture<R> checkAndMakeFuture(Node node, BiConsumer<DhLodPos, CompletableFuture<R>> allNullCompleter) {
        CompletableFuture<R> future = new CompletableFuture<>();
        CompletableFuture<R> casValue = Atomics.compareAndExchange(node.future, null, future);
        if (casValue != null) { // cas failed. Existing future. Return it.
            return future;
        }
        // Next, we need to make the future completable.
        // We first check for each child connection if it exists. If it does, we store it for a later 'allOf'.
        boolean allNull = true;
        @SuppressWarnings("unchecked")
        CompletableFuture<R>[] childFutures = new CompletableFuture[4];
        for (int i = 0; i < 4; i++) {
            Node nextChild = node.children.get(i);
            if (nextChild != null) { // child node exists. Recursively make or get the child's future.
                allNull = false;
                childFutures[i] = checkAndMakeFuture(nextChild, allNullCompleter);
            }
        }
        if (allNull) { // all children are null. We can then just run the allNullCompleter in this node.
            allNullCompleter.accept(node.pos, future);
        } else { // some children exist. We need to wait for some or all of them to complete.
            // But before that, we need to create the children node where they are missing.
            for (int i = 0; i < 4; i++) {
                if (childFutures[i] == null) {
                    CompletableFuture<R> newChildFuture = new CompletableFuture<>();
                    node.children.set(i, new Node(node.pos.getChild(i), newChildFuture));
                    // Since the child is new, we can be sure that it doesn't have any children.
                    // So, we need to make the new child's future completable by running the allNullCompleter.
                    //  (The above relies on the fact that we did a CAS on the beginning of this method,
                    //  which means that we have unique access to the node and it's links to the children, and that
                    //  no other thread can be concurrently modifying its links)
                    allNullCompleter.accept(node.pos.getChild(i), newChildFuture);
                }
            }
            // Now, we can wait for all the child futures to complete, and then complete this node's future with
            //     the combined result of all child futures.
            CompletableFuture.allOf(childFutures).thenRun(() ->
                    future.complete(childFutures[0].join().combineWith(
                            childFutures[1].join(), childFutures[2].join(), childFutures[3].join())));
        }
        return future;
    }

    public CompletableFuture<R> createOrUseExisting(DhLodPos pos, BiConsumer<DhLodPos, CompletableFuture<R>> completer) {
        RootMap map = rootMap.get();
        if (map.topLevel == pos.detail) {
            CompletableFuture<R> future = new CompletableFuture<>();
            Node newNode = new Node(pos, future);
            Node cas = map.compareNullAndExchange(pos, newNode);
            if (cas == null) { // cas succeeded. No existing overlapping node in same detail level.
                // Since any lower level nodes should have upper level nodes as parent up to the top level,
                // and that there are no same level nodes, we can assume that the new node does not overlap any existing nodes.
                // Therefore, we can apply the completer function to the new node, and return the future.
                completer.accept(pos, future);
                return future;
            } else { // cas failed. Existing overlapping node.
                // Run the checkAndMakeFuture method on the existing node to update and get the generation future.
                return checkAndMakeFuture(cas, completer);
            }
        } else if (map.topLevel > pos.detail) {
            // We need to traverse down the tree with the following rules during the traversal:
            // 1. If the next node is not null and has a future, halt and return that future.
            // 2. If the next node is not null with no future, continue traversing down the tree.
            // 3. if the next node is null, create a new node and CompareExchange it into the current node, and run rule 1/2.
            //    Note that DO NOT assume that all subsequent nodes will fall into case 3, as someone else can concurrently
            //    use and modify the newly created node!

            // To start, just treat the rootMap as the... well, root, and it's content as the children node.
            // We can then traverse down the tree until we reach the target node or hit the 1st case and return prematurely.

            // First iteration:
            Node currentNode;
            Node childNode = map.setIfNullAndGet(pos.convertUpwardsTo((byte) map.topLevel), new Node(pos, null)); // rule 3: if null, create a new node.
            CompletableFuture<R> future = childNode.future.get();
            if (future != null) { // rule 1: if future is not null, halt and return the future.
                return future;
            } else { // rule 2: if future is null, continue traversing down the tree.
                currentNode = childNode;

                // Second and subsequent iterations:
                while (currentNode.pos.detail > pos.detail) {
                    Node newNode = new Node(pos.convertUpwardsTo((byte)(currentNode.pos.detail-1)), null);
                    childNode = Atomics.compareAndSetThenGet(currentNode.children,
                            newNode.pos.getChildIndexOfParent(), null, newNode); // rule 3: if null, create a new node.
                    if (childNode == null) { // rule 1: if future is not null, halt and return the future.
                        return newNode.future.get();
                    } else { // rule 2: if future is null, continue traversing down the tree.
                        currentNode = childNode;
                    }
                }
            }
            // At this point, we have reached the target node.
            LodUtil.assertTrue(currentNode.pos.equals(pos));
            // We can now run the checkAndMakeFuture method on the target node to update and get the generation future.
            return checkAndMakeFuture(currentNode, completer); // Technically, this will rerun the 1st rule. But code is cleaner this way.
        } else { // map.topLevel < pos.detail
            // Now, this is the complex case. We need to rebase the tree to the higher detail level.
            // For now, this implementation will do a lock based version. However, I will figure out a way to do this without a lock.

            // Before we do anything, we ...

            // TODO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            return null;

        }
    }
}
