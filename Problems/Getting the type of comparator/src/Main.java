//import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Class to work with
 */
class ComparatorInspector {

    /**
     * Return Type variable that corresponds to the type parameterizing Comparator.   
     *
     * @param clazz {@link Class} object, should be non null
     * @return {@link Type} object or null if Comparable does not have type parameter
     */
    public static <T, C extends Comparable<?>> Type getComparatorType(Class<C> clazz) {
        // Add implementation
        final Type[] genericInterfaces = clazz.getGenericInterfaces();
        return Arrays.stream(genericInterfaces)
                .filter(i -> i instanceof ParameterizedType)
                .map(i -> (ParameterizedType) i)
                .filter(i -> i.getRawType().equals(Comparable.class))
                .map(i -> i.getActualTypeArguments()[0])
                .findFirst()
                .orElse(null);
    }

}

//class MyInt implements Comparable<Integer> {
//    @Override
//    public int compareTo(@NotNull Integer o) {
//        return 0;
//    }
    // Implementation omitted
//}

//class Test{}
//
//public class Main {
//    public static void main(String[] args) {
//// Method to implement
//        Type type = ComparatorInspector.getComparatorType(MyInt.class);
//
//        System.out.println(type.getTypeName());
//// prints: java.lang.Integer since MyInt implements Comparable with Integer parameter type
//    }
//}