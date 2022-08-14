package com.seibel.lod.core.a7.util;

import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.util.LodUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LazySectionPosTree<T> implements ConcurrentMap<DhLodPos, T> {
    class Node implements Entry<DhLodPos, T> {
        private Node parent;
        private int child0to3;
        private final DhLodPos pos;
        private final AtomicInteger sizeCounter = size;
        private T value = null;
        private Node child0 = null;
        private Node child1 = null;
        private Node child2 = null;
        private Node child3 = null;
        private Node(Node parent, int child0to3, DhLodPos pos) {
            this.parent = parent;
            this.child0to3 = child0to3;
            this.pos = pos;
        }
        private Node(Node parent, int child0to3, DhLodPos pos, T value) {
            this.parent = parent;
            this.child0to3 = child0to3;
            this.pos = pos;
            this.value = value;
        }
        @Override
        public DhLodPos getKey() {
            return pos;
        }
        @Override
        public T getValue() {
            return value;
        }
        @Override
        public T setValue(T value) {
            T old = this.value;
            this.value = value;
            if (old == null && value != null) {
                sizeCounter.incrementAndGet();
            } else if (old != null && value == null) {
                sizeCounter.decrementAndGet();
            }
            return old;
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
        private T setIfAbsent(T value) {
            T old = this.value;
            if (old == null) {
                this.value = value;
                sizeCounter.incrementAndGet();
            }
            return old;
        }
        private T computeIfAbsent(@NotNull Function<? super DhLodPos, ? extends T> mappingFunction) {
            if (value == null) {
                value = mappingFunction.apply(pos);
                sizeCounter.incrementAndGet();
            }
            return value;
        }
        private T computeIfPresent(@NotNull BiFunction<? super DhLodPos, ? super T, ? extends T> remappingFunction) {
            if (value != null) {
                T newValue = remappingFunction.apply(pos, value);
                if (newValue != null) {
                    value = newValue;
                } else {
                    sizeCounter.decrementAndGet();
                    value = null;
                }
            }
            return value;
        }


        private boolean noChildren() {
            return child0 == null && child1 == null && child2 == null && child3 == null;
        }

        private Node makeOrGetChild(int child0to3) {
            LodUtil.assertTrue(child0to3 >= 0 && child0to3 <= 3);
            switch (child0to3) {
                case 0:
                    return child0 == null ? child0 = new Node(this, 0, pos.getChild(0)) : child0;
                case 1:
                    return child1 == null ? child1 = new Node(this, 1, pos.getChild(1)) : child1;
                case 2:
                    return child2 == null ? child2 = new Node(this, 2, pos.getChild(2)) : child2;
                case 3:
                    return child3 == null ? child3 = new Node(this, 3, pos.getChild(3)) : child3;
            }
            LodUtil.assertNotReach();
            return new Node(null, 0, pos.getChild(0)); // unreachable. Just hack to make contract happy.
        }
        private Node getChild(int child0to3) {
            LodUtil.assertTrue(child0to3 >= 0 && child0to3 <= 3);
            switch (child0to3) {
                case 0:
                    return child0;
                case 1:
                    return child1;
                case 2:
                    return child2;
                case 3:
                    return child3;
            }
            LodUtil.assertNotReach();
            return null;
        }
        private void removeChild(int child0to3) {
            LodUtil.assertTrue(child0to3 >= 0 && child0to3 <= 3);
            switch (child0to3) {
                case 0:
                    child0 = null;
                    break;
                case 1:
                    child1 = null;
                    break;
                case 2:
                    child2 = null;
                    break;
                case 3:
                    child3 = null;
                    break;
            }
            LodUtil.assertNotReach();
        }
        private void setChild(int child0to3, Node child) {
            LodUtil.assertTrue(child0to3 >= 0 && child0to3 <= 3);
            child.parent = this;
            switch (child0to3) {
                case 0:
                    child0 = child;
                    child.child0to3 = 0;
                    break;
                case 1:
                    child1 = child;
                    child.child0to3 = 1;
                    break;
                case 2:
                    child2 = child;
                    child.child0to3 = 2;
                    break;
                case 3:
                    child3 = child;
                    child.child0to3 = 3;
                    break;
            }
            LodUtil.assertNotReach();
        }
    }
    private ConcurrentSkipListMap<DhLodPos, Node> nodes = new ConcurrentSkipListMap<>();
    private byte topLevel = 0;
    private AtomicInteger size = new AtomicInteger(0);
    public LazySectionPosTree() {}
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

    @Override
    public int size() {
        return size.get();
    }
    @Override
    public boolean isEmpty() {
        return size.get() == 0;
    }

    private Node travel(Node from, DhLodPos pos) {
        if (from == null) return null;
        LodUtil.assertTrue(pos != null);
        LodUtil.assertTrue(from.pos.detail > pos.detail);
        LodUtil.assertTrue(from.pos.overlaps(pos));
        byte iterDetail = from.pos.detail;
        while (iterDetail > pos.detail) {
            from = from.getChild(pos.convertUpwardsTo(--iterDetail).getChildIndexOfParent());
            if (from == null) return null;
        }
        LodUtil.assertTrue(from.pos.equals(pos));
        return from;
    }
    private Node initTravel(Node from, DhLodPos pos) {
        LodUtil.assertTrue(from != null);
        LodUtil.assertTrue(pos != null);
        LodUtil.assertTrue(from.pos.detail > pos.detail);
        LodUtil.assertTrue(from.pos.overlaps(pos));
        byte iterDetail = from.pos.detail;
        while (iterDetail > pos.detail)
            from = from.makeOrGetChild(pos.convertUpwardsTo(--iterDetail).getChildIndexOfParent());
        LodUtil.assertTrue(from.pos.equals(pos));
        return from;
    }

    private void upcastTreeBase() {

    }
    private byte upcastSingeTreeBase() {
        byte nextLevel = (byte) (topLevel + 1);
        ConcurrentSkipListMap<DhLodPos, Node> newBase = new ConcurrentSkipListMap<>();
        nodes.forEach((pos, node) ->
            newBase.compute(pos.convertUpwardsTo(nextLevel), (key, old) -> {
                if (old == null) {
                    old = new Node(null, 0, pos.convertUpwardsTo(nextLevel));
                }
                old.setChild(pos.getChildIndexOfParent(), node);
                return old;
            })
        );
        nodes = newBase; // todo: cas operation to here. (Will be block free but not wait free)
        topLevel = nextLevel; //todo: atomic???
        return nextLevel;
    }
    private void downcastTreeBase() {
        byte prevLevel = (byte) (topLevel - 1);
        ConcurrentSkipListMap<DhLodPos, Node> newBase = new ConcurrentSkipListMap<>();


    }



    @Override
    public boolean containsKey(Object key) {
        DhLodPos pos = (DhLodPos) key;
        if (pos.detail > topLevel) return false;
        if (pos.detail == topLevel) return nodes.containsKey(pos);
        Node node = travel(nodes.get(pos.convertUpwardsTo(topLevel)), pos);
        return node != null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Such operation is not supported in LazySectionPosTree");
    }

    @Override
    public T get(Object key) {
        DhLodPos pos = (DhLodPos) key;
        if (pos.detail > topLevel) return null;
        if (pos.detail == topLevel) return nodes.get(pos).value;
        Node node = travel(nodes.get(pos.convertUpwardsTo(topLevel)), pos);
        return node == null ? null : node.value;
    }

    @Override
    public T getOrDefault(Object key, T defaultValue) {
        T value = get(key);
        return value == null ? defaultValue : value;
    }

    @Nullable
    @Override
    public T put(DhLodPos key, T value) {
        if (key.detail == topLevel) {
            return nodes.computeIfAbsent(key, k -> new Node(null, 0, key, value)).setValue(value);
        }
        if (key.detail < topLevel) {
            Node node = initTravel(nodes.get(key.convertUpwardsTo(topLevel)), key);
            return node.setValue(value);
        }
        // key.detail > topLevel:
        // Rebase the tree
        //upcastTreeBase(key.detail);
        return nodes.computeIfAbsent(key, k -> new Node(null, 0, key, value)).setValue(value);
    }

    private void removeNode(Node node) {
        if (node.parent != null) {
            node.parent.removeChild(node.child0to3);
            if (node.parent.noChildren()) {
                removeNode(node.parent);
            }
        }
        else nodes.remove(node.pos);
    }

    @Override
    public T remove(Object key) {
        DhLodPos pos = (DhLodPos) key;
        if (pos.detail > topLevel) return null;
        Node node;
        if (pos.detail == topLevel) {
            node = nodes.remove(pos);
        } else {
            node = travel(nodes.get(pos.convertUpwardsTo(topLevel)), pos);
        }
        if (node == null) return null;
        // Pop the value
        T value = node.setValue(null);
        // Delete the node if there are no children
        if (node.noChildren()) {
            removeNode(node);
        }
        return value;
    }

    @Override
    public boolean remove(@NotNull Object key, Object value) {
        DhLodPos pos = (DhLodPos) key;
        if (pos.detail > topLevel) return false;
        Node node;
        if (pos.detail == topLevel) {
            node = nodes.get(pos);
        } else {
            node = travel(nodes.get(pos.convertUpwardsTo(topLevel)), pos);
        }
        if (node == null) return false;
        //TODO: Make this atomic
        if (node.value.equals(value)) {
            removeNode(node);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(@NotNull DhLodPos key, @NotNull T oldValue, @NotNull T newValue) {
        if (key.detail > topLevel) return false;
        Node node;
        if (key.detail == topLevel) {
            node = nodes.get(key);
        } else {
            node = travel(nodes.get(key.convertUpwardsTo(topLevel)), key);
        }
        if (node == null) return false;
        //TODO: Make this atomic
        if (node.value.equals(oldValue)) {
            node.setValue(newValue);
            return true;
        }
        return false;
    }

    @Override
    public T replace(@NotNull DhLodPos key, @NotNull T value) {
        if (key.detail == topLevel) {
            Node n = nodes.get(key);
            //TODO: Make this atomic
            if (n == null || n.value==null) return null;
            return n.setValue(value);
        }
        if (key.detail < topLevel) {
            Node node = travel(nodes.get(key.convertUpwardsTo(topLevel)), key);
            //TODO: Make this atomic
            if (node == null || node.value==null) return null;
            return node.setValue(value);
        }
        // key.detail > topLevel: Does not exist
        return null;
    }

    @Nullable
    @Override
    public T putIfAbsent(@NotNull DhLodPos key, T value) {
        if (key.detail == topLevel) {
            return nodes.computeIfAbsent(key, k -> new Node(null, 0, key, null)).setIfAbsent(value);
        }
        if (key.detail < topLevel) {
            Node node = initTravel(nodes.get(key.convertUpwardsTo(topLevel)), key);
            return node.setIfAbsent(value);
        }
        // key.detail > topLevel:
        // Rebase the tree
        //upcastTreeBase(key.detail);
        return nodes.computeIfAbsent(key, k -> new Node(null, 0, key, null)).setIfAbsent(value);

    }

    @Override
    public T computeIfAbsent(DhLodPos key, @NotNull Function<? super DhLodPos, ? extends T> mappingFunction) {
        if (key.detail == topLevel) {
            return nodes.computeIfAbsent(key, k -> new Node(null, 0, key, null)).computeIfAbsent(mappingFunction);
        }
        if (key.detail < topLevel) {
            Node node = initTravel(nodes.get(key.convertUpwardsTo(topLevel)), key);
            return node.computeIfAbsent(mappingFunction);
        }
        // key.detail > topLevel:
        // Rebase the tree
        //upcastTreeBase(key.detail);
        return nodes.computeIfAbsent(key, k -> new Node(null, 0, key, null)).computeIfAbsent(mappingFunction);

    }

    @Override
    public T computeIfPresent(DhLodPos key, @NotNull BiFunction<? super DhLodPos, ? super T, ? extends T> remappingFunction) {
        if (key.detail == topLevel) {
            Node n = nodes.get(key);
            if (n == null) return null;
            T r = n.computeIfPresent(remappingFunction);
            if (r == null && n.noChildren()) {
                nodes.remove(key);
            }
            return r;
        }
        if (key.detail < topLevel) {
            Node node = travel(nodes.get(key.convertUpwardsTo(topLevel)), key);
            if (node == null) return null;
            T r = node.computeIfPresent(remappingFunction);
            if (r == null && node.noChildren()) {
                removeNode(node);
            }
        }
        // key.detail > topLevel: Does not exist
        return null;
    }

    // TODO: Improve this naive implementation of compute
    @Override
    public T compute(DhLodPos key, @NotNull BiFunction<? super DhLodPos, ? super T, ? extends T> remappingFunction) {
        T r = get(key);
        if (r == null) {
            r = remappingFunction.apply(key, null);
            if (r != null) {
                put(key, r);
            }
        } else {
            r = remappingFunction.apply(key, r);
            if (r != null) {
                put(key, r);
            } else {
                remove(key);
            }
        }
        return r;
    }

    // TODO: Optimize putAll
    @Override
    public void putAll(@NotNull Map<? extends DhLodPos, ? extends T> m) {
        for (Map.Entry<? extends DhLodPos, ? extends T> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        nodes.clear();
        size = new AtomicInteger(0); // Do this to swap the counter obj so old nodes won't mess up the counter
    }

    @NotNull
    @Override
    public Set<DhLodPos> keySet() {
        //TODO
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Collection<T> values() {
        //TODO
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Set<Entry<DhLodPos, T>> entrySet() {
        //TODO
        throw new NotImplementedException();
    }

    @Override
    public void forEach(BiConsumer<? super DhLodPos, ? super T> action) {
        //TODO
        throw new NotImplementedException();
    }

    @Override
    public void replaceAll(BiFunction<? super DhLodPos, ? super T, ? extends T> function) {
        //TODO
        throw new NotImplementedException();
    }

    // merge: Use default implementation
    //public T merge(DhLodPos key, @NotNull T value, @NotNull BiFunction<? super T, ? super T, ? extends T> remappingFunction);
}
