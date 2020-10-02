/**
 Get name of the class for the object provided.
 */
class ClassGetter {

    public String getObjectClassName(Object object) {
        // Add implementation here
        final Class<?> aClass = object.getClass();
        return aClass.getCanonicalName();
    }

}