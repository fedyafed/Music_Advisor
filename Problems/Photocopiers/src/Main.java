import java.util.Arrays;

/**
 * Class to work with
 */
class Multiplicator {

	public static <T extends Copy<T>> Folder<T>[] multiply(Folder<T> folder, int arraySize) {
		// Method to implement
        Folder<T>[] result = new Folder[arraySize];
        final T source = folder.get();
        for (int i = 0; i < arraySize; i++) {
            final T copy = source.copy();
            final Folder<T> target = new Folder<>();
            target.put(copy);
            result[i] = target;
        }

        return result;
	}

}

// Don't change the code below
interface Copy<T> {
	T copy();
}

class Folder<T> {

    private T item;

    public void put(T item) {
    	this.item = item;
    }

    public T get() {
        return this.item;
    }

/*    @Override
    public String toString() {
        return "Folder{" +
                "item=" + item +
                '}';
    }*/
}

/*
class IntCopy implements Copy<IntCopy> {
    private final int i;

    public IntCopy(int i) {
        this.i = i;
    }

    @Override
    public IntCopy copy() {
        return new IntCopy(i - 1);
    }

    @Override
    public String toString() {
        return String.valueOf(i);
    }
}

public class Main {
    public static void main(String[] args) {
        final Folder<IntCopy> f1 = new Folder<>();
        f1.put(new IntCopy(10));
        System.out.println(Arrays.toString(Multiplicator.multiply(f1, 2)));
    }
}*/
