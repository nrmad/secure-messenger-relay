package networking;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {

    void accept(T t) throws E;

    static <T, E extends Throwable> Consumer<T> unchecked(ThrowingConsumer<T, E> c){
        return t -> {
            try{
                c.accept(t);
            } catch (Throwable e){
                throw new RuntimeException();
            }
        };
    }
}
