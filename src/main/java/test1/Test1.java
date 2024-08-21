package test1;
// Java program to demonstrate varargs

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

class Test1 {
	// A method that takes variable
	// number of integer arguments.
	static void fun(int... a) {
		System.out.println("Number of arguments: " + a.length);

		// using for each loop to display contents of a
		for (int i : a)
			System.out.print(i + " ");
		System.out.println();
	}

	private static void DebugList(String s, Integer[] first) {
	}

	public static Integer[] uniqueInts(Integer[] ... arrs) {
		Set<Integer> set = new HashSet<>();

		int i=0;
		for(Integer[] arr: arrs){
			if(ArrayUtils.isNotEmpty(arr)) {
				DebugList("element "+i, arr);
				Collections.addAll(set, arr);
			}
			i++;
		}
		DebugList("result", set.toArray(new Integer[set.size()]));

		return set.toArray(new Integer[set.size()]);
	}

	// Driver code
	public static void main(String args[]) {
		// Calling the varargs method with
		// different number of parameters

		// one parameter
		fun(100);

		// four parameters
		fun(1, 2, 3, 4);

		// no parameter
		fun();
	}
}
