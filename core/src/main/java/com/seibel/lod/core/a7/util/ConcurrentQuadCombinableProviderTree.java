package com.seibel.lod.core.a7.util;

import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.Atomics;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class ConcurrentQuadCombinableProviderTree<R extends CombinableResult<R>> {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static class Node<R> {
        private final DhLodPos pos;
        public final AtomicReference<CompletableFuture<R>> future;
        // The child node is stored as a weak reference so that it can be garbage collected when that node's future is completed
        //    and which then releases the hold on that node, thus allowing automatic garbage collection.
        public final AtomicReferenceArray<WeakReference<Node<R>>> children = new AtomicReferenceArray<>(4);
        @SuppressWarnings("unused")
        AtomicReference<Node<R>> parent = null; // This is only used to ensure that the parent is not garbage collected before the child.
        private Node(DhLodPos pos, CompletableFuture<R> future) {
            this.pos = pos;
            this.future = new AtomicReference<>(future);
        }
        private Node(DhLodPos pos, CompletableFuture<R> future, Node<R> parent) {
            this.pos = pos;
            this.future = new AtomicReference<>(future);
            this.parent = new AtomicReference<>(parent);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node<R> node = (Node<R>) o;
            return pos.equals(node.pos);
        }
        @Override
        public int hashCode() {
            return pos.hashCode();
        }
        public Node<R> setIfNullAndGet(int childIndex, Node<R> newChild) {
            WeakReference<Node<R>> newRef = new WeakReference<>(newChild);
            WeakReference<Node<R>> oldRef;
            do {
                oldRef = Atomics.compareAndExchange(children, childIndex, null, newRef);
                if (oldRef == null) return newChild; // CompareAndExchange succeeded
                Node<R> oldNode = oldRef.get();
                if (oldNode != null) return oldNode; // CompareAndExchange failed with old node not null
                // Otherwise, the old node weak reference is null.
            } while (!children.compareAndSet(childIndex, oldRef, newRef)); // If this cas fails, then try again. (Some other thread beat us to it.)
            return newChild; // If we get here, then we successfully replaced the old node weak reference with the new one.
        }
    }

    static class RootMap<R> {
        private final ConcurrentHashMap<DhLodPos, WeakReference<Node<R>>> roots = new ConcurrentHashMap<>();
        private final int topLevel;

        RootMap(int topLevel) {
            this.topLevel = topLevel;
        }

        public int getTopLevel() {
            return topLevel;
        }
        public Node<R> get(DhLodPos pos) {
            WeakReference<Node<R>> ref = roots.get(pos);
            return ref == null ? null : ref.get();
        }
        public Node<R> compareNullAndExchange(DhLodPos pos, Node<R> newRoot) {
            WeakReference<Node<R>> newRef = new WeakReference<>(newRoot);
            WeakReference<Node<R>> oldRef;
            do {
                oldRef = roots.putIfAbsent(pos, newRef);
                if (oldRef == null) return null; // putIfAbsent succeeded
                Node<R> oldRoot = oldRef.get();
                if (oldRoot != null) return oldRoot; // putIfAbsent failed with old root not null
                // Otherwise, the old root weak reference is null.
            } while (!roots.replace(pos, oldRef, newRef)); // If this cas fails, then try again. (Some other thread beat us to it.)
            return null; // If we get here, then we successfully replaced the old root weak reference with the new one, so return null.
        }
        public boolean compareNullAndSet(DhLodPos pos, Node<R> newRoot) {
            WeakReference<Node<R>> newRef = new WeakReference<>(newRoot);
            WeakReference<Node<R>> oldRef;
            do {
                oldRef = roots.putIfAbsent(pos, newRef);
                if (oldRef == null) return true; // putIfAbsent succeeded
                Node<R> oldRoot = oldRef.get();
                if (oldRoot != null) return false; // putIfAbsent failed with old root not null
                // Otherwise, the old root weak reference is null.
            } while (!roots.replace(pos, oldRef, newRef)); // If this cas fails, then try again. (Some other thread beat us to it.)
            return true; // If we get here, then we successfully replaced the old root weak reference with the new one.
        }
        public Node<R> setIfNullAndGet(DhLodPos pos, Node<R> newRoot) {
            WeakReference<Node<R>> newRef = new WeakReference<>(newRoot);
            WeakReference<Node<R>> oldRef;
            do {
                oldRef = roots.putIfAbsent(pos, newRef);
                if (oldRef == null) return newRoot; // putIfAbsent succeeded
                Node<R> oldRoot = oldRef.get();
                if (oldRoot != null) return oldRoot; // putIfAbsent failed with old root not null
                // Otherwise, the old root weak reference is null.
            } while (!roots.replace(pos, oldRef, newRef)); // If this cas fails, then try again. (Some other thread beat us to it.)
            return newRoot; // If we get here, then we successfully replaced the old root weak reference with the new one.
        }
        public void clean() {
            roots.forEach((k,v) -> {
                if (v.get() == null) // Remove the entry if the root is null
                    roots.remove(k, v); // But only if what we check is what we will be removing. (A CAS operation)
                // Otherwise, continue.
                // (It is not important that we must remove the entry if the root is null,
                // as this is just a cleanup op to shrink the map.)
            });
        }
    }

    private final ReentrantReadWriteLock rootMapGlobalLock = new ReentrantReadWriteLock();
    private final AtomicReference<RootMap<R>> rootMap = new AtomicReference<>(new RootMap<>(0));


    public ConcurrentQuadCombinableProviderTree() {}
    @Override
    public String toString() {
        return "CQCPT@" + rootMap.get().topLevel + "(~" + rootMap.get().roots.size() + ")";
    }

    // Atomically update and get the generation future
    private CompletableFuture<R> checkAndMakeFuture(Node<R> node, Function<DhLodPos, CompletableFuture<R>> allNullCompleter) {
        CompletableFuture<R> future = new CompletableFuture<>();
        CompletableFuture<R> casValue = Atomics.compareAndExchange(node.future, null, future);
        if (casValue != null) { // cas failed. Existing future. Return it.
            return casValue;
        }

        // Next, we need to make the future completable.
        // We first check for each child connection if it exists. If it does, we store it for a later 'allOf'.
        boolean allNull = true;
        @SuppressWarnings("unchecked")
        CompletableFuture<R>[] childFutures = new CompletableFuture[4];
        for (int i = 0; i < 4; i++) {
            WeakReference<Node<R>> childRef = node.children.get(i);
            Node<R> nextChild = childRef == null ? null : childRef.get();
            if (nextChild != null) { // child node exists. Recursively make or get the child's future.
                allNull = false;
                childFutures[i] = checkAndMakeFuture(nextChild, allNullCompleter);
            }
        }
        if (allNull) { // all children are null. We can then just run the allNullCompleter in this node.
            allNullCompleter.apply(node.pos).whenComplete((r, e) -> {
                // NOTE(*1): This *HAVE* to get the future via the node reference instead of directly capturing the future,
                //  as otherwise the node will be garbage collected before the future is completed.
                // With this, we can guarantee that the node is garbage collected only when the future is (being) completed.
                // (The actual order is not important however as long as the node is still alive when the generation is in progress)
                CompletableFuture<R> f = node.future.get();
                LodUtil.assertTrue(f != null, "Future should not be null");
                if (e != null) {
                    f.completeExceptionally(e);
                } else {
                    f.complete(r);
                }
            });
        } else { // some children exist. We need to wait for some or all of them to complete.
            // But before that, we need to create the children node where they are missing.
            for (int i = 0; i < 4; i++) {
                if (childFutures[i] == null) {
                    CompletableFuture<R> newChildFuture = new CompletableFuture<>();
                    Node<R> newChild = new Node<>(node.pos.getChild(i), newChildFuture, node);
                    node.children.set(i, new WeakReference<>(newChild));
                    childFutures[i] = newChildFuture;
                    // Since the child is new, we can be sure that it doesn't have any children.
                    // So, we need to make the new child's future completable by running the allNullCompleter.
                    //  (The above relies on the fact that we did a CAS on the beginning of this method,
                    //  which means that we have unique access to the node and its links to the children, and that
                    //  no other thread can be concurrently modifying its links)
                    allNullCompleter.apply(newChild.pos).whenComplete((r, e) -> {
                        // NOTE: Same as 'NOTE(*1)', we *HAVE* to get the future via the node reference instead of directly capturing the future.
                        CompletableFuture<R> f = newChild.future.get();
                        LodUtil.assertTrue(f != null, "Future should not be null");
                        if (e != null) {
                            f.completeExceptionally(e);
                        } else {
                            f.complete(r);
                        }
                    });
                }
                LodUtil.assertTrue(childFutures[i] != null);
            }
            // Now, we can wait for all the child futures to complete, and then complete this node's future with
            //     the combined result of all child futures.
            CompletableFuture.allOf(childFutures).handle((v, e) -> {
                // NOTE: Same as 'NOTE(*1)', we *HAVE* to get the future via the node reference instead of directly capturing the future.
                CompletableFuture<R> f = node.future.get();
                LodUtil.assertTrue(f != null, "Future should not be null");
                if (e != null) {
                    f.completeExceptionally(e);
                } else {
                    try {
                        f.complete(childFutures[0].join().combineWith(
                                childFutures[1].join(), childFutures[2].join(), childFutures[3].join()));
                    } catch (Throwable e2) {
                        f.completeExceptionally(e2);
                    }
                }
                return null;
            });
        }
        return future;
    }

    public CompletableFuture<R> createOrUseExisting(DhLodPos pos, Function<DhLodPos, CompletableFuture<R>> completer) {
        LOGGER.info("Creating or using existing future for {}", pos);
        int cleanRng = ThreadLocalRandom.current().nextInt(0, 10);
        if (cleanRng == 0) cleanIfNeeded();
        // First, ensure that the root map is locked for reading. (The lock is for the structure of the map, not the values)
        rootMapGlobalLock.readLock().lock();
        RootMap<R> map = rootMap.get();
        // Next, do different thing depending on the top level of the map compared to the target position.
        if (map.topLevel == pos.detail) { // The target position is at the top level, meaning that we can directly use the root.
            // Make the future and node first for the later CAS on null.
            CompletableFuture<R> future = new CompletableFuture<>();
            Node<R> newNode = new Node<R>(pos, future); // No parent node as it's the root.
            Node<R> cas = map.compareNullAndExchange(pos, newNode); // CAS the node into the map.
            rootMapGlobalLock.readLock().unlock(); // We're done with the map, as following code no longer accesses it.

            if (cas == null) { // cas succeeded. Which means no existing overlapping node in same detail level.
                // Reason: Since any lower level nodes should have upper level nodes as parent up to the top level,
                // and that there are no same level nodes, we can assume that the new node does not overlap any existing nodes.
                // Therefore, we can apply the completer function to the new node, and return the future.
                completer.apply(pos).whenComplete((r, e) -> {
                    // See NOTE(*1) above.
                    CompletableFuture<R> f = newNode.future.get();
                    LodUtil.assertTrue(f != null, "Future should not be null");
                    if (e != null) {
                        f.completeExceptionally(e);
                    } else {
                        f.complete(r);
                    }
                });
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
            Node<R> currentNode;
            DhLodPos childPos = pos.convertUpwardsTo((byte) map.topLevel);
            Node<R> childNode = map.setIfNullAndGet( // rule 3: if null, create a new node.
                    childPos, new Node<R>(childPos, null)); // No parent node as it's the root.
            rootMapGlobalLock.readLock().unlock(); // We're done with the map, as following code no longer accesses it.

            CompletableFuture<R> future = childNode.future.get();
            if (future != null) { // rule 1: if future is not null, halt and return the future.
                return future;
            } else { // rule 2: if future is null, continue traversing down the tree.
                currentNode = childNode;

                // Second and subsequent iterations:
                while (currentNode.pos.detail > pos.detail) {
                    childPos = pos.convertUpwardsTo((byte) (currentNode.pos.detail - 1));
                    // Note: It is important that child link is set and created before we check the child future,
                    //  so to avoid race conditions with checkAndMakeFuture.
                    childNode = currentNode.setIfNullAndGet(childPos.getChildIndexOfParent(),
                            new Node<R>(childPos, null, currentNode)); // rule 3: if null, create a new node.
                    CompletableFuture<R> childFuture = childNode.future.get();
                    if (childFuture != null) { // rule 1: if future is not null, halt and return the future.
                        return childFuture;
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

            rootMapGlobalLock.readLock().unlock();
            while (map.topLevel < pos.detail) {
                map = rebaseUpward(pos.detail);
            }
            LodUtil.assertTrue(map.topLevel >= pos.detail);
            return createOrUseExisting(pos, completer); // After rebasing, we can just call the createOrUseExisting method again.
        }
    }

    private RootMap<R> rebaseUpward(int targetLevel) {
        rootMapGlobalLock.writeLock().lock();
        try {
            RootMap<R> map = rootMap.get();
            if (map.topLevel >= targetLevel) {
                return map;
            }
            // At this point, we have exclusive access to the rootMap.
            map.clean(); // Clean the map. (Could actually be done with just readLock.)
            RootMap<R> newMap = new RootMap<>(map.topLevel + 1);
            map.roots.forEach((pos, nodeRef) -> {
                Node<R> node = nodeRef.get();
                if (node == null) return; // If null, ignore that node.
                LodUtil.assertTrue(pos.detail+1 == newMap.topLevel);
                LodUtil.assertTrue(node.parent.get() == null);
                LodUtil.assertTrue(node.pos.equals(pos));
                DhLodPos newPos = pos.convertUpwardsTo((byte) (pos.detail+1));

                // Create the parent node, or if it already exists, use it to set the child node's parent.
                // NOTE: While this section is protected by the rootMapGlobalLock, we still need to use the normal
                //  CAS methods to setAndGet the parent node, as the parent node may be GC'd concurrently by other threads
                //  who have just completed the node's future, and caused the GC parent chain up to the new map.
                Node<R> newParentNode = newMap.setIfNullAndGet(newPos, new Node<R>(newPos, null));
                node.parent.set(newParentNode);
            });
            boolean casWorked = rootMap.compareAndSet(map, newMap);
            LodUtil.assertTrue(casWorked);
            return newMap;
        } finally {
            rootMapGlobalLock.writeLock().unlock();
        }
    }

    public void cleanIfNeeded() {
        if (rootMapGlobalLock.readLock().tryLock()) {
            rootMap.get().clean();
            rootMapGlobalLock.readLock().unlock();
        }
    }
}
