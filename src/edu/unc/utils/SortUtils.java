package edu.unc.utils;

/**
 * Custom sorting utilities
 * see: http://stackoverflow.com/questions/951848/java-array-sort-quick-way-to-get-a-sorted-list-of-indices-of-an-array
 * @author timpalpant
 *
 */
public class SortUtils {
	public static int[] rank(float[] main) {
		int[] index = new int[main.length];
		for (int i = 0; i < index.length; i++) {
			index[i] = i;
		}
		
		quicksort(main, index, 0, index.length-1);
		
		return index;
	}

	// quicksort a[left] to a[right]
	private static void quicksort(float[] a, int[] index, int left, int right) {
		if (right <= left)
			return;
		int i = partition(a, index, left, right);
		quicksort(a, index, left, i - 1);
		quicksort(a, index, i + 1, right);
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition(float[] a, int[] index, int left, int right) {
		int i = left - 1;
		int j = right;
		while (true) {
			while (a[index[++i]] < a[index[right]])
				// find item on left to swap
				; // a[right] acts as sentinel
			while (a[index[right]] < a[index[--j]])
				// find item on right to swap
				if (j == left)
					break; // don't go out-of-bounds
			if (i >= j)
				break; // check if pointers cross
			exch(a, index, i, j); // swap two elements into place
		}
		exch(a, index, i, right); // swap with partition element
		return i;
	}

	// exchange a[i] and a[j]
	private static void exch(float[] a, int[] index, int i, int j) {
		// Don't swap the data
		// float swap = a[i];
		// a[i] = a[j];
		// a[j] = swap;
		int b = index[i];
		index[i] = index[j];
		index[j] = b;
	}
}
