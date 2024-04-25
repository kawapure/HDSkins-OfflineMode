package com.minelittlepony.hdskins.client;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public interface Memoize<T> extends Supplier<T> {

    default void expireNow() {}

    static <T> Memoize<T> nonExpiring(Supplier<T> supplier) {
        return supplier::get;
    }

    static <T> Memoize<T> withExpiration(Supplier<T> supplier) {
        return new Memoize<>() {
            private Supplier<T> value = Suppliers.memoizeWithExpiration(supplier::get, 1, TimeUnit.SECONDS)::get;
            @Override
            public T get() {
                return value.get();
            }

            @Override
            public void expireNow() {
                value = Suppliers.memoizeWithExpiration(supplier::get, 1, TimeUnit.SECONDS)::get;
            }
        };
    }
}
