class BoxInside {

    // Complete this method
    public static <T extends Animal>void showAnimal(Box<T> box) {
        final T animal = box.getAnimal();
        System.out.println(animal.toString());
        box.setAnimal(animal);
    }
}

// Don't change the code below
class Animal {

    private String name;

    public Animal(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}

class Box<T> {

    private T animal;

    void setAnimal(T animal) {
        this.animal = animal;
    }

    T getAnimal() {
        return animal;
    }
}
