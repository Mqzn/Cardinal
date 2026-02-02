package eg.mqzen.cardinal.util;

import java.util.Iterator;
import java.util.LinkedHashSet;

public final class BoundedSet<T> extends LinkedHashSet<T> {
    private final int maxSize;
    
    public BoundedSet(int maxSize) {
        this.maxSize = maxSize;
    }
    
    @Override
    public boolean add(T element) {
        if (size() >= maxSize) {
            // Remove oldest element (first in LinkedHashSet)
            Iterator<T> iterator = iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        return super.add(element);
    }
}