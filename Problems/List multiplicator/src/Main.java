import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
    Class to modify
*/
class ListMultiplicator {

    /**
        Repeats original list content provided number of times   
        @param list list to repeat
        @param n times to repeat, should be zero or greater
    */
	public static void multiply(List<?> list, int n) {
		// Add implementation here
		multiplyT(list, n);
	}

	public static <T> void multiplyT(List<T> list, int n) {
		// Add implementation here
		List<T> source = new ArrayList<>(list);
		list.clear();
		for (int i = 0; i < n; i++) {
			list.addAll(source);
		}
	}
}