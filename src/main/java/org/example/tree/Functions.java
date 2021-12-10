package org.example.tree;

import lombok.SneakyThrows;

public abstract class Functions {
    interface Fn1E<T>{
        T invoke() throws Exception;
    }
    interface Fn0E{
        void invoke() throws Exception;
    }
    @SneakyThrows
    public static <T> T sneakThrows(Fn1E<T> fn){
        return fn.invoke();
    }
    @SneakyThrows
    public static void sneakThrows(Fn0E fn){
        fn.invoke();
    }
}
