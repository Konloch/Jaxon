package util.vector;

import kernel.Kernel;

public class Vec {
    private static final int DEFAULT_CAPACITY = 10;
    private Object[] elements;
    private int size;

    public Vec() {
        this.elements = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }

    public Vec(int initialCapacity) {
        if (initialCapacity < 0)
            Kernel.panic("Illegal Capacity");
        this.elements = new Object[initialCapacity];
        this.size = 0;
    }

    public void Add(Object element) {
        ensureCapacity(size + 1);
        elements[size++] = element;
    }

    public void Remove(Object element) {
        for (int i = 0; i < size; i++) {
            if (elements[i] == element) {
                Remove(i);
                return;
            }
        }
    }

    public void Remove(int index) {
        if (index < 0 || index >= size)
            Kernel.panic("Index out of bounds for vector removal");
        for (int i = index; i < size - 1; i++) {
            elements[i] = elements[i + 1];
        }
        size--;
    }

    public Object Get(int index) {
        if (index < 0 || index >= size)
            Kernel.panic("Index out of bounds for vector access");
        return elements[index];
    }

    public int Size() {
        return size;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > elements.length) {
            int newCapacity = elements.length * 2;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }

            Object[] newElements = new Object[newCapacity];
            for (int i = 0; i < size; i++) {
                newElements[i] = elements[i];
            }
            elements = newElements;
        }
    }
}
