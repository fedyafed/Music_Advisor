import java.util.ArrayList;
import java.util.List;

class QualityControl {

    public static boolean check(List<Box<? extends Bakery>> boxes) {
        // Add implementation here
        try {
            for (Box<? extends Bakery> box : boxes) {
                final Bakery bakery = box.get();
                if (bakery == null) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}

// Don't change the code below
class Bakery {
}

class Cake extends Bakery {
}

class Pie extends Bakery {
}

class Tart extends Bakery {
}

class Paper {
}

class Box<T> {

    private T item;

    public void put(T item) {
        this.item = item;
    }

    public T get() {
        return this.item;
    }
}

//public class Main{
//    public static void main(String[] args) {
//        List list = new ArrayList();
//        Box box = new Box();
//        box.put(new Paper());
//        list.add(box);
//        System.out.println(QualityControl.check(list));
//    }
//}