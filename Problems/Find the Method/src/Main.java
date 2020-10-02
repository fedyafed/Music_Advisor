import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.function.Predicate.isEqual;

class MethodFinder {

    public static String findMethod(String methodName, String[] classNames) throws ClassNotFoundException {
        for (String className : classNames) {
            if (Arrays.stream(Class.forName(className).getMethods())
                    .map((Function<Method, Object>) Method::getName)
                    .anyMatch(isEqual(methodName))) {
                return className;
            }
        }
        return null;
    }
}