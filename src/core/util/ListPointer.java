package core.util;

import com.badlogic.gdx.utils.Array;

public class ListPointer<T> {

    private int currentIndex;
    private Array<T> data;
    private boolean loop;
    
    public ListPointer(Array<T> data) {
        this(data, false);
    }
    
    public ListPointer(Array<T> data, boolean loop) {
        this.data = data;
        this.currentIndex = 0;
        this.loop = loop;
    }
    
    public T first() {
        currentIndex = 0;
        return get();
    }
    
    public T last() {
        currentIndex = data.size - 1;
        return get();
    }
    
    public T get() {
        return data.get(currentIndex);
    }
    
    public T select(int index) {
        if (index > 0 && index < data.size) {
            currentIndex = index;
            return get();
        }
        throw new IndexOutOfBoundsException();
    }
    
    public T select(T object) {
        int index = data.indexOf(object, true);
        if (index != -1) {
            currentIndex = index;
            return object;
        }
        throw new NullPointerException("Item not found in array");
    }
    
    public T next() {
        if (currentIndex < data.size - 1) {
            currentIndex++;
            return get();
        } else if (loop) {
            currentIndex = 0;
            return get();
        }
        throw new IndexOutOfBoundsException();
    }
    
    public T prev() {
        if (currentIndex > 0) {
            currentIndex--;
            return get();
        } else if (loop) {
            currentIndex = data.size - 1;
            return get();
        }
        throw new IndexOutOfBoundsException();
    }
    
    public T peekNext() {
        int index = currentIndex;
        if (currentIndex < data.size - 1) {
            index++;
            return data.get(index);
        } else if (loop) {
            index = 0;
            return data.get(index);
        }
        throw new IndexOutOfBoundsException();
    }
    
    public T peekPrev() {
        int index = currentIndex;
        if (currentIndex > 0) {
            index--;
            return data.get(index);
        } else if (loop) {
            index = data.size - 1;
            return data.get(index);
        }
        throw new IndexOutOfBoundsException();
    }
}
